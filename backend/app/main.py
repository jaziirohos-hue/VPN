"""Xalane VPN control API.

Phones register anonymously, list servers and ask for a tunnel on a server.
VPN nodes poll for the peers they should allow and report a heartbeat.
"""
import asyncio
import ipaddress
import logging
import time
import uuid
from collections import defaultdict, deque
from contextlib import asynccontextmanager
from datetime import datetime, timedelta, timezone

from fastapi import Depends, FastAPI, Header, HTTPException, Request, status
from sqlalchemy import delete, func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from . import schemas
from .config import Settings, get_settings
from .db import Base, make_engine, make_sessionmaker
from .models import Device, Peer, Server, utcnow
from .security import hash_token, new_token, tokens_equal

log = logging.getLogger("xalane")


def _aware(dt: datetime | None) -> datetime | None:
    # SQLite returns naive datetimes; treat them as UTC.
    if dt is not None and dt.tzinfo is None:
        return dt.replace(tzinfo=timezone.utc)
    return dt


class RateLimiter:
    """Small in-memory sliding-window limiter, keyed by client IP."""

    def __init__(self, limit: int, window_seconds: int):
        self.limit, self.window = limit, window_seconds
        self.hits: dict[str, deque] = defaultdict(deque)

    def allow(self, key: str) -> bool:
        now = time.monotonic()
        q = self.hits[key]
        while q and now - q[0] > self.window:
            q.popleft()
        if len(q) >= self.limit:
            return False
        q.append(now)
        return True


def endpoint(host: str, port: int) -> str:
    """host:port, with brackets around IPv6 addresses."""
    return f"[{host}]:{port}" if ":" in host else f"{host}:{port}"


def cleanup(db: Session, settings: Settings) -> tuple[int, int]:
    """Removes stale peers and inactive devices. Returns (peers, devices) removed."""
    now = utcnow()
    peers = db.execute(delete(Peer).where(Peer.last_used_at < now - timedelta(days=settings.peer_ttl_days))).rowcount
    devices = db.execute(delete(Device).where(Device.last_seen_at < now - timedelta(days=settings.device_ttl_days))).rowcount
    db.commit()
    return peers or 0, devices or 0


