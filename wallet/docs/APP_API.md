# App developer guide — JSON API and explorer

This document is for **Android / client developers**. You do **not** need SSH, the VPS, `2x2coind`, or `2x2coin-cli`. All chain data the wallet needs is available over **HTTPS from any laptop**.

The coin is **2x2**. There is no API key. There is no web UI. Private keys stay on the phone; the server only returns JSON and relays a signed raw transaction.

VPS operators: see [SERVER.md](SERVER.md).

## Hosts you can call

| Service | Base URL | Used for |
|---|---|---|
| Official wallet API | `https://server.2x2coin.com` | Status, fee, balance, UTXOs, broadcast |
| Wallet explorer JSON | `https://serverexplorer.2x2coin.com` | Fallback status and fallback balance |
| Public block explorer | `https://explorer.2x2coin.com` | Extra fallback the **server** uses; the app does not call this host directly |

Hardcoded in `NetworkParameters` / `ApiEndpoints`:

- `OFFICIAL_API_BASE_URL` = `https://server.2x2coin.com`
- `EXPLORER_BASE_URL` = `https://serverexplorer.2x2coin.com`

Ports `50012` and `50011` are localhost-only on the VPS. From your machine always use `https://…` on port **443**.

## How the app talks to the servers

`X2xWalletApp` builds `X2xApiClient` with those base URLs. `WalletRepository` is the only UI layer that should call the client.

| App action | Java | HTTP |
|---|---|---|
| Home / pull-to-refresh (network) | `apiClient.getStatus()` | `GET /api/status`, then explorer `GET /ext/getsummary` on failure |
| Home / pull-to-refresh (balance) | `apiClient.getBalanceResponse(address)` | `GET /api/address/{addr}/balance`, then explorer `GET /ext/getaddress/{addr}` on failure |
| After send (optional) | `apiClient.invalidateBalanceCache(address)` | `POST /api/cache/invalidate/{addr}` |
| Send — fee | `apiClient.getFeePerKb()` | `GET /api/fee` (falls back to `10000` sat/kB) |
| Send — coins to spend | `apiClient.getUtxos(address)` | `GET /api/address/{addr}/utxos` |
| Send — broadcast | `apiClient.broadcast(rawHex)` | `POST /api/tx/broadcast` |

Signing happens **on the device** (`TransactionSigner`). The API never sees a private key.

Retry: up to 3 attempts with 500 ms / 1 s / 1.5 s backoff on official API calls.

## Commands (laptop)

Replace `ADDR` with a mainnet P2PKH address (starts with `2`). Example used in smoke tests:

```bash
ADDR=2NHBXKyRY4ZBvyfyuZ2fZvqaGyo89vMGFW
API=https://server.2x2coin.com
EXPLORER=https://serverexplorer.2x2coin.com
```

Pretty-print optional: pipe any `curl` to `jq .`.

Run every check at once (same commands the CI/laptop script uses):

```bash
bash scripts/test-server-external.sh
```

Custom hosts:

```bash
API_BASE="$API" EXPLORER_BASE="$EXPLORER" TEST_ADDRESS="$ADDR" bash scripts/test-server-external.sh
```

---

### 1. Is the API up?

```bash
curl -sS "$API/api/health"
```

```json
{"api":"ok","rpc":"ok"}
```

If you get `{"error":"..."}` with HTTP 502, the HTTP front is up but the node behind it is not. The app does not call `/api/health`; it is for you to debug from outside.

### 2. Network status (home screen)

```bash
curl -sS "$API/api/status"
```

```json
{
  "online": true,
  "chain": "main",
  "blocks": 182240,
  "headers": 182240,
  "progress": 100.0,
  "peers": 30
}
```

If this fails, the client calls the explorer:

```bash
curl -sS "$EXPLORER/ext/getsummary"
```

```json
{
  "blockcount": 182240,
  "supply": "29226600.2365954",
  "connections": 30
}
```

Map explorer → `NetworkStatus`: `blocks`/`headers` = `blockcount`, `peers` = `connections`, `source` = `explorer-fallback`.

### 3. Suggested fee (send)

```bash
curl -sS "$API/api/fee"
```

```json
{"feePerKbSatoshis": 10000}
```

On failure the app uses `10000` satoshis per kB (`NetworkParameters.DEFAULT_FEE_PER_KB`).

### 4. Balance (home / receive)

```bash
curl -sS "$API/api/address/${ADDR}/balance"
```

```json
{
  "balance": "12.50000000",
  "address": "2NHBXKyRY4ZBvyfyuZ2fZvqaGyo89vMGFW",
  "indexedHeight": 182200,
  "chainTip": 182240,
  "scanning": true,
  "source": "index"
}
```

