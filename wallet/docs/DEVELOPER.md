# Developer notes

## Coin parameters

Edit `x2x-core/.../chain/NetworkParameters.java`.

Every module depends on this class — do not duplicate constants. The coin name is **2x2**.

## Tests

```bash
./gradlew :x2x-core:test
```

The Android SDK is not required for `:x2x-core:test`. `:x2x-android` is included only when `ANDROID_HOME` or `local.properties` points at a valid SDK; otherwise `./gradlew :x2x-core:test` still runs.

Tests cover:

- official Base58 vectors
- compressed WIF round-trip
- signed transaction assembly

## Update the certificate pin

TLS pins are not stored in the APK yet (`server.2x2coin.com` / `serverexplorer.2x2coin.com` need a production certificate). Until then the HTTP client trusts the system store.

After HTTPS is live, store the SHA-256 SPKI in `x2x-android/src/main/cpp/pin_config.cpp`.

1. Get the TLS public-key SHA-256:

```bash
echo | openssl s_client -connect server.2x2coin.com:443 -servername server.2x2coin.com 2>/dev/null \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -hex
```

2. Generate XOR bytes for `x2x-android/src/main/cpp/pin_config.cpp` (`getApiPinnedHashes` / `getExplorerPinnedHashes`)
3. Rebuild the APK

## Debug the Android app (adb logcat)

Linux / macOS:

```bash
adb logcat -s X2xWallet:E OkHttp:W AndroidRuntime:E
adb logcat *:E | grep -iE 'X2xWallet|2x2coin|certificate|pin mismatch|SSL'
```

Windows (PowerShell / CMD):

```bat
adb logcat -s X2xWallet:E OkHttp:W AndroidRuntime:E
adb logcat *:E | findstr /I "X2xWallet 2x2coin certificate pin SSL"
```

Common log errors:

| Message | Cause |
|---|---|
| `certificate pin mismatch` | TLS certificate changed — update `pin_config.cpp` |
| `HTTP 4xx/5xx` | API/explorer returning an error |
| `Unable to resolve host` | DNS or a wrong URL in an old APK |

## Add a failover server

In `ApiEndpoints.OFFICIAL_BASE_URLS`, add extra URLs:

```java
public static final List<String> OFFICIAL_BASE_URLS = List.of(
    NetworkParameters.OFFICIAL_API_BASE_URL,
    "https://server2.2x2coin.com"
);
```

Explorer fallback: `NetworkParameters.EXPLORER_BASE_URL` (`serverexplorer.2x2coin.com`)

## Balance indexer (server)

The API does not use daemon `getreceivedbyaddress` / `listunspent` (those RPCs only see the node wallet). The server keeps an on-chain indexer in `~/.x2x-wallet-index` (or `INDEX_DIR`).

| Variable | Default | Description |
|---|---|---|
| `INDEX_DIR` | `~/.x2x-wallet-index` | Persisted index directory |
| `INDEX_START_HEIGHT` | `0` | First block of the full sync |
| `INDEX_FAST_LOOKBACK_WINDOWS` | `30,60,120` | Fast lookback windows |
| `INDEX_FAST_BUDGET_MS` | `6000` | Fast query time budget |
| `INDEX_LOOKBACK_WINDOWS` | `200,500,1000,2000` | Deep scan windows |
| `INDEX_QUERY_BUDGET_MS` | `60000` | Deep scan time budget |
| `RPC_TIMEOUT_SECONDS` | `8` | Timeout per RPC/CLI call |

After updating the server on the VPS:

```bash
bash scripts/run-server-services.sh
curl -s https://server.2x2coin.com/api/address/2NHBXKyRY4ZBvyfyuZ2fZvqaGyo89vMGFW/balance
bash scripts/test-server-external.sh
```

The first query can take a few minutes while the index scans recent blocks; the full sync continues in the background.

## Expected REST endpoints

How the Android app consumes these hosts without VPS access (curl cookbook): [APP_API.md](APP_API.md).

Full operator contract (JSON examples, errors, environment variables): [SERVER.md](SERVER.md).

### Official API (`https://server.2x2coin.com` -> `127.0.0.1:50012`)

| Method | Path |
|---|---|
| GET | `/api/status` |
| GET | `/api/fee` |
| GET | `/api/address/{addr}/balance` |
| GET | `/api/address/{addr}/utxos` |
| GET | `/api/address/{addr}/txs` |
| POST | `/api/tx/broadcast` |
| POST | `/api/cache/invalidate/{addr}` |

### Explorer JSON (`https://serverexplorer.2x2coin.com` -> `127.0.0.1:50011`, no web UI)

| Method | Path |
|---|---|
| GET | `/ext/getsummary` |
| GET | `/ext/getaddress/{addr}` |

## Transaction serialization

2x2 wire order:

1. `nVersion` (int32)
2. `nTime` (uint32)
3. `vin[]`
4. `vout[]`
5. `nLockTime` (uint32)

The `nTime` field is required — it differs from modern Bitcoin Core.
