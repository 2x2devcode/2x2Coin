# Installation

JSON API and explorer reference (endpoints, JSON, start/stop, tests): **[SERVER.md](SERVER.md)**.

App developers without VPS access: **[APP_API.md](APP_API.md)**.

## Requirements

- Ubuntu 22.04+
- JDK 17+
- Android SDK 35 (only for `:x2x-android`)
- 2x2 daemon (`2x2coind`) for the API server

## 1. Clone and build

```bash
git clone https://github.com/2x2devcode/2x2Coin.git
cd 2x2Coin/wallet
./gradlew :x2x-core:test :x2x-api:build :x2x-server:build
```

Ubuntu 22.04 shortcut (installs JDK, builds, tests, and starts the API):

```bash
bash scripts/ubuntu-22.04-api.sh          # from the repository root
# or
cd wallet && bash scripts/ubuntu-22.04-api.sh
```

> **Note:** `:x2x-core:test` does not need the Android SDK. `:x2x-android` is included only when `local.properties` or `ANDROID_HOME` points at a valid SDK.

## 2. Configure the 2x2 daemon

Edit `~/.2x2coin/2x2coin.conf`:

```ini
server=1
rpcuser=x2xrpc
rpcpassword=<strong-password>
rpcport=15189
rpcallowip=127.0.0.1
```

Start the daemon and wait for sync. **After changing `rpcuser`/`rpcpassword`, restart the daemon:**

```bash
2x2coind stop
sleep 2
2x2coind -daemon
```

Confirm `2x2coin-cli getinfo` works on the VPS. The JSON API calls that binary (it does not open HTTP RPC on its own). Scripts read `rpcuser`/`rpcpassword` from `~/.2x2coin/2x2coin.conf` and pass them to the CLI.

## 3. Start the API and explorer (same host)

Build:

```bash
bash scripts/build-server-services.sh
```

Start both (JSON only, no web UI):

```bash
# Update code, rebuild, and restart in the background (recommended on the VPS)
git pull origin main
bash scripts/restart-server-services.sh
```

Stop the API and explorer:

```bash
bash scripts/stop-server-services.sh
```

The script kills the PIDs in `wallet/.run/` and any process still listening on `50012`/`50011`. It does not stop `2x2coind`.

Interactive test (stops on Ctrl+C):

```bash
bash scripts/run-server-services.sh
```

| Service | Public URL | Local bind (VPS) | Endpoints |
|---|---|---|---|
| Official API | `https://server.2x2coin.com` | `127.0.0.1:50012` | `/api/*` |
| Explorer fallback | `https://serverexplorer.2x2coin.com` | `127.0.0.1:50011` | `/ext/*` |

Nginx example (API) — use `scripts/nginx-x2x-api.conf.example` (includes `proxy_read_timeout 30s`):

```nginx
server {
    listen 443 ssl;
    server_name server.2x2coin.com;
    location / {
        proxy_pass http://127.0.0.1:50012;
        proxy_read_timeout 30s;
    }
}
```

Nginx example (explorer):

```nginx
server {
    listen 443 ssl;
    server_name serverexplorer.2x2coin.com;
    location / {
        proxy_pass http://127.0.0.1:50011;
    }
}
```

## 4. Test from outside the VPS

The API and explorer bind to `127.0.0.1` only. Do not open `50012`/`50011` on the firewall: external access is HTTPS `:443` through nginx.

### Prerequisites

1. Services running on the VPS (`bash scripts/restart-server-services.sh`)
2. **Local** test OK:

```bash
curl -s http://127.0.0.1:50012/api/health
curl -s http://127.0.0.1:50011/ext/health
```

Expected: `{"api":"ok","rpc":"ok"}` and `{"explorer":"ok","rpc":"ok"}`.

