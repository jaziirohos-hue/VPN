from datetime import timedelta

from app.main import cleanup, endpoint
from app.models import Peer, utcnow
from conftest import admin_headers, bearer, key, register


def test_health(client):
    assert client.get("/health").json() == {"status": "ok"}


def test_register_validates_key_and_rejects_duplicates(client):
    assert client.post("/v1/devices", json={"public_key": "not-a-key"}).status_code == 422
    register(client, 1)
    assert client.post("/v1/devices", json={"public_key": key(1)}).status_code == 409


def test_server_list_shows_online_state(client, server):
    servers = client.get("/v1/servers").json()
    assert len(servers) == 1
    s = servers[0]
    assert s["id"] == "de-fra-1" and s["country_code"] == "DE" and s["online"] is True and s["load"] == 20
    assert "public_key" not in s


def test_new_server_is_offline_until_heartbeat(client):
    client.post("/v1/admin/servers", headers=admin_headers(), json={
        "id": "nl-ams-1", "country": "Netherlands", "country_code": "NL", "city": "Amsterdam",
        "endpoint_host": "nl1.example.net", "public_key": key(201),
    })
    assert client.get("/v1/servers").json()[0]["online"] is False


def test_connect_returns_config_and_reuses_address(client, server):
    sid, _ = server
    token = register(client, 1)
    r = client.post("/v1/connect", headers=bearer(token), json={"server_id": sid})
    assert r.status_code == 200, r.text
    body = r.json()
    assert body["interface"]["address"] == "10.8.0.2/32"
    assert body["peer"]["public_key"] == key(200)
    assert body["peer"]["endpoint"] == "de1.example.net:51820"
    assert body["peer"]["allowed_ips"] == ["0.0.0.0/0", "::/0"]
    again = client.post("/v1/connect", headers=bearer(token), json={"server_id": sid}).json()
    assert again["interface"]["address"] == "10.8.0.2/32"


def test_each_device_gets_its_own_address_and_node_sees_them(client, server):
    sid, node_token = server
    addresses = set()
    for n in range(1, 4):
        r = client.post("/v1/connect", headers=bearer(register(client, n)), json={"server_id": sid})
        addresses.add(r.json()["interface"]["address"])
    assert addresses == {"10.8.0.2/32", "10.8.0.3/32", "10.8.0.4/32"}
    peers = client.get("/v1/node/peers", headers=bearer(node_token)).json()
    assert peers["server_id"] == sid
    assert {p["public_key"] for p in peers["peers"]} == {key(1), key(2), key(3)}


def test_full_server_is_refused(client, server):
    sid, _ = server
    # /29 has 6 hosts; the server takes one and capacity is 5
    for n in range(1, 6):
        assert client.post("/v1/connect", headers=bearer(register(client, n)), json={"server_id": sid}).status_code == 200
    r = client.post("/v1/connect", headers=bearer(register(client, 6)), json={"server_id": sid})
    assert r.status_code == 503 and "full" in r.json()["detail"]


def test_offline_or_unknown_server_is_refused(client, server):
    token = register(client, 1)
    assert client.post("/v1/connect", headers=bearer(token), json={"server_id": "nope"}).status_code == 404
    client.patch("/v1/admin/servers/de-fra-1", headers=admin_headers(), json={"enabled": False})
    assert client.post("/v1/connect", headers=bearer(token), json={"server_id": "de-fra-1"}).status_code == 404


def test_auth_is_required(client, server):
    assert client.post("/v1/connect", json={"server_id": "de-fra-1"}).status_code == 401
    assert client.post("/v1/connect", headers=bearer("wrong"), json={"server_id": "de-fra-1"}).status_code == 401
    assert client.get("/v1/node/peers", headers=bearer("wrong")).status_code == 401
    assert client.get("/v1/admin/servers").status_code == 401
    assert client.get("/v1/admin/servers", headers=bearer("wrong")).status_code == 403


def test_delete_device_removes_its_peers(client, server):
    sid, node_token = server
    token = register(client, 1)
    client.post("/v1/connect", headers=bearer(token), json={"server_id": sid})
    assert client.delete("/v1/devices/me", headers=bearer(token)).status_code == 204
    assert client.get("/v1/node/peers", headers=bearer(node_token)).json()["peers"] == []
    assert client.post("/v1/connect", headers=bearer(token), json={"server_id": sid}).status_code == 401


def test_rotate_node_token(client, server):
    sid, old = server
    new = client.post(f"/v1/admin/servers/{sid}/rotate-token", headers=admin_headers()).json()["node_token"]
    assert client.get("/v1/node/peers", headers=bearer(old)).status_code == 401
    assert client.get("/v1/node/peers", headers=bearer(new)).status_code == 200


def test_admin_list_and_stats(client, server):
    client.post("/v1/connect", headers=bearer(register(client, 1)), json={"server_id": server[0]})
    s = client.get("/v1/admin/servers", headers=admin_headers()).json()[0]
    assert s["allocated_peers"] == 1 and s["public_key"] == key(200)
    assert client.get("/v1/admin/stats", headers=admin_headers()).json() == {"devices": 1, "peers": 1, "servers": 1}


def test_cleanup_removes_stale_peers(client, server):
    sid, node_token = server
    client.post("/v1/connect", headers=bearer(register(client, 1)), json={"server_id": sid})
    with client.app.state.sessionmaker() as db:
        for p in db.query(Peer).all():
            p.last_used_at = utcnow() - timedelta(days=60)
        db.commit()
        assert cleanup(db, client.app.state.settings) == (1, 0)
    assert client.get("/v1/node/peers", headers=bearer(node_token)).json()["peers"] == []


def test_registration_rate_limit(tmp_path):
    from fastapi.testclient import TestClient
    from app.config import Settings
    from app.main import create_app
    app = create_app(Settings(database_url=f"sqlite:///{tmp_path/'r.db'}", register_limit_per_hour=2))
    with TestClient(app) as c:
        assert c.post("/v1/devices", json={"public_key": key(1)}).status_code == 201
        assert c.post("/v1/devices", json={"public_key": key(2)}).status_code == 201
        assert c.post("/v1/devices", json={"public_key": key(3)}).status_code == 429


def test_endpoint_formats_ipv6():
    assert endpoint("203.0.113.10", 51820) == "203.0.113.10:51820"
    assert endpoint("2001:db8::1", 51820) == "[2001:db8::1]:51820"


def test_privacy_page(tmp_path):
    from fastapi.testclient import TestClient
    from app.config import Settings
    from app.main import create_app
    app = create_app(Settings(database_url=f"sqlite:///{tmp_path/'p.db'}", privacy_contact="help@example.com <x>"))
    with TestClient(app) as c:
        r = c.get("/privacy")
        assert r.status_code == 200 and r.headers["content-type"].startswith("text/html")
        assert "Xalane VPN Privacy Policy" in r.text
        assert "help@example.com &lt;x&gt;" in r.text
        assert "{contact}" not in r.text and "{effective_date}" not in r.text
