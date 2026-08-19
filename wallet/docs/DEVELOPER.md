# Manual do Desenvolvedor

## Alterar parâmetros da moeda

Edite `x2x-core/.../chain/NetworkParameters.java`.

Todos os módulos dependem desta classe — não duplique constantes.

## Testes

```bash
./gradlew :x2x-core:test
```

Nao e necessario Android SDK para os testes do `x2x-core`. O modulo `:x2x-android` so e incluido quando `ANDROID_HOME` ou `local.properties` apontam para um SDK valido; caso contrario, `./gradlew :x2x-core:test` roda normalmente.

Testes cobrem:
- vetores Base58 do repositório oficial
- round-trip WIF comprimida
- montagem de transação assinada

## Atualizar certificate pin

Pins TLS ainda nao estao gravados no APK (servidores `server.2x2coin.com` / `serverexplorer.2x2coin.com` precisam do certificado em producao). Ate la o cliente HTTP aceita a cadeia do sistema.

Depois do HTTPS no ar, grave o SHA-256 SPKI em `x2x-android/src/main/cpp/pin_config.cpp`.

1. Obtenha o SHA-256 da chave pública TLS:

```bash
echo | openssl s_client -connect server.2x2coin.com:443 -servername server.2x2coin.com 2>/dev/null \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -hex
```

2. Gere bytes XOR para `x2x-android/src/main/cpp/pin_config.cpp` (`getApiPinnedHashes` / `getExplorerPinnedHashes`)
3. Recompile o APK

## Depurar o app Android (adb logcat)

Linux / macOS:

```bash
adb logcat -s X2xWallet:E OkHttp:W AndroidRuntime:E
adb logcat *:E | grep -iE 'X2xWallet|x2xcoin|certificate|pin mismatch|SSL'
```

Windows (PowerShell / CMD):

```bat
adb logcat -s X2xWallet:E OkHttp:W AndroidRuntime:E
adb logcat *:E | findstr /I "X2xWallet x2xcoin certificate pin SSL"
```

Erros comuns no log:

| Mensagem | Causa |
|---|---|
| `certificate pin mismatch` | Certificado TLS mudou — atualize `pin_config.cpp` |
| `HTTP 4xx/5xx` | API/explorer retornando erro |
| `Unable to resolve host` | DNS ou URL errada no APK antigo |

## Adicionar servidor de failover

Em `ApiEndpoints.OFFICIAL_BASE_URLS`, inclua URLs adicionais:

```java
public static final List<String> OFFICIAL_BASE_URLS = List.of(
    NetworkParameters.OFFICIAL_API_BASE_URL,
    "https://server2.2x2coin.com"
);
```

Explorer fallback: `NetworkParameters.EXPLORER_BASE_URL` (`serverexplorer.2x2coin.com`)

## Indexador de saldo (servidor)

A API nao usa mais `getreceivedbyaddress` / `listunspent` do daemon (esses RPCs so enxergam enderecos da carteira do no). O `x2x-server` mantem um indexador on-chain em `~/.x2x-wallet-index` (ou `INDEX_DIR`).

| Variavel | Padrao | Descricao |
|---|---|---|
| `INDEX_DIR` | `~/.x2x-wallet-index` | Pasta do indice persistido |
| `INDEX_START_HEIGHT` | `0` | Bloco inicial da sincronizacao completa |
| `INDEX_FAST_LOOKBACK_WINDOWS` | `30,60,120` | Varredura rapida na API (segundos) |
| `INDEX_FAST_BUDGET_MS` | `6000` | Tempo maximo da consulta rapida |
| `INDEX_LOOKBACK_WINDOWS` | `200,500,1000,2000` | Varredura profunda em segundo plano |
| `INDEX_QUERY_BUDGET_MS` | `60000` | Tempo maximo da varredura profunda |
| `RPC_TIMEOUT_SECONDS` | `8` | Timeout por chamada RPC |

Apos atualizar o servidor na VPS:

```bash
bash scripts/run-server-services.sh
curl -s https://server.2x2coin.com/api/address/2NHBXKyRY4ZBvyfyuZ2fZvqaGyo89vMGFW/balance
bash scripts/test-server-external.sh
```

A primeira consulta pode levar alguns minutos enquanto o indice varre os blocos recentes; a sincronizacao completa continua em segundo plano.

## Endpoints REST esperados

How the Android app consumes these hosts without VPS access (curl cookbook): [APP_API.md](APP_API.md).

Full operator contract (JSON examples, errors, environment variables): [SERVER.md](SERVER.md).

### API oficial (`https://server.2x2coin.com` -> `127.0.0.1:50012`)

| Método | Path |
|---|---|
| GET | `/api/status` |
| GET | `/api/fee` |
| GET | `/api/address/{addr}/balance` |
| GET | `/api/address/{addr}/utxos` |
| GET | `/api/address/{addr}/txs` |
| POST | `/api/tx/broadcast` |
| POST | `/api/cache/invalidate/{addr}` |

### Explorer JSON (`https://serverexplorer.2x2coin.com` -> `127.0.0.1:50011`, sem interface web)

| Método | Path |
|---|---|
| GET | `/ext/getsummary` |
| GET | `/ext/getaddress/{addr}` |

## Serialização de transação

Ordem wire 2x2Coin:
1. `nVersion` (int32)
2. `nTime` (uint32)
3. `vin[]`
4. `vout[]`
5. `nLockTime` (uint32)

O campo `nTime` é obrigatório — difere do Bitcoin Core moderno.
