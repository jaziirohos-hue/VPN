#!/bin/sh
# Firewall and NAT rules for a Xalane VPN node. Called by wg-quick:
#   PostUp   = /opt/xalane/xalane-firewall.sh up   <wg-if> <tunnel-cidr> <internet-if>
#   PostDown = /opt/xalane/xalane-firewall.sh down <wg-if> <tunnel-cidr> <internet-if>
#
# VPN clients may reach the public internet and this server's HTTPS API, nothing else:
#   - no traffic between VPN clients
#   - no private, link-local (cloud metadata), CGNAT, loopback or multicast addresses
#   - no other services on this server (SSH etc.)
set -eu

ACTION="$1"; WG_IF="$2"; NET="$3"; OUT_IF="$4"
CHAIN=XALANE-FWD
BLOCKED="10.0.0.0/8 172.16.0.0/12 192.168.0.0/16 169.254.0.0/16 100.64.0.0/10 127.0.0.0/8 0.0.0.0/8 224.0.0.0/4 240.0.0.0/4"

remove() {
  iptables -D FORWARD -j "$CHAIN" 2>/dev/null || true
  iptables -F "$CHAIN" 2>/dev/null || true
  iptables -X "$CHAIN" 2>/dev/null || true
  iptables -t nat -D POSTROUTING -s "$NET" -o "$OUT_IF" -j MASQUERADE 2>/dev/null || true
  iptables -D INPUT -i "$WG_IF" -m conntrack --ctstate RELATED,ESTABLISHED -j ACCEPT 2>/dev/null || true
  iptables -D INPUT -i "$WG_IF" -p tcp -m multiport --dports 80,443 -j ACCEPT 2>/dev/null || true
  iptables -D INPUT -i "$WG_IF" -j DROP 2>/dev/null || true
}

case "$ACTION" in
  up)
    remove
    iptables -N "$CHAIN"
    # Replies coming back to clients
    iptables -A "$CHAIN" -o "$WG_IF" -m conntrack --ctstate RELATED,ESTABLISHED -j ACCEPT
    # Client to client
    iptables -A "$CHAIN" -i "$WG_IF" -o "$WG_IF" -j DROP
    for n in $BLOCKED; do
      iptables -A "$CHAIN" -i "$WG_IF" -d "$n" -j DROP
    done
    iptables -A "$CHAIN" -i "$WG_IF" -o "$OUT_IF" -j ACCEPT
    iptables -A "$CHAIN" -i "$WG_IF" -j DROP
    # Inserted first so provider rules (e.g. Oracle's FORWARD REJECT) and Docker's chains don't swallow VPN traffic.
    # Traffic that isn't from or to the VPN falls through unchanged.
    iptables -I FORWARD 1 -j "$CHAIN"
    iptables -t nat -A POSTROUTING -s "$NET" -o "$OUT_IF" -j MASQUERADE
    # Clients may use the API on this server (HTTPS), not other local services
    iptables -I INPUT 1 -i "$WG_IF" -j DROP
    iptables -I INPUT 1 -i "$WG_IF" -p tcp -m multiport --dports 80,443 -j ACCEPT
    iptables -I INPUT 1 -i "$WG_IF" -m conntrack --ctstate RELATED,ESTABLISHED -j ACCEPT
    ;;
  down)
    remove
    ;;
  *)
    echo "usage: $0 up|down <wg-if> <tunnel-cidr> <internet-if>" >&2
    exit 2
    ;;
esac
