import ipaddress
import re

from pydantic import BaseModel, Field, field_validator

from .security import is_wireguard_key


def _check_key(v: str) -> str:
    if not is_wireguard_key(v):
        raise ValueError("must be a WireGuard public key (44 base64 characters)")
    return v


class RegisterRequest(BaseModel):
    public_key: str
    platform: str = Field(default="android", max_length=20)

    _key = field_validator("public_key")(_check_key)


class RegisterResponse(BaseModel):
    device_id: str
    token: str


class ServerOut(BaseModel):
    id: str
    country: str
    country_code: str
    city: str
    lat: float | None
    lon: float | None
    host: str
    online: bool
    load: int = Field(description="Percent of capacity in use")
    streaming: bool


class ConnectRequest(BaseModel):
    server_id: str


class InterfaceOut(BaseModel):
    address: str
    dns: list[str]


class PeerOut(BaseModel):
    public_key: str
    endpoint: str
    allowed_ips: list[str]
    persistent_keepalive: int


class ConnectResponse(BaseModel):
    server_id: str
    interface: InterfaceOut
    peer: PeerOut


class NodeHeartbeat(BaseModel):
    active_peers: int = 0
    rx_bytes: int = 0
    tx_bytes: int = 0


class NodePeer(BaseModel):
    public_key: str
    allowed_ip: str


class NodePeersResponse(BaseModel):
    server_id: str
    peers: list[NodePeer]


class ServerCreate(BaseModel):
    id: str = Field(pattern=r"^[a-z0-9-]{2,40}$")
    country: str = Field(min_length=2, max_length=80)
    country_code: str = Field(min_length=2, max_length=2)
    city: str = Field(min_length=1, max_length=80)
    lat: float | None = None
    lon: float | None = None
    endpoint_host: str = Field(min_length=1, max_length=255)
    endpoint_port: int = Field(default=51820, ge=1, le=65535)
    public_key: str
    tunnel_cidr: str = "10.8.0.0/22"
    capacity: int = Field(default=250, ge=1, le=60000)
    streaming: bool = False

    _key = field_validator("public_key")(_check_key)

    @field_validator("country_code")
    @classmethod
    def _upper(cls, v: str) -> str:
        if not re.fullmatch(r"[A-Za-z]{2}", v):
            raise ValueError("must be a two-letter country code")
        return v.upper()

    @field_validator("tunnel_cidr")
    @classmethod
    def _cidr(cls, v: str) -> str:
        net = ipaddress.ip_network(v, strict=True)
        if net.version != 4 or net.num_addresses < 4:
            raise ValueError("must be an IPv4 network such as 10.8.0.0/22")
        return str(net)


class ServerUpdate(BaseModel):
    enabled: bool | None = None
    capacity: int | None = Field(default=None, ge=1, le=60000)
    streaming: bool | None = None
    endpoint_host: str | None = None
    public_key: str | None = None

    @field_validator("public_key")
    @classmethod
    def _key(cls, v: str | None) -> str | None:
        return None if v is None else _check_key(v)


class ServerCreated(BaseModel):
    id: str
    node_token: str = Field(description="Shown once. Put it in the node's agent config.")


class AdminServerOut(ServerOut):
    enabled: bool
    endpoint_port: int
    public_key: str
    tunnel_cidr: str
    capacity: int
    active_peers: int
    allocated_peers: int
    last_seen_at: str | None
