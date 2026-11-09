#!/usr/bin/env python3
"""Command-line tool for managing Xalane VPN servers through the admin API.

Set XALANE_API_URL and XALANE_ADMIN_TOKEN, then for example:
  xalane-admin.py add-server --id de-fra-1 --country Germany --code DE --city Frankfurt \
      --host 203.0.113.10 --public-key <server.pub> --lat 50.1 --lon 8.7
  xalane-admin.py list
  xalane-admin.py disable de-fra-1
"""
import argparse
import json
import os
import sys
import urllib.error
import urllib.request


def call(method: str, path: str, body: dict | None = None):
    base = os.environ.get("XALANE_API_URL", "").rstrip("/")
    token = os.environ.get("XALANE_ADMIN_TOKEN", "")
    if not base or not token:
        sys.exit("Set XALANE_API_URL and XALANE_ADMIN_TOKEN first.")
    req = urllib.request.Request(base + path, method=method, data=json.dumps(body).encode() if body is not None else None,
                                 headers={"Authorization": f"Bearer {token}", "Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            raw = r.read()
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as e:
        sys.exit(f"Error {e.code}: {e.read().decode()[:300]}")


def main():
    p = argparse.ArgumentParser(prog="xalane-admin")
    sub = p.add_subparsers(dest="cmd", required=True)
    a = sub.add_parser("add-server", help="Register a new VPN node and print its node token")
    a.add_argument("--id", required=True, help="Short id, e.g. de-fra-1")
    a.add_argument("--country", required=True)
    a.add_argument("--code", required=True, help="Two-letter country code")
    a.add_argument("--city", required=True)
    a.add_argument("--host", required=True, help="Public IP or hostname of the node")
    a.add_argument("--port", type=int, default=51820)
    a.add_argument("--public-key", required=True, help="Contents of /etc/wireguard/server.pub on the node")
    a.add_argument("--cidr", default="10.8.0.0/22", help="Tunnel network; must match TUNNEL_ADDR on the node")
    a.add_argument("--capacity", type=int, default=250)
    a.add_argument("--lat", type=float)
    a.add_argument("--lon", type=float)
    a.add_argument("--streaming", action="store_true")
    sub.add_parser("list", help="List servers")
    sub.add_parser("stats", help="Show device and peer counts")
    u = sub.add_parser("update", help="Change a server's address, key or capacity")
    u.add_argument("id")
    u.add_argument("--host")
    u.add_argument("--public-key")
    u.add_argument("--capacity", type=int)
    for name in ("enable", "disable", "delete", "rotate-token"):
        sub.add_parser(name).add_argument("id")
    args = p.parse_args()

    if args.cmd == "add-server":
        r = call("POST", "/v1/admin/servers", {
            "id": args.id, "country": args.country, "country_code": args.code, "city": args.city,
            "endpoint_host": args.host, "endpoint_port": args.port, "public_key": args.public_key,
            "tunnel_cidr": args.cidr, "capacity": args.capacity, "lat": args.lat, "lon": args.lon,
            "streaming": args.streaming,
        })
        print(f"Added {r['id']}.\nNode token (shown once, put it on the node):\n{r['node_token']}")
    elif args.cmd == "list":
        rows = call("GET", "/v1/admin/servers")
        if not rows:
            print("No servers yet.")
        for s in rows:
            state = "online" if s["online"] else ("disabled" if not s["enabled"] else "offline")
            print(f"{s['id']:<14} {s['country_code']} {s['city']:<16} {s['host']:<22} {state:<8} "
                  f"active {s['active_peers']}/{s['capacity']}  allocated {s['allocated_peers']}")
    elif args.cmd == "stats":
        print(json.dumps(call("GET", "/v1/admin/stats"), indent=2))
    elif args.cmd == "update":
        body = {k: v for k, v in {"endpoint_host": args.host, "public_key": args.public_key, "capacity": args.capacity}.items() if v is not None}
        if not body:
            sys.exit("Nothing to update. Use --host, --public-key or --capacity.")
        call("PATCH", f"/v1/admin/servers/{args.id}", body)
        print(f"{args.id} updated.")
    elif args.cmd in ("enable", "disable"):
        call("PATCH", f"/v1/admin/servers/{args.id}", {"enabled": args.cmd == "enable"})
        print(f"{args.id} {args.cmd}d.")
    elif args.cmd == "delete":
        call("DELETE", f"/v1/admin/servers/{args.id}")
        print(f"{args.id} deleted.")
    elif args.cmd == "rotate-token":
        print(call("POST", f"/v1/admin/servers/{args.id}/rotate-token")["node_token"])


if __name__ == "__main__":
    main()
