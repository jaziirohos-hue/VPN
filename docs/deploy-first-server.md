# Deploy the first Xalane VPN server

One Ubuntu server runs both the HTTPS API and the first WireGuard VPN node. Every step is labeled:

- **[Your computer]**: your laptop or desktop
- **[Server]**: the Ubuntu server, after you connect with SSH
- **[Phone]**: your Android phone with Xalane VPN installed

Nothing in this guide buys anything. Creating a server at AWS, or upgrading an Oracle account, can cost money: decide that yourself in the provider's console.

**Secrets stay private.** Never paste these into chat, screenshots, issues or Git: your SSH private key (`.pem`/`.key`), the admin token (`backend/deploy/.env`), node tokens (`/etc/xalane/agent.env`), your DuckDNS token, and WireGuard private keys (`/etc/wireguard/server.key`). The installer keeps them in root-only files and never prints them.

---

## 1. Choose where the server runs

The server needs: Ubuntu 22.04 or 24.04, a **stable public IPv4 address**, and inbound **TCP 80**, **TCP 443** and **UDP 51820**. The API's internal port 8000 stays private (it listens on `127.0.0.1` only).

### Recommended paid pilot: Amazon Lightsail, Frankfurt

- **Price:** the Linux plans with a public IPv4 start at **$5/month** (2 vCPU, 0.5 GB memory, 20 GB disk, 1 TB transfer). Choose the **$7/month** plan (1 GB memory, 2 TB transfer): the API's Docker build is tight in 0.5 GB. Prices are listed per bundle, not per region, so confirm the Frankfurt price on the create-instance page.
- **Data transfer:** both directions count toward the allowance; only outbound traffic above it is charged ($0.09/GB in Frankfurt).
- **Stable IPv4:** a Lightsail **static IP is free while attached** to an instance (a small hourly fee applies if it sits unattached).
- **Firewall:** Lightsail has its own allow-only firewall per instance (Networking tab).
- **Requires:** an AWS account, which needs a payment card.

Why Frankfurt: traffic from Somalia usually reaches the internet through East African and European cable routes, and Frankfurt is a major exchange point. **Measure, don't assume** (next section).

#### Test which region is fastest from Somalia

1. **[Phone]** On Somali mobile data, open **cloudping.info** (or any "AWS latency test" page) and press *HTTP Ping*. Note the times for **Frankfurt (eu-central-1)**, **Paris (eu-west-3)** and **Mumbai (ap-south-1)**. Do it again on Wi-Fi.
2. Pick the region with the lowest and most stable time. Under ~150 ms is good for a VPN.
3. After creating the server, test the real thing from the phone: install any ping app and ping the server's static IP. Compare a few times of day.

### Free fallback: Oracle Cloud Always Free

- **Current limits (official docs):** Ampere A1 Arm instances totaling **2 OCPUs and 12 GB memory** (1,500 OCPU-hours and 9,000 GB-hours a month), plus two small AMD micro instances. Oracle's free-tier pages list **10 TB/month** of outbound data. Some users reported their larger A1 instances being disabled when the limit dropped in August 2026, so check *Governance → Limits, Quotas and Usage* in your console.
- **Home region only:** Always Free servers can only be created in the home region you pick at sign-up, and it can't be changed. That region is your VPN location.
- **Availability:** popular regions often show *Out of capacity* for A1. Retry later, try another availability domain, or use `VM.Standard.E2.1.Micro`.
- **Idle reclamation:** Oracle may reclaim an Always Free instance if, over 7 days, its 95th-percentile CPU, network and (for A1) memory use are all under 20%. A quiet VPN can look idle. Upgrading to Pay As You Go avoids this, but that's a billing change: only do it if you accept that risk, and set a budget alert.
- **Requires:** a payment card for identity verification. Oracle doesn't currently accept Somalia as a billing country; the country, address and card must match.

### Home Ubuntu PC (only if your connection allows it)

This works only if your internet connection gives your router a **public IPv4 address**:

