# 2x2 Wallet API

JSON API for the non-custodial Android wallet of the **2x2** coin, modeled on [InfiniteRicks-new](https://github.com/2x2devcode/InfiniteRicks-new).

The server runs on a Linux VPS, calls **`2x2coin-cli`** (the same client as the daemon), and exposes `/api/*` to the app. Private keys never leave the phone.

Gradle modules keep an `x2x-` prefix in source paths; the coin name is **2x2**.

## Modules

| Module | Description |
|---|---|
| `x2x-core` | Network parameters, cryptography, transactions, wallet storage |
| `x2x-api` | HTTP client with TLS, retry, failover, and certificate pinning |
| `x2x-server` | JSON API on `127.0.0.1:50012` and explorer on `127.0.0.1:50011` |
| `x2x-android` | Android 15+ app (omitted if the SDK is not installed) |

## Mainnet parameters

Source: this repository (`src/chainparams.cpp`, `src/main.cpp`)

- P2PKH version: `0x03` (addresses start with `2`)
- P2SH version: `0x5A`
- WIF version: `0x80` (compressed)
- `COIN = 100_000_000`
- `MIN_TX_FEE = 10_000` satoshis
- Transactions include the `nTime` field (Peercoin extension)
- Message prefix: `2x2Coin Signed Message:\n`
- P2P `15190` / RPC `15189`
- Unix datadir: `~/.2x2coin` / `2x2coin.conf`

## Ubuntu 22.04 — build and run

From the repository root:

```bash
bash scripts/ubuntu-22.04-api.sh
```

Or from `wallet/`:

```bash
bash scripts/ubuntu-22.04-api.sh
```

The script installs JDK 17 if needed, runs Gradle tests, starts a `MockRpcServer` when the daemon is down, brings up the API + explorer, and checks:

```bash
curl -s http://127.0.0.1:50012/api/health
curl -s http://127.0.0.1:50011/ext/health
```

Keep the services running after the smoke test:

```bash
KEEP_RUNNING=1 bash scripts/ubuntu-22.04-api.sh
```

Stop the API and explorer afterwards:

```bash
bash scripts/stop-server-services.sh
```

With a synced `2x2coind`, `2x2coin-cli` on `PATH`, and `~/.2x2coin/2x2coin.conf` set, the script uses the real CLI. Without a daemon it starts `MockRpcServer` and `scripts/mock-2x2coin-cli.sh` (same flags as `2x2coin-cli`).

## VPS with a real daemon

```bash
cd wallet
./gradlew :x2x-core:test :x2x-server:installDist
bash scripts/restart-server-services.sh   # start in the background
bash scripts/stop-server-services.sh      # stop API (50012) and explorer (50011)
```

| Service | Public URL | Local bind | Routes |
|---|---|---|---|
| Official API | `https://server.2x2coin.com` | `127.0.0.1:50012` | `/api/*` |
| JSON explorer | `https://serverexplorer.2x2coin.com` | `127.0.0.1:50011` | `/ext/*` |
| Public explorer (fallback) | `https://explorer.2x2coin.com` | — | `/ext/getsummary`, `/ext/getbalance/{addr}` |

Local (on the VPS) and external (HTTPS from any machine):

```bash
curl -s http://127.0.0.1:50012/api/health
curl -s http://127.0.0.1:50011/ext/health
bash scripts/test-server-external.sh
```

Ports `50012`/`50011` are not public. From outside the VPS use `https://server.2x2coin.com` and `https://serverexplorer.2x2coin.com` (nginx + DNS).

**App developer (no VPS):** [docs/APP_API.md](docs/APP_API.md) — public HTTPS URLs, curl commands, and how the Android client reads API + explorer data.

**Server operator:** [docs/SERVER.md](docs/SERVER.md) — endpoints, JSON, start/stop, nginx, and tests.

Other: [INSTALLATION.md](docs/INSTALLATION.md), [ARCHITECTURE.md](docs/ARCHITECTURE.md), [DEVELOPER.md](docs/DEVELOPER.md).
