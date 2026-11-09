# Run Xalane VPN for free

For the complete deployment, verification and troubleshooting guide (including a paid Lightsail option and home servers), see [deploy-first-server.md](deploy-first-server.md).

This guide puts the whole Xalane backend (the API and one VPN server) on **one free server** from Oracle Cloud, with a free web address from DuckDNS. Cost: $0.

## What you get for free

- **Oracle Cloud Always Free:** an ARM server with up to **2 CPUs and 12 GB memory** in total, and **10 TB of outbound traffic a month**. It never expires.
- **DuckDNS:** a free web address such as `xalane.duckdns.org`, with free HTTPS.

**Limits:**
- **One country.** Oracle's free servers only run in the region you choose at sign-up (your "home region"). That region becomes your VPN location and can't be changed later. More countries need more servers, which cost money (about $5/month each elsewhere).
- Oracle asks for a **bank card to verify your identity**. It places a small temporary hold and doesn't charge you as long as you stay on free resources.
- Free ARM servers are popular. If Oracle says **"Out of capacity"**, try again later, or pick the smaller free server `VM.Standard.E2.1.Micro` (slower but fine to start).
- Oracle can reclaim free servers that sit **almost idle for 7 days**. To prevent this, upgrade the account to *Pay As You Go* (it stays free while you use only free resources), and set a budget alert of $1 so you're warned if anything ever costs money.

## Step 1. Create an Oracle Cloud account

1. Go to **oracle.com/cloud/free** and click *Start for free*.
2. Choose your **home region** carefully. Good choices:
   - Close to East Africa: **Jeddah**, **Dubai**, **Abu Dhabi**, **Johannesburg**
   - Popular VPN locations: **Frankfurt**, **Amsterdam**, **London**
3. Finish sign-up and card verification.

## Step 2. Create the server

1. In the Oracle console, open **Compute → Instances → Create instance**.
2. **Image:** *Canonical Ubuntu 24.04*.
3. **Shape:** *Ampere → VM.Standard.A1.Flex*, at most 2 OCPU and 12 GB memory (the current free limit). If that's out of capacity, use *VM.Standard.E2.1.Micro*.
4. **Networking:** keep *Assign a public IPv4 address* turned on.
5. **SSH keys:** choose *Generate a key pair* and **download the private key**. You need it to log in.
6. Click **Create**. After a minute, copy the server's **Public IP address**.

## Step 3. Open the ports in Oracle's firewall

1. On the instance page, click the **Subnet**, then the **Default Security List**.
2. Click **Add Ingress Rules** and add these three rules (Source CIDR `0.0.0.0/0` for each):

| Protocol | Destination port | Used for |
|---|---|---|
| TCP | 80 | HTTPS certificate |
| TCP | 443 | API (HTTPS) |
| UDP | 51820 | VPN (WireGuard) |

## Step 4. Get a free address from DuckDNS

1. Go to **duckdns.org** and sign in with Google or GitHub.
2. Type a name, for example `xalane`, and click **add domain**. If it's taken, try another name.
3. Paste your server's public IP into the **current ip** box and click **update ip**.

Your API address is now `https://xalane.duckdns.org` (with your chosen name).

## Step 5. Install Xalane on the server

Log in to the server (on Windows, use PowerShell; on Mac or Linux, use Terminal):

```bash
ssh -i path/to/downloaded-key.key ubuntu@YOUR_SERVER_IP
```

Then run these two commands, using your own DuckDNS address:

```bash
git clone -b claude/blissful-feynman-l5nohx https://github.com/jaziirohos-hue/VPN.git && cd VPN
```

```bash
sudo DOMAIN=xalane.duckdns.org bash setup-free-server.sh
```

If the GitHub repository is private, `git clone` asks for your GitHub username and a personal access token (GitHub → Settings → Developer settings → Personal access tokens) instead of your password.

The script takes about 5 minutes. It:
- opens the server's own firewall;
- installs Docker and starts the API with HTTPS;
- installs WireGuard;
- detects the server's country and city and registers it as a Xalane VPN server.

At the end it prints your API address and whether HTTPS is working. Your admin token is saved in `backend/deploy/.env` on the server.

## Step 6. Build the app

Send the API address (for example `https://xalane.duckdns.org`) to whoever builds the app. The build command is:

```bash
./gradlew bundleRelease -PxalaneApiUrl=https://xalane.duckdns.org
```

The app then lists your free server and connects with one tap.

## Useful commands on the server

```bash
cd ~/VPN
export XALANE_API_URL=http://127.0.0.1:8000
export XALANE_ADMIN_TOKEN=$(sudo grep ADMIN_TOKEN backend/deploy/.env | cut -d= -f2)
python3 backend/xalane-admin.py list      # is the VPN server online?
python3 backend/xalane-admin.py stats     # how many phones are registered
sudo journalctl -u xalane-agent -f        # VPN agent log
cd backend/deploy && sudo docker compose logs -f   # API log
```
