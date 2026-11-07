#!/usr/bin/env python3
"""Xalane node agent.

Runs on each VPN server. Every few seconds it:
  1. sends a heartbeat to the Xalane API (so the server shows as online), and
  2. makes the WireGuard interface's peers match the list the API returns.

Only the Python standard library is used. Settings come from environment variables
(see agent.env.example).
"""
import json
import logging
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request

log = logging.getLogger("xalane-agent")


def run_wg(args: list[str]) -> str:
    return subprocess.run(["wg", *args], check=True, capture_output=True, text=True).stdout


def current_peers(interface: str, wg=run_wg) -> tuple[dict[str, str], int, int, int]:
    """Returns ({public_key: allowed_ips}, active_peers, rx_bytes, tx_bytes) from `wg show dump`."""
    lines = wg(["show", interface, "dump"]).strip().splitlines()
    peers: dict[str, str] = {}
    active = rx = tx = 0
    now = time.time()
    for line in lines[1:]:  # first line describes the interface itself
        f = line.split("\t")
        if len(f) < 8:
            continue
        key, allowed, handshake = f[0], f[3], int(f[4] or 0)
        peers[key] = allowed
        rx += int(f[5] or 0)
        tx += int(f[6] or 0)
        if handshake and now - handshake < 180:
            active += 1
    return peers, active, rx, tx


def plan(current: dict[str, str], desired: dict[str, str]) -> tuple[dict[str, str], list[str]]:
    """Peers to add or update, and peers to remove."""
    upsert = {k: ip for k, ip in desired.items() if current.get(k) != ip}
    remove = [k for k in current if k not in desired]
    return upsert, remove


def apply(interface: str, upsert: dict[str, str], remove: list[str], wg=run_wg) -> None:
    for key, ip in upsert.items():
        wg(["set", interface, "peer", key, "allowed-ips", ip])
    for key in remove:
        wg(["set", interface, "peer", key, "remove"])


class Api:
    def __init__(self, base_url: str, token: str, timeout: int = 10):
        self.base, self.token, self.timeout = base_url.rstrip("/"), token, timeout

    def _call(self, method: str, path: str, body: dict | None = None):
        data = json.dumps(body).encode() if body is not None else None
        req = urllib.request.Request(self.base + path, data=data, method=method, headers={
            "Authorization": f"Bearer {self.token}",
            "Content-Type": "application/json",
            "User-Agent": "xalane-agent/1.0",
        })
        with urllib.request.urlopen(req, timeout=self.timeout) as r:
            raw = r.read()
            return json.loads(raw) if raw else None

    def heartbeat(self, active: int, rx: int, tx: int) -> None:
        self._call("POST", "/v1/node/heartbeat", {"active_peers": active, "rx_bytes": rx, "tx_bytes": tx})

    def desired_peers(self) -> dict[str, str]:
        return {p["public_key"]: p["allowed_ip"] for p in self._call("GET", "/v1/node/peers")["peers"]}


def sync_once(api: Api, interface: str, wg=run_wg) -> tuple[int, int]:
    current, active, rx, tx = current_peers(interface, wg)
    api.heartbeat(active, rx, tx)
    upsert, remove = plan(current, api.desired_peers())
    apply(interface, upsert, remove, wg)
    return len(upsert), len(remove)


def main() -> int:
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    url, token = os.getenv("XALANE_API_URL"), os.getenv("XALANE_NODE_TOKEN")
    if not url or not token:
        log.error("Set XALANE_API_URL and XALANE_NODE_TOKEN")
        return 2
    interface = os.getenv("WG_INTERFACE", "wg0")
    interval = int(os.getenv("SYNC_INTERVAL", "15"))
    api = Api(url, token)
    log.info("syncing %s with %s every %ss", interface, url, interval)
    while True:
        try:
            added, removed = sync_once(api, interface)
            if added or removed:
                log.info("peers: %s added or updated, %s removed", added, removed)
        except urllib.error.HTTPError as e:
            log.error("API returned %s: %s", e.code, e.read()[:200])
        except Exception as e:  # keep running through network blips
            log.error("sync failed: %s", e)
        time.sleep(interval)


if __name__ == "__main__":
    sys.exit(main())