| Field | App use |
|---|---|
| `balance` | String in 2x2 (8 decimals), **not** satoshis |
| `scanning` | `true` → index still catching up; show a “updating” state, do not treat `0` as final |
| `source` | `index` or `explorer` (server-side fallback) |
| `indexedHeight` / `chainTip` | How far the server index is vs the chain |

If the official call fails, the client uses:

```bash
curl -sS "$EXPLORER/ext/getaddress/${ADDR}"
```

```json
{
  "balance": "12.50000000",
  "final_balance": "12.50000000"
}
```

Read `balance`, or `final_balance` if `balance` is missing.

### 5. UTXOs (send — inputs)

```bash
curl -sS "$API/api/address/${ADDR}/utxos"
```

```json
{
  "utxos": [
    {
      "txid": "abc...",
      "vout": 0,
      "amountSatoshis": 1250000000,
      "confirmations": 12
    }
  ]
}
```

The app only spends UTXOs with `confirmations >= 1`. Amounts here **are satoshis**.

### 6. Transaction history

```bash
curl -sS "$API/api/address/${ADDR}/txs"
```

```json
{"transactions": []}
```

Reserved. Do not depend on this for UI yet.

### 7. Drop cached balance (after send)

```bash
curl -sS -X POST -H 'Content-Type: application/json' \
  -d '{}' \
  "$API/api/cache/invalidate/${ADDR}"
```

```json
{"ok": true}
```

The repository already does this when `invalidateCache` is true; ignore HTTP errors (cache will expire in ~15 s anyway).

### 8. Broadcast (send — last step)

Build and **sign on the device**, then POST hex. The server calls `sendrawtransaction`; it does not sign.

```bash
curl -sS -X POST -H 'Content-Type: application/json' \
  -d '{"rawTx":"<hex>"}' \
  "$API/api/tx/broadcast"
```

```json
{"txid":"..."}
```

Wire format must include Peercoin `nTime` (see [DEVELOPER.md](DEVELOPER.md)). A Bitcoin-Core-style tx without `nTime` will be rejected.

Do not broadcast real funds from a laptop unless you intend to spend.

---

## End-to-end flows (commands + app)

### Sync / home refresh

```bash
curl -sS "$API/api/status"
# on failure:
curl -sS "$EXPLORER/ext/getsummary"

curl -sS "$API/api/address/${ADDR}/balance"
# on failure:
curl -sS "$EXPLORER/ext/getaddress/${ADDR}"
```

If `scanning` is `true` and `balance` is `"0.00000000"`, wait and retry; the server index may still be scanning recent blocks.

### Send

```bash
curl -sS "$API/api/fee"
curl -sS "$API/api/address/${ADDR}/utxos"
# device: select confirmed UTXOs, sign locally
curl -sS -X POST -H 'Content-Type: application/json' \
  -d '{"rawTx":"<hex>"}' \
  "$API/api/tx/broadcast"
curl -sS -X POST -H 'Content-Type: application/json' -d '{}' \
  "$API/api/cache/invalidate/${ADDR}"
curl -sS "$API/api/address/${ADDR}/balance"
```

## Client code map

```
x2x-android  X2xWalletApp          → builds OkHttp + X2xApiClient
x2x-android  WalletRepository      → getStatus, getBalanceResponse, getUtxos, getFeePerKb, broadcast
x2x-api      X2xApiClient          → HTTPS GET/POST, retries, explorer fallback
x2x-api      ApiEndpoints          → path constants
x2x-core     NetworkParameters     → hosts, fee, address version
```

User-Agent: `2x2Coin-Wallet/1.1.7`. TLS pinning in the APK is currently empty (system trust store). After production certs are stable, pins go in `x2x-android/src/main/cpp/pin_config.cpp` ([DEVELOPER.md](DEVELOPER.md)).

## Errors you will see from outside

All JSON errors look like `{"error":"…"}`.

| What you see | Meaning | What the app should do |
|---|---|---|
| `Could not resolve host` | DNS for `server.2x2coin.com` / `serverexplorer.2x2coin.com` is not live yet | Keep explorer fallback; do not hard-fail the UI |
| HTTP 404 `not found` | Wrong path | Check `ApiEndpoints` |
| HTTP 502 `{"error":"..."}` | Node/CLI behind nginx failed | Retry; then explorer for status/balance |
| HTTP 504 / empty body | nginx timeout | Retry; show last known balance |
| `scanning: true`, balance `0` | Server index not caught up | Poll; do not show “empty wallet” as final |
| Broadcast HTTP 502 | Invalid hex, missing `nTime`, or node reject | Surface the `error` string |

There is no authentication. Anyone can read public address data. Spending still requires a key on the device.

## What you cannot do without the VPS

- Inspect `2x2coin-cli` / daemon logs
- Hit `http://127.0.0.1:50012` or `:50011`
- Restart the Java processes

If HTTPS is down, report the failing `curl -sS -D -` (status line + body) to the operator. They use [SERVER.md](SERVER.md).
