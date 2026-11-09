# Xalane VPN backend

Two parts:

- **Control API** (`backend/`): Python + FastAPI. Keeps the server list, registers phones anonymously and hands each phone its own WireGuard settings for the server it picks.
- **Node agent** (`node/`): runs on every VPN server. Sends a heartbeat and keeps WireGuard's peer list in sync with the API.

```
 Phone ──HTTPS──▶ Control API ◀──HTTPS (poll every 15 s)── Node agent ─▶ wg0
   │                                                        (VPN server)
   └────────────── WireGuard tunnel (UDP 51820) ──────────────▶
```

The phone makes its own WireGuard key pair and only sends the public key. The API never sees private keys or traffic.

## API

| Method | Path | Who | Purpose |
|---|---|---|---|
| GET | `/health` | anyone | Health check |
| POST | `/v1/devices` | app | Register `{public_key}` → `{device_id, token}` (rate limited per IP) |
| DELETE | `/v1/devices/me` | app | Delete this phone's data |
| GET | `/v1/servers` | app | Server list with online state and load |
| POST | `/v1/connect` | app | `{server_id}` → interface address, DNS and server peer settings |
| POST | `/v1/node/heartbeat` | node | Marks the node online |
| GET | `/v1/node/peers` | node | Peers the node should allow |
| GET/POST/PATCH/DELETE | `/v1/admin/servers…` | admin | Manage servers, rotate node tokens |
| GET | `/v1/admin/stats` | admin | Device, peer and server counts |

Interactive docs are at `/docs` once the API runs.

Housekeeping runs hourly: tunnel addresses unused for `PEER_TTL_DAYS` (30) are freed, and phones inactive for `DEVICE_TTL_DAYS` (180) are deleted.

**First deployment:** follow [docs/deploy-first-server.md](../docs/deploy-first-server.md) for step-by-step setup, checks and troubleshooting.

**Want it free?** Follow [docs/free-server-guide.md](../docs/free-server-guide.md): one free Oracle Cloud server runs both the API and a VPN server, set up by `setup-free-server.sh`.

## 1. Deploy the API

Any small Linux VPS works (1 CPU, 1 GB RAM). You need a domain name pointing at it, for example `api.xalane.com`.

```bash
git clone https://github.com/jaziirohos-hue/VPN.git
cd VPN/backend/deploy
cp .env.example .env
```

Edit `.env`: set `API_DOMAIN` and a long random `ADMIN_TOKEN` (`openssl rand -hex 32`). Then:

```bash
docker compose up -d
```

Caddy gets an HTTPS certificate automatically. Check it: `curl https://api.xalane.com/health`.

The database is SQLite in the `xalane-data` Docker volume. Back it up regularly.

## 2. Add a VPN server

On a fresh Ubuntu or Debian VPS in the country you want:

```bash
git clone https://github.com/jaziirohos-hue/VPN.git
cd VPN/node
sudo bash setup-node.sh
```

It installs WireGuard, turns on forwarding and NAT, and prints the server's **public key** and **public IP**.

On your own computer, register it (needs Python 3):

```bash
export XALANE_API_URL=https://api.xalane.com
export XALANE_ADMIN_TOKEN=your-admin-token
python3 backend/xalane-admin.py add-server --id de-fra-1 --country Germany --code DE --city Frankfurt \
  --host <public IP> --public-key <public key> --lat 50.1 --lon 8.7
```

It prints a **node token**. Back on the VPN server, run the setup again with it:

```bash
sudo XALANE_API_URL=https://api.xalane.com XALANE_NODE_TOKEN=<node token> bash setup-node.sh
```

Within 15 seconds `python3 backend/xalane-admin.py list` shows the server as **online**, and it appears in the app.

Make sure UDP port 51820 is open in the VPS provider's firewall.

## 3. Point the app at the API

Build the app with the API address:

```bash
./gradlew bundleRelease -PxalaneApiUrl=https://api.xalane.com
```

or set `xalaneApiUrl` in `gradle.properties`.

## Admin commands

```bash
python3 backend/xalane-admin.py list
python3 backend/xalane-admin.py stats
python3 backend/xalane-admin.py disable de-fra-1
python3 backend/xalane-admin.py enable de-fra-1
python3 backend/xalane-admin.py rotate-token de-fra-1
python3 backend/xalane-admin.py delete de-fra-1
```

## Development

```bash
cd backend
python3 -m venv .venv && .venv/bin/pip install -r requirements-dev.txt
.venv/bin/pytest                      # API tests
.venv/bin/pytest ../node/tests        # agent tests
ADMIN_TOKEN=dev .venv/bin/uvicorn app.main:app --reload
```

## Settings (environment variables)

| Name | Default | Meaning |
|---|---|---|
| `DATABASE_URL` | `sqlite:///./xalane.db` | SQLAlchemy URL (PostgreSQL also works) |
| `ADMIN_TOKEN` | (empty, admin API off) | Secret for admin endpoints |
| `VPN_DNS` | `1.1.1.1,1.0.0.1` | DNS given to phones |
| `NODE_TIMEOUT_SECONDS` | `90` | How long a node counts as online after a heartbeat |
| `PEER_TTL_DAYS` | `30` | Free unused tunnel addresses after this |
| `DEVICE_TTL_DAYS` | `180` | Delete inactive phones after this |
| `REGISTER_LIMIT_PER_HOUR` | `200` | Registrations per IP per hour (mobile networks share IPs) |
