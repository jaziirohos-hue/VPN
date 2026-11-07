import base64
import os

import pytest
from fastapi.testclient import TestClient

from app.config import Settings
from app.main import create_app

ADMIN = "test-admin-token"


def key(n: int) -> str:
    """A syntactically valid WireGuard key for tests."""
    return base64.b64encode(bytes([n]) * 32).decode()


@pytest.fixture
def client(tmp_path):
    settings = Settings(database_url=f"sqlite:///{tmp_path/'t.db'}", admin_token=ADMIN, register_limit_per_hour=1000)
    with TestClient(create_app(settings)) as c:
        yield c


def admin_headers():
    return {"Authorization": f"Bearer {ADMIN}"}


def bearer(token: str):
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture
def server(client):
    """Creates an online server and returns (server_id, node_token)."""
    r = client.post("/v1/admin/servers", headers=admin_headers(), json={
        "id": "de-fra-1", "country": "Germany", "country_code": "de", "city": "Frankfurt",
        "lat": 50.1, "lon": 8.7, "endpoint_host": "de1.example.net", "public_key": key(200),
        "tunnel_cidr": "10.8.0.0/29", "capacity": 5,
    })
    assert r.status_code == 201, r.text
    token = r.json()["node_token"]
    assert client.post("/v1/node/heartbeat", headers=bearer(token), json={"active_peers": 1}).status_code == 204
    return "de-fra-1", token


def register(client, n: int) -> str:
    r = client.post("/v1/devices", json={"public_key": key(n)})
    assert r.status_code == 201, r.text
    return r.json()["token"]


os.environ.setdefault("DATABASE_URL", "sqlite:///:memory:")