1. **[Your computer]** Compare the router's WAN/Internet IP (router admin page) with `curl -4 https://api.ipify.org`. If they differ, or the router shows an address in `100.64.0.0`–`100.127.255.255`, you are behind **CGNAT** and incoming connections can't reach you. Most Somali mobile and many fixed connections use CGNAT; ask your provider for a public IP.
2. In the router, forward **TCP 80**, **TCP 443** and **UDP 51820** to the PC's local IP, and give the PC a fixed local IP (DHCP reservation).
3. Keep DuckDNS updated automatically (see section 3.4).
4. Run the installer with `ENDPOINT_HOST=yourname.duckdns.org`. Why: by default each VPN server is registered with its **IPv4 address**, and phones connect to that address. If your home IP changes, updating DuckDNS fixes the API's HTTPS address but phones would keep connecting to the **old IP** stored for the VPN endpoint. With `ENDPOINT_HOST` set to the DuckDNS name, phones look up the current IP each time they connect.

---

## 2. What the code review found

Fixed in the repository (verified):

| Issue | Effect | Fix | How it was verified |
|---|---|---|---|
| VPN forwarding rules were appended after existing rules | On Oracle images (which end the FORWARD chain with a REJECT rule), phones handshake but get **no internet** | New `node/xalane-firewall.sh` inserts its own chain first | Simulated server with an Oracle-style REJECT rule in Linux network namespaces: old rules blocked the internet, new rules allowed it |
| VPN clients could reach private networks and cloud metadata (`169.254.169.254`) | A user could query the cloud provider's metadata service or other internal hosts | Firewall drops private, link-local, CGNAT, loopback and multicast destinations | Same simulation: 10.0.0.5, 192.168.1.1 and 169.254.169.254 blocked; the internet reachable; NAT source is the server |
| VPN clients could reach each other and services on the server (SSH) | Users could probe each other's phones and the server | Client-to-client traffic dropped; from the tunnel only TCP 80/443 on the server is allowed | Simulation: phone-to-phone ping blocked (drop counter incremented); server 443 reachable, 22 blocked |
| Registration limit of 20 per IP per hour | On mobile networks many phones share one public IP (CGNAT), so real users would be refused | Default raised to 200 | Config and tests |
| Re-running the installer kept the old endpoint address and key | Changing the IP or reinstalling WireGuard broke connections | Re-run now updates the server's host and public key (`xalane-admin.py update`) | Tested against a running API |
| Endpoint always the IPv4 | Home servers with changing IPs break | `ENDPOINT_HOST` option | Script lint (shellcheck) |

Still needs a live server or your phone (not verified here): Let's Encrypt certificate issuance for your DuckDNS name, the provider firewalls, WireGuard on a real kernel, a real handshake from a Somali mobile network, behavior after reboot, and real speeds.

---

## 3. Deploy

### 3.1 Create the server

**Lightsail** (in the Lightsail console):
1. *Create instance* → region **Frankfurt** (or your fastest) → platform **Linux/Unix** → blueprint **OS Only → Ubuntu 24.04 LTS**.
2. SSH key: create a new key pair and **download it** (keep it private).
3. Plan: **$7 (1 GB)** with public IPv4. Name it `xalane-1`. Create.
4. *Networking → Create static IP* → attach it to `xalane-1`. Write down the static IP.
5. Instance → *Networking → IPv4 Firewall*: keep SSH (TCP 22) and add **HTTP (TCP 80)**, **HTTPS (TCP 443)**, and **Custom UDP 51820**.