def create_app(settings: Settings | None = None) -> FastAPI:
    settings = settings or get_settings()
    engine = make_engine(settings.database_url)
    Base.metadata.create_all(engine)
    SessionLocal = make_sessionmaker(engine)
    register_limiter = RateLimiter(settings.register_limit_per_hour, 3600)

    async def cleanup_loop():
        while True:
            try:
                with SessionLocal() as db:
                    p, d = cleanup(db, settings)
                    if p or d:
                        log.info("cleanup removed %s peers and %s devices", p, d)
            except Exception:  # keep the loop alive
                log.exception("cleanup failed")
            await asyncio.sleep(3600)

    @asynccontextmanager
    async def lifespan(_: FastAPI):
        task = asyncio.create_task(cleanup_loop())
        yield
        task.cancel()

    app = FastAPI(title="Xalane VPN API", version="1.0.0", lifespan=lifespan)
    app.state.sessionmaker = SessionLocal
    app.state.settings = settings

    def get_db():
        with SessionLocal() as db:
            yield db

    def bearer(authorization: str | None) -> str:
        if not authorization or not authorization.startswith("Bearer "):
            raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Missing bearer token")
        return authorization.removeprefix("Bearer ").strip()

    def current_device(authorization: str | None = Header(default=None), db: Session = Depends(get_db)) -> Device:
        device = db.scalar(select(Device).where(Device.token_hash == hash_token(bearer(authorization))))
        if device is None:
            raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Unknown device token")
        return device

    def current_node(authorization: str | None = Header(default=None), db: Session = Depends(get_db)) -> Server:
        server = db.scalar(select(Server).where(Server.node_token_hash == hash_token(bearer(authorization))))
        if server is None:
            raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Unknown node token")
        return server

    def require_admin(authorization: str | None = Header(default=None)) -> None:
        if not settings.admin_token:
            raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, "ADMIN_TOKEN is not configured")
        if not tokens_equal(bearer(authorization), settings.admin_token):
            raise HTTPException(status.HTTP_403_FORBIDDEN, "Wrong admin token")

    def is_online(server: Server) -> bool:
        seen = _aware(server.last_seen_at)
        return bool(server.enabled and seen and utcnow() - seen < timedelta(seconds=settings.node_timeout_seconds))

    def load(server: Server) -> int:
        return min(100, round(100 * server.active_peers / max(server.capacity, 1)))

    def server_out(s: Server) -> schemas.ServerOut:
        return schemas.ServerOut(
            id=s.id, country=s.country, country_code=s.country_code, city=s.city, lat=s.lat, lon=s.lon,
            host=s.endpoint_host, online=is_online(s), load=load(s), streaming=s.streaming,
        )

    # ---------- public ----------

    @app.get("/health")
    def health():
        return {"status": "ok"}

    @app.post("/v1/devices", response_model=schemas.RegisterResponse, status_code=201)
    def register(body: schemas.RegisterRequest, request: Request, db: Session = Depends(get_db)):
        ip = request.client.host if request.client else "unknown"
        if not register_limiter.allow(ip):
            raise HTTPException(status.HTTP_429_TOO_MANY_REQUESTS, "Too many registrations. Try again later.")
        token = new_token()
        device = Device(id=str(uuid.uuid4()), token_hash=hash_token(token), public_key=body.public_key, platform=body.platform)
        db.add(device)
        try:
            db.commit()
        except IntegrityError:
            db.rollback()
            raise HTTPException(status.HTTP_409_CONFLICT, "This public key is already registered")
        return schemas.RegisterResponse(device_id=device.id, token=token)

    @app.delete("/v1/devices/me", status_code=204)
    def delete_me(device: Device = Depends(current_device), db: Session = Depends(get_db)):
        db.delete(db.merge(device))
        db.commit()

    @app.get("/v1/servers", response_model=list[schemas.ServerOut])
    def list_servers(db: Session = Depends(get_db)):
        servers = db.scalars(select(Server).where(Server.enabled.is_(True)).order_by(Server.country, Server.city)).all()
        return [server_out(s) for s in servers]

    @app.post("/v1/connect", response_model=schemas.ConnectResponse)
    def connect(body: schemas.ConnectRequest, device: Device = Depends(current_device), db: Session = Depends(get_db)):
        server = db.get(Server, body.server_id)
        if server is None or not server.enabled:
            raise HTTPException(status.HTTP_404_NOT_FOUND, "Server not found")
        if not is_online(server):
            raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, "This server is offline. Pick another location.")

        device = db.merge(device)
        now = utcnow()
        device.last_seen_at = now
        peer = db.scalar(select(Peer).where(Peer.device_id == device.id, Peer.server_id == server.id))
        if peer is None:
            peer = allocate_peer(db, device, server)
        peer.last_used_at = now
        db.commit()

        return schemas.ConnectResponse(
            server_id=server.id,
            interface=schemas.InterfaceOut(address=f"{peer.address}/32", dns=settings.dns),
            peer=schemas.PeerOut(
                public_key=server.public_key,
                endpoint=endpoint(server.endpoint_host, server.endpoint_port),
                allowed_ips=["0.0.0.0/0", "::/0"],
                persistent_keepalive=25,
            ),
        )

    def allocate_peer(db: Session, device: Device, server: Server) -> Peer:
        used = set(db.scalars(select(Peer.address).where(Peer.server_id == server.id)))
        network = ipaddress.ip_network(server.tunnel_cidr)
        hosts = network.hosts()
        next(hosts)  # first address belongs to the server
        if len(used) >= server.capacity:
            raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, "This server is full. Pick another location.")
        for addr in hosts:
            if str(addr) not in used:
                peer = Peer(device_id=device.id, server_id=server.id, address=str(addr))
                db.add(peer)
                db.flush()
                return peer
        raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, "This server is full. Pick another location.")

    # ---------- nodes ----------

    @app.post("/v1/node/heartbeat", status_code=204)
    def heartbeat(body: schemas.NodeHeartbeat, server: Server = Depends(current_node), db: Session = Depends(get_db)):
        server = db.merge(server)
        server.last_seen_at = utcnow()
        server.active_peers = max(0, body.active_peers)
        db.commit()

    @app.get("/v1/node/peers", response_model=schemas.NodePeersResponse)
    def node_peers(server: Server = Depends(current_node), db: Session = Depends(get_db)):
        rows = db.execute(
            select(Device.public_key, Peer.address).join(Peer, Peer.device_id == Device.id).where(Peer.server_id == server.id)
        ).all()
        return schemas.NodePeersResponse(
            server_id=server.id,
            peers=[schemas.NodePeer(public_key=k, allowed_ip=f"{a}/32") for k, a in rows],
        )

    # ---------- admin ----------

    def admin_out(db: Session, s: Server) -> schemas.AdminServerOut:
        allocated = db.scalar(select(func.count()).select_from(Peer).where(Peer.server_id == s.id)) or 0
        seen = _aware(s.last_seen_at)
        return schemas.AdminServerOut(
            **server_out(s).model_dump(), enabled=s.enabled, endpoint_port=s.endpoint_port, public_key=s.public_key,
            tunnel_cidr=s.tunnel_cidr, capacity=s.capacity, active_peers=s.active_peers, allocated_peers=allocated,
            last_seen_at=seen.isoformat() if seen else None,
        )

    @app.get("/v1/admin/servers", response_model=list[schemas.AdminServerOut], dependencies=[Depends(require_admin)])
    def admin_list(db: Session = Depends(get_db)):
        return [admin_out(db, s) for s in db.scalars(select(Server).order_by(Server.id)).all()]

    @app.post("/v1/admin/servers", response_model=schemas.ServerCreated, status_code=201, dependencies=[Depends(require_admin)])
    def admin_create(body: schemas.ServerCreate, db: Session = Depends(get_db)):
        if db.get(Server, body.id):
            raise HTTPException(status.HTTP_409_CONFLICT, "A server with this id already exists")
        token = new_token()
        db.add(Server(**body.model_dump(), node_token_hash=hash_token(token)))
        db.commit()
        return schemas.ServerCreated(id=body.id, node_token=token)

    @app.patch("/v1/admin/servers/{server_id}", response_model=schemas.AdminServerOut, dependencies=[Depends(require_admin)])
    def admin_update(server_id: str, body: schemas.ServerUpdate, db: Session = Depends(get_db)):
        server = db.get(Server, server_id)
        if server is None:
            raise HTTPException(status.HTTP_404_NOT_FOUND, "Server not found")
        for k, v in body.model_dump(exclude_none=True).items():
            setattr(server, k, v)
        db.commit()
        return admin_out(db, server)

    @app.post("/v1/admin/servers/{server_id}/rotate-token", response_model=schemas.ServerCreated, dependencies=[Depends(require_admin)])
    def admin_rotate(server_id: str, db: Session = Depends(get_db)):
        server = db.get(Server, server_id)
        if server is None:
            raise HTTPException(status.HTTP_404_NOT_FOUND, "Server not found")
        token = new_token()
        server.node_token_hash = hash_token(token)
        db.commit()
        return schemas.ServerCreated(id=server.id, node_token=token)

    @app.delete("/v1/admin/servers/{server_id}", status_code=204, dependencies=[Depends(require_admin)])
    def admin_delete(server_id: str, db: Session = Depends(get_db)):
        server = db.get(Server, server_id)
        if server is None:
            raise HTTPException(status.HTTP_404_NOT_FOUND, "Server not found")
        db.delete(server)
        db.commit()

    @app.get("/v1/admin/stats", dependencies=[Depends(require_admin)])
    def admin_stats(db: Session = Depends(get_db)):
        return {
            "devices": db.scalar(select(func.count()).select_from(Device)),
            "peers": db.scalar(select(func.count()).select_from(Peer)),
            "servers": db.scalar(select(func.count()).select_from(Server)),
        }

    return app


app = create_app()
