"""Settings read from environment variables."""
import os
from dataclasses import dataclass, field


def _list(name: str, default: str) -> list[str]:
    return [x.strip() for x in os.getenv(name, default).split(",") if x.strip()]


@dataclass(frozen=True)
class Settings:
    database_url: str = field(default_factory=lambda: os.getenv("DATABASE_URL", "sqlite:///./xalane.db"))
    # Token for /v1/admin endpoints. Required in production.
    admin_token: str = field(default_factory=lambda: os.getenv("ADMIN_TOKEN", ""))
    # DNS servers given to devices when they connect.
    dns: list[str] = field(default_factory=lambda: _list("VPN_DNS", "1.1.1.1,1.0.0.1"))
    # A node counts as online if it sent a heartbeat within this many seconds.
    node_timeout_seconds: int = field(default_factory=lambda: int(os.getenv("NODE_TIMEOUT_SECONDS", "90")))
    # Peers unused for this many days are removed from servers.
    peer_ttl_days: int = field(default_factory=lambda: int(os.getenv("PEER_TTL_DAYS", "30")))
    # Devices inactive for this many days are deleted.
    device_ttl_days: int = field(default_factory=lambda: int(os.getenv("DEVICE_TTL_DAYS", "180")))
    # Device registrations allowed per client IP per hour. Mobile networks put many phones behind one IP (CGNAT).
    register_limit_per_hour: int = field(default_factory=lambda: int(os.getenv("REGISTER_LIMIT_PER_HOUR", "200")))


def get_settings() -> Settings:
    return Settings()