**Oracle** (in the OCI console):
1. *Compute → Instances → Create* → image **Canonical Ubuntu 24.04** → shape **VM.Standard.A1.Flex**, up to **2 OCPU / 12 GB** (or `VM.Standard.E2.1.Micro`).
2. Networking: keep *Assign a public IPv4 address*. For a stable address, reserve one: *Networking → IP management → Reserved public IPs*, then attach it to the instance's VNIC.
3. SSH keys: *Generate a key pair* and **download the private key**.
4. Subnet → *Default Security List → Add Ingress Rules*, source `0.0.0.0/0`: **TCP 80**, **TCP 443**, **UDP 51820**. (The installer opens the same ports in Ubuntu's own firewall.)

### 3.2 Point DuckDNS at the server

**[Your computer]** On duckdns.org, next to your name (e.g. `xalane`), put the server's static IP in *current ip* → *update ip*. Check:

```bash
nslookup xalane.duckdns.org
```

Expected: the answer shows your server's IP. Wait a few minutes if it doesn't yet.

If you don't control `xalane.duckdns.org`, use your own name everywhere below (e.g. `mynamevpn.duckdns.org`) and, after deploying, set it in the app: **Settings → Xalane server → `https://mynamevpn.duckdns.org` → Save**. No rebuild is needed. (To make it the default for everyone, build with `./gradlew bundleRelease -PxalaneApiUrl=https://mynamevpn.duckdns.org`.)

### 3.3 Connect and install

**[Your computer]** Protect the key and connect (Lightsail user is `ubuntu`; Oracle Ubuntu is also `ubuntu`):

```bash
chmod 600 ~/Downloads/xalane-key.pem
ssh -i ~/Downloads/xalane-key.pem ubuntu@SERVER_IP
```

On Windows PowerShell the same `ssh` command works; if it complains about key permissions, right-click the key → Properties → Security and leave only your user.

**[Server]**

```bash
sudo apt-get update
sudo apt-get install -y git
git clone --branch claude/blissful-feynman-l5nohx https://github.com/jaziirohos-hue/VPN.git
cd VPN
sudo DOMAIN=xalane.duckdns.org bash setup-free-server.sh
```

For a home server with a changing IP add `ENDPOINT_HOST=xalane.duckdns.org` before `bash`. The run takes about 5 minutes and ends with a summary including `HTTPS working: yes`.

### 3.4 (Home server only) Keep DuckDNS updated

**[Server]** Replace `xalane` and run `sudo crontab -e`, then add one line (your token is on the DuckDNS page; it stays in root's crontab, not in Git):

```
*/5 * * * * curl -fsS "https://www.duckdns.org/update?domains=xalane&token=YOUR_DUCKDNS_TOKEN&ip=" >/dev/null
```

---

## 4. Verify

| # | Where | Command / action | Expected |
|---|---|---|---|
| 1 | [Your computer] | `curl -sS https://xalane.duckdns.org/health` | `{"status":"ok"}` and **no** certificate warning |
| 2 | [Your computer] | `curl -sS https://xalane.duckdns.org/v1/servers` | One server with `"online":true` |
| 2b | [Server] | `cd ~/VPN && export XALANE_API_URL=http://127.0.0.1:8000 XALANE_ADMIN_TOKEN=$(sudo grep ADMIN_TOKEN backend/deploy/.env \| cut -d= -f2) && python3 backend/xalane-admin.py list` | Your server, `online` |
| 3 | [Phone] | Turn off Wi-Fi (use Somali mobile data). Open Xalane → choose the server → big button → accept Android's VPN prompt | "Connection secure", timer running, speeds moving |
| 4 | [Server] | `sudo wg show wg0` | A `peer:` with `latest handshake: N seconds ago` and growing `transfer` |
| 5 | [Phone] | Before and after connecting, open `https://api.ipify.org` | Before: your mobile IP. After: the **server's** IPv4 |
| 6 | [Phone] | Open `https://dnsleaktest.com` → *Standard test* | Only Cloudflare (or AdGuard if *Block ads* is on), not your mobile operator |
| 7 | [Phone] | Open `https://test-ipv6.com` while connected | "No IPv6" (intended: IPv6 is sent into the tunnel and dropped, so it can't leak around the VPN) |
| 8 | [Server] | `sudo reboot`, wait 2 minutes, reconnect with SSH | Checks 1, 2 and 3 pass again without doing anything |

## 5. Troubleshooting

**HTTPS fails (check 1):**
```bash
nslookup xalane.duckdns.org                       # [Your computer] must show the server IP
cd ~/VPN/backend/deploy && sudo docker compose logs caddy | tail -50   # [Server]
```
Look for certificate errors. Usual causes: DuckDNS points elsewhere, or TCP 80/443 is closed in the provider firewall. After fixing: `sudo docker compose restart caddy`.

**Server offline (check 2):**
```bash
sudo journalctl -u xalane-agent -n 50            # [Server]
curl -s http://127.0.0.1:8000/health             # [Server] API up locally?
```
`401` in the agent log means a wrong node token: re-run the installer (it rotates the token).

**No handshake (check 4 shows no `latest handshake`):**
- UDP 51820 closed in the provider firewall (most common), or on the server: `sudo iptables -S INPUT | grep 51820`.
- The phone has a stale server address. In the app, pick the server again.
- Some networks block or throttle UDP 51820. Try port 443/UDP (Caddy only uses 443/TCP): open UDP 443 in the provider firewall, re-run the installer with `WG_PORT=443`, and the server's registration is updated.

**Handshake but no internet:**
```bash
sudo sysctl net.ipv4.ip_forward                  # must be 1
sudo iptables -S XALANE-FWD                      # chain must exist
sudo iptables -t nat -S POSTROUTING | grep MASQUERADE
ip -o -4 route show to default                   # the interface name must match the one in /etc/wireguard/wg0.conf PostUp
```
If the default interface changed, re-run the installer. If websites stall but small pages load, it's MTU: the Android app uses 1280 by default, which suits mobile networks.

## 6. Security check before public launch

The firewall installed by `xalane-firewall.sh` lets VPN clients reach only the public internet and the server's HTTPS API. To confirm on the live server:

**[Server]**
```bash
sudo iptables -S XALANE-FWD
```
Expected: DROP rules for `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, `169.254.0.0/16`, `100.64.0.0/10`, a `-i wg0 -o wg0 -j DROP`, then ACCEPT to the internet interface.

**[Phone]** while connected, open `http://169.254.169.254/` in the browser: it must **time out**. Normal websites must still load.

Also before launch:
- Update the server regularly: `sudo apt-get update && sudo apt-get upgrade -y`, and `cd ~/VPN && git pull && sudo DOMAIN=... bash setup-free-server.sh` for Xalane updates.
- Back up the database: `sudo docker run --rm -v deploy_xalane-data:/data -v $PWD:/b alpine tar czf /b/xalane-db.tgz -C /data .` (run in `~/VPN/backend/deploy`; keep the file private).
- Keep SSH key-only (the cloud images already disable password login).

## 7. Add more locations

Start with one working location. Then, for each new country (for example Amsterdam for Europe, Virginia for the Americas, Singapore for Asia):

1. **[Provider console]** Create an Ubuntu server, give it a stable IPv4, and open **UDP 51820** only (it needs no 80/443).
2. **[New server]**
   ```bash
   sudo apt-get update && sudo apt-get install -y git
   git clone --branch claude/blissful-feynman-l5nohx https://github.com/jaziirohos-hue/VPN.git
   cd VPN/node && sudo bash setup-node.sh
   ```
   Note the printed **public key** and **public IP**.
3. **[First server]** (it holds the admin token)
   ```bash
   cd ~/VPN && export XALANE_API_URL=http://127.0.0.1:8000 XALANE_ADMIN_TOKEN=$(sudo grep ADMIN_TOKEN backend/deploy/.env | cut -d= -f2)
   python3 backend/xalane-admin.py add-server --id nl-ams-1 --country Netherlands --code NL --city Amsterdam \
     --host NEW_SERVER_IP --public-key NEW_SERVER_PUBLIC_KEY --lat 52.4 --lon 4.9
   ```
   It prints a node token. Copy it directly into the next command; don't save it anywhere else.
4. **[New server]**
   ```bash
   cd ~/VPN/node && sudo XALANE_API_URL=https://xalane.duckdns.org XALANE_NODE_TOKEN=PASTE_TOKEN bash setup-node.sh
   ```
5. Within 15 seconds `xalane-admin.py list` shows it online, and the app lists it on the next refresh.

Each node can use the default tunnel network `10.8.0.0/22`; nodes are independent.

---

Sources: [Lightsail pricing](https://aws.amazon.com/lightsail/pricing/), [Lightsail data transfer](https://docs.aws.amazon.com/lightsail/latest/userguide/amazon-lightsail-faq-data-transfer-allowance.html), [Lightsail static IPs](https://docs.aws.amazon.com/lightsail/latest/userguide/understanding-static-ip-addresses-in-amazon-lightsail.html), [Lightsail firewalls](https://docs.aws.amazon.com/lightsail/latest/userguide/understanding-firewall-and-port-mappings-in-amazon-lightsail.html), [Oracle Always Free resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm), [Oracle Free Tier](https://www.oracle.com/cloud/free/), [DuckDNS spec](https://www.duckdns.org/spec.jsp).