3. DNS: **A** records for `server.2x2coin.com` and `serverexplorer.2x2coin.com` pointing at the VPS IP
4. nginx + TLS (Let's Encrypt). Example: `scripts/nginx-x2x-api.conf.example`

```bash
sudo apt-get install -y nginx certbot python3-certbot-nginx
sudo cp scripts/nginx-x2x-api.conf.example /etc/nginx/sites-available/x2x-api
sudo ln -sf /etc/nginx/sites-available/x2x-api /etc/nginx/sites-enabled/x2x-api
sudo nginx -t && sudo systemctl reload nginx
sudo certbot --nginx -d server.2x2coin.com -d serverexplorer.2x2coin.com
```

### From any machine (laptop, phone, CI)

```bash
# shortcut
bash scripts/test-server-external.sh

# or manual curls
curl -sS https://server.2x2coin.com/api/health
curl -sS https://server.2x2coin.com/api/status
curl -sS https://server.2x2coin.com/api/fee
curl -sS https://server.2x2coin.com/api/address/2NHBXKyRY4ZBvyfyuZ2fZvqaGyo89vMGFW/balance

curl -sS https://serverexplorer.2x2coin.com/ext/health
curl -sS https://serverexplorer.2x2coin.com/ext/getsummary
curl -sS https://serverexplorer.2x2coin.com/ext/getaddress/2NHBXKyRY4ZBvyfyuZ2fZvqaGyo89vMGFW
```

Custom hosts:

```bash
API_BASE=https://your-api-host EXPLORER_BASE=https://your-explorer-host bash scripts/test-server-external.sh
```

### No DNS yet (SSH tunnel)

From a laptop, without exposing the ports on the internet:

```bash
ssh -L 50012:127.0.0.1:50012 -L 50011:127.0.0.1:50011 user@VPS_IP
```

In another terminal on the laptop:

```bash
curl -s http://127.0.0.1:50012/api/health
curl -s http://127.0.0.1:50011/ext/health
```

| Symptom | Cause |
|---|---|
| `Could not resolve host` | Missing DNS A record |
| `Connection refused` / timeout on :443 | nginx down, firewall, or wrong IP |
| `502` / `504` | nginx is up, but the local API/explorer is down or slow |
| Local OK, HTTPS fails | nginx `proxy_pass` pointing at the wrong port |

## 5. Build the APK

Configure the SDK (one of):

```bash
# Option A: environment variable (local.properties is generated)
export ANDROID_HOME=$HOME/Android/Sdk

# Option B: helper script
bash scripts/setup-android-sdk.sh

# Option C: manual file
cp local.properties.example local.properties
# edit sdk.dir in the file
```

Then build:

```bash
./gradlew :x2x-android:assembleRelease
```

APK: `x2x-android/build/outputs/apk/release/x2x-android-release.apk`

## 6. Release signing

```bash
bash scripts/generate-release-keystore.sh
```

The script creates `release/x2x-wallet.jks` and `keystore.properties` (both gitignored). Gradle reads those properties automatically for `assembleRelease`.

Custom passwords:

```bash
STORE_PASS='your-password' KEY_PASS='your-password' bash scripts/generate-release-keystore.sh
```

## 7. Publish the explorer fallback

`https://serverexplorer.2x2coin.com/ext/getsummary` must be reachable for network fallback. The explorer has no web UI.

## 8. Troubleshooting

If a public `curl` returns `Server Error` or JSON `{"error":"..."}`:

```bash
bash scripts/diagnose-server.sh          # on the VPS (local)
bash scripts/test-server-external.sh     # from any machine (HTTPS)
```

`run-server-services.sh` loads credentials from `~/.2x2coin/2x2coin.conf` automatically.

Common causes:

| Symptom | Cause |
|---|---|
| `2x2coin-cli ... authorization failed` | `X2X_RPC_USER` / `X2X_RPC_PASSWORD` differ from the daemon, or `2x2coin-cli` is missing |
| `failed to start 2x2coin-cli` | binary not on `PATH` — set `X2X_CLI=/usr/local/bin/2x2coin-cli` |
| `Connection refused` / CLI error | `2x2coind` is not running or `server=1` is missing |
| `502` with a CLI message | API/explorer is up, but `2x2coin-cli` cannot talk to the daemon |
| Plain-text `Server Error` | Old build without JSON errors — update with `git pull` |

Local test before HTTPS:

```bash
curl -s http://127.0.0.1:50012/api/health
curl -s http://127.0.0.1:50011/ext/health
```
