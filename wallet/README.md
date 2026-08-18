# 2x2 Wallet API

API JSON para a carteira Android **não-custodial** da moeda **2x2Coin (2X2)**, no mesmo modelo do [InfiniteRicks-new](https://github.com/2x2devcode/InfiniteRicks-new).

O servidor instala em uma VPS Linux, chama **`2x2coin-cli`** (o mesmo cliente do daemon) e expõe `/api/*` para o aplicativo. As chaves privadas nunca saem do celular.

## Módulos

| Módulo | Descrição |
|---|---|
| `x2x-core` | Parâmetros da rede, criptografia, transações e armazenamento da carteira |
| `x2x-api` | Cliente HTTP com TLS, retry, failover e certificate pinning |
| `x2x-server` | API JSON em `127.0.0.1:40012` e explorer em `127.0.0.1:40061` |
| `x2x-android` | Aplicativo Android 15+ (omitido se o SDK não estiver instalado) |

## Parâmetros da rede (mainnet)

Fonte: este repositório (`src/chainparams.cpp`, `src/main.cpp`)

- P2PKH version: `0x03` (endereços começam com `2`)
- P2SH version: `0x5A`
- WIF version: `0x80` (comprimido)
- `COIN = 100_000_000`
- `MIN_TX_FEE = 10_000` satoshis
- Transações incluem campo `nTime` (extensão Peercoin)
- Mensagem: `2x2Coin Signed Message:\n`
- P2P `15190` / RPC `15189`
- Datadir Unix: `~/.2x2coin` / `2x2coin.conf`

## Ubuntu 22.04 — compilar e executar

Na raiz do repositório:

```bash
bash scripts/ubuntu-22.04-api.sh
```

Ou dentro de `wallet/`:

```bash
bash scripts/ubuntu-22.04-api.sh
```

O script instala JDK 17 se necessário, roda os testes Gradle, sobe um `MockRpcServer` quando o daemon não está no ar, inicia API + explorer e valida:

```bash
curl -s http://127.0.0.1:40012/api/health
curl -s http://127.0.0.1:40061/ext/health
```

Para deixar os serviços rodando após o smoke test:

```bash
KEEP_RUNNING=1 bash scripts/ubuntu-22.04-api.sh
```

Com `2x2coind` já sincronizado, `2x2coin-cli` no PATH e `~/.2x2coin/2x2coin.conf` configurado, o script usa o CLI real. Sem daemon, sobe `MockRpcServer` e `scripts/mock-2x2coin-cli.sh` (mesmos argumentos do `2x2coin-cli`).

## VPS com daemon real

```bash
cd wallet
./gradlew :x2x-core:test :x2x-server:installDist
bash scripts/restart-server-services.sh
```

| Serviço | URL pública | Bind local | Rotas |
|---|---|---|---|
| API oficial | `https://server.2x2coin.com` | `127.0.0.1:40012` | `/api/*` |
| Explorer JSON | `https://serverexplorer.2x2coin.com` | `127.0.0.1:40061` | `/ext/*` |
| Explorer público (fallback) | `https://explorer.2x2coin.com` | — | `/ext/getsummary`, `/ext/getbalance/{addr}` |

Documentação: [INSTALLATION.md](docs/INSTALLATION.md), [ARCHITECTURE.md](docs/ARCHITECTURE.md), [DEVELOPER.md](docs/DEVELOPER.md).
