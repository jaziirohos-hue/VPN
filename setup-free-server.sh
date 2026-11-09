#!/usr/bin/env bash
# Sets up a complete Xalane VPN on ONE server: the control API (with HTTPS) and a VPN node.
# Made for free servers such as Oracle Cloud Always Free (Ubuntu 22.04/24.04, ARM or x86).
#
# Usage, as root, from the repository folder:
#   sudo DOMAIN=xalane.duckdns.org bash setup-free-server.sh
#
# Optional: SERVER_ID=de-fra-1 COUNTRY=Germany CODE=DE CITY=Frankfurt LAT=50.1 LON=8.7
# (detected automatically from the server's IP when not given)
# Optional: WG_PORT=51820 (VPN UDP port), ENDPOINT_HOST=<name phones use for the VPN>.
# ENDPOINT_HOST defaults to the server's public IPv4. On a home connection whose IP changes,
# set ENDPOINT_HOST to your DuckDNS name so phones follow DuckDNS updates.
set -euo pipefail

REPO="$(cd "$(dirname "$0")" && pwd)"
DOMAIN="${DOMAIN:-}"
WG_PORT="${WG_PORT:-51820}"
export WG_PORT
[ "$(id -u)" -eq 0 ] || { echo "Run as root: sudo DOMAIN=your.domain bash setup-free-server.sh"; exit 1; }
[ -n "$DOMAIN" ] || { echo "Set DOMAIN, for example: sudo DOMAIN=xalane.duckdns.org bash setup-free-server.sh"; exit 1; }

say() { echo; echo "==> $*"; }

say "Installing basic tools"
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get install -y -qq curl ca-certificates python3 openssl iptables >/dev/null

say "Opening ports 80, 443 (HTTPS) and ${WG_PORT}/udp (WireGuard) in the server firewall"
# Oracle's Ubuntu images block everything except SSH with iptables rules.
for rule in "-p tcp --dport 80" "-p tcp --dport 443" "-p udp --dport ${WG_PORT}"; do
  # shellcheck disable=SC2086
  iptables -C INPUT $rule -j ACCEPT 2>/dev/null || iptables -I INPUT 1 $rule -j ACCEPT
done
if command -v netfilter-persistent >/dev/null; then netfilter-persistent save >/dev/null 2>&1 || true; fi

if ! command -v docker >/dev/null; then
  say "Installing Docker"
  curl -fsSL https://get.docker.com | sh >/dev/null
fi

say "Configuring the API for https://${DOMAIN}"
ENV_FILE="${REPO}/backend/deploy/.env"
if [ ! -f "$ENV_FILE" ]; then
  sed -e "s#^API_DOMAIN=.*#API_DOMAIN=${DOMAIN}#" -e "s#^ADMIN_TOKEN=.*#ADMIN_TOKEN=$(openssl rand -hex 32)#" \
    "${REPO}/backend/deploy/.env.example" > "$ENV_FILE"
  chmod 600 "$ENV_FILE"
fi
ADMIN_TOKEN="$(grep '^ADMIN_TOKEN=' "$ENV_FILE" | cut -d= -f2)"

say "Starting the API (first build takes a few minutes)"
(cd "${REPO}/backend/deploy" && docker compose up -d --build)
for _ in $(seq 1 60); do
  curl -fs http://127.0.0.1:8000/health >/dev/null && break
  sleep 3
done
curl -fs http://127.0.0.1:8000/health >/dev/null || { echo "The API did not start. See: cd backend/deploy && docker compose logs"; exit 1; }

say "Installing WireGuard"
bash "${REPO}/node/setup-node.sh" >/dev/null
PUBKEY="$(cat /etc/wireguard/server.pub)"
PUBLIC_IP="$(curl -fs -4 https://api.ipify.org)"
ENDPOINT_HOST="${ENDPOINT_HOST:-$PUBLIC_IP}"

say "Detecting the server's location"
GEO="$(curl -fs "http://ip-api.com/json/${PUBLIC_IP}?fields=country,countryCode,city,lat,lon" || echo '{}')"
geo() { python3 -c "import json,sys;print(json.loads(sys.argv[1]).get('$1',''))" "$GEO"; }
COUNTRY="${COUNTRY:-$(geo country)}"; CODE="${CODE:-$(geo countryCode)}"; CITY="${CITY:-$(geo city)}"
LAT="${LAT:-$(geo lat)}"; LON="${LON:-$(geo lon)}"
COUNTRY="${COUNTRY:-Unknown}"; CODE="${CODE:-XX}"; CITY="${CITY:-Server}"
SERVER_ID="${SERVER_ID:-$(echo "${CODE}-${CITY}-1" | tr '[:upper:]' '[:lower:]' | tr -c 'a-z0-9-\n' '-' | cut -c1-40)}"

export XALANE_API_URL=http://127.0.0.1:8000 XALANE_ADMIN_TOKEN="$ADMIN_TOKEN"
if python3 "${REPO}/backend/xalane-admin.py" list | grep -q "^${SERVER_ID} "; then
  say "Server ${SERVER_ID} is already registered; updating its address and key"
  python3 "${REPO}/backend/xalane-admin.py" update "$SERVER_ID" --host "$ENDPOINT_HOST" --public-key "$PUBKEY" >/dev/null
  NODE_TOKEN="$(python3 "${REPO}/backend/xalane-admin.py" rotate-token "$SERVER_ID")"
else
  say "Registering this machine as VPN server ${SERVER_ID} (${CITY}, ${COUNTRY})"
  GEO_ARGS=()
  [ -n "$LAT" ] && GEO_ARGS+=(--lat "$LAT")
  [ -n "$LON" ] && GEO_ARGS+=(--lon "$LON")
  NODE_TOKEN="$(python3 "${REPO}/backend/xalane-admin.py" add-server --id "$SERVER_ID" --country "$COUNTRY" --code "$CODE" \
    --city "$CITY" --host "$ENDPOINT_HOST" --port "$WG_PORT" --public-key "$PUBKEY" "${GEO_ARGS[@]}" | tail -n1)"
fi

say "Starting the VPN node agent"
XALANE_API_URL=http://127.0.0.1:8000 XALANE_NODE_TOKEN="$NODE_TOKEN" bash "${REPO}/node/setup-node.sh" >/dev/null
sleep 20
python3 "${REPO}/backend/xalane-admin.py" list

HTTPS_OK=no
curl -fs "https://${DOMAIN}/health" >/dev/null && HTTPS_OK=yes

cat <<DONE

------------------------------------------------------------
 Xalane VPN is running on this server.

 API address   : https://${DOMAIN}   (HTTPS working: ${HTTPS_OK})
 VPN server    : ${SERVER_ID} - ${CITY}, ${COUNTRY}
 VPN endpoint  : ${ENDPOINT_HOST}:${WG_PORT}/udp
 Admin token   : saved in backend/deploy/.env (keep it secret)

 Build the app with:  -PxalaneApiUrl=https://${DOMAIN}
DONE
if [ "$HTTPS_OK" = no ]; then
  cat <<HINT

 HTTPS is not working yet. Check that:
  - ${DOMAIN} points to ${PUBLIC_IP}
  - ports 80 and 443 (TCP) and ${WG_PORT} (UDP) are open in your cloud
    provider's firewall (Oracle: VCN > Security List > Ingress Rules)
 Then run: cd backend/deploy && docker compose restart caddy
HINT
fi
echo "------------------------------------------------------------"
