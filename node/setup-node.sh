#!/usr/bin/env bash
# Sets up a fresh Ubuntu/Debian server as a Xalane VPN node.
#
# Usage (as root):
#   XALANE_API_URL=https://api.example.com XALANE_NODE_TOKEN=... bash setup-node.sh
#
# Without a token it still installs WireGuard and prints the server's public key,
# which you need to register the server with `xalane-admin add-server`.
set -euo pipefail

WG_IF=wg0
WG_PORT="${WG_PORT:-51820}"
TUNNEL_ADDR="${TUNNEL_ADDR:-10.8.0.1/22}"
HERE="$(cd "$(dirname "$0")" && pwd)"

[ "$(id -u)" -eq 0 ] || { echo "Run as root."; exit 1; }

echo "==> Installing WireGuard"
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get install -y -qq wireguard iptables python3 >/dev/null

echo "==> Enabling IP forwarding"
cat >/etc/sysctl.d/99-xalane.conf <<SYSCTL
net.ipv4.ip_forward=1
SYSCTL
sysctl -q --system

OUT_IF="$(ip -o -4 route show to default | awk '{print $5}' | head -n1)"
umask 077
mkdir -p /etc/wireguard /etc/xalane /opt/xalane
if [ ! -f /etc/wireguard/server.key ]; then
  wg genkey | tee /etc/wireguard/server.key | wg pubkey >/etc/wireguard/server.pub
fi

# Network form of TUNNEL_ADDR, e.g. 10.8.0.1/22 -> 10.8.0.0/22
TUNNEL_NET="$(python3 -c "import ipaddress,sys;print(ipaddress.ip_interface(sys.argv[1]).network)" "$TUNNEL_ADDR")"
install -m 755 "${HERE}/xalane-firewall.sh" /opt/xalane/xalane-firewall.sh

echo "==> Writing /etc/wireguard/${WG_IF}.conf"
OLD_SUM="$(sha256sum /etc/wireguard/${WG_IF}.conf 2>/dev/null || true)"
cat >/etc/wireguard/${WG_IF}.conf <<WG
[Interface]
Address = ${TUNNEL_ADDR}
ListenPort = ${WG_PORT}
PrivateKey = $(cat /etc/wireguard/server.key)
# Peers are added by the Xalane agent; do not list them here.
# Internet access only: see /opt/xalane/xalane-firewall.sh
PostUp = /opt/xalane/xalane-firewall.sh up ${WG_IF} ${TUNNEL_NET} ${OUT_IF}
PostDown = /opt/xalane/xalane-firewall.sh down ${WG_IF} ${TUNNEL_NET} ${OUT_IF}
WG
systemctl enable --now wg-quick@${WG_IF} >/dev/null
if [ -n "$OLD_SUM" ] && [ "$OLD_SUM" != "$(sha256sum /etc/wireguard/${WG_IF}.conf)" ]; then
  # Config changed on a re-run: apply it. The agent re-adds peers within seconds.
  systemctl restart wg-quick@${WG_IF}
fi

if command -v ufw >/dev/null && ufw status | grep -q active; then
  ufw allow "${WG_PORT}/udp" >/dev/null
fi

echo "==> Installing the Xalane agent"
install -m 755 "${HERE}/xalane-agent.py" /opt/xalane/xalane-agent.py
install -m 644 "${HERE}/xalane-agent.service" /etc/systemd/system/xalane-agent.service
if [ -n "${XALANE_API_URL:-}" ] && [ -n "${XALANE_NODE_TOKEN:-}" ]; then
  cat >/etc/xalane/agent.env <<ENV
XALANE_API_URL=${XALANE_API_URL}
XALANE_NODE_TOKEN=${XALANE_NODE_TOKEN}
WG_INTERFACE=${WG_IF}
SYNC_INTERVAL=15
ENV
  systemctl daemon-reload
  systemctl enable --now xalane-agent >/dev/null
  echo "==> Agent running. Check it with: journalctl -u xalane-agent -f"
else
  systemctl daemon-reload
  echo "==> Agent installed but not started (no token yet)."
fi

PUBLIC_IP="$(curl -s -4 https://api.ipify.org || true)"
echo
echo "Server public key : $(cat /etc/wireguard/server.pub)"
echo "Public IP         : ${PUBLIC_IP:-unknown}"
echo "WireGuard port    : ${WG_PORT}/udp"
echo "Tunnel network    : ${TUNNEL_ADDR}"
