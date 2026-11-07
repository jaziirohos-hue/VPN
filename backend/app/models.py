from datetime import datetime, timezone

from sqlalchemy import Boolean, DateTime, Float, ForeignKey, Integer, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from .db import Base


def utcnow() -> datetime:
    return datetime.now(timezone.utc)


class Server(Base):
    """A VPN node running WireGuard and the Xalane agent."""

    __tablename__ = "servers"

    id: Mapped[str] = mapped_column(String(40), primary_key=True)  # e.g. "de-fra-1"
    country: Mapped[str] = mapped_column(String(80))
    country_code: Mapped[str] = mapped_column(String(2))
    city: Mapped[str] = mapped_column(String(80))
    lat: Mapped[float | None] = mapped_column(Float, nullable=True)
    lon: Mapped[float | None] = mapped_column(Float, nullable=True)
    endpoint_host: Mapped[str] = mapped_column(String(255))
    endpoint_port: Mapped[int] = mapped_column(Integer, default=51820)
    public_key: Mapped[str] = mapped_column(String(44))
    tunnel_cidr: Mapped[str] = mapped_column(String(43))  # e.g. "10.8.0.0/22"; .1 is the server
    capacity: Mapped[int] = mapped_column(Integer, default=250)
    streaming: Mapped[bool] = mapped_column(Boolean, default=False)
    enabled: Mapped[bool] = mapped_column(Boolean, default=True)
    node_token_hash: Mapped[str] = mapped_column(String(64), unique=True)
    last_seen_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    active_peers: Mapped[int] = mapped_column(Integer, default=0)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

    peers: Mapped[list["Peer"]] = relationship(back_populates="server", cascade="all, delete-orphan")


class Device(Base):
    """An app install. Anonymous: identified only by its WireGuard public key and a random token."""

    __tablename__ = "devices"

    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    token_hash: Mapped[str] = mapped_column(String(64), unique=True)
    public_key: Mapped[str] = mapped_column(String(44), unique=True)
    platform: Mapped[str] = mapped_column(String(20), default="android")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    last_seen_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

    peers: Mapped[list["Peer"]] = relationship(back_populates="device", cascade="all, delete-orphan")


class Peer(Base):
    """A device's tunnel address on one server."""

    __tablename__ = "peers"
    __table_args__ = (
        UniqueConstraint("device_id", "server_id"),
        UniqueConstraint("server_id", "address"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    device_id: Mapped[str] = mapped_column(ForeignKey("devices.id", ondelete="CASCADE"))
    server_id: Mapped[str] = mapped_column(ForeignKey("servers.id", ondelete="CASCADE"))
    address: Mapped[str] = mapped_column(String(39))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    last_used_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

    device: Mapped[Device] = relationship(back_populates="peers")
    server: Mapped[Server] = relationship(back_populates="peers")
