# Manual de Instalação

## Requisitos

- Ubuntu 22.04+
- JDK 17+
- Android SDK 35 (somente para `:x2x-android`)
- 2x2Coin daemon (`2x2coind`) para o servidor API

## 1. Clonar e compilar

```bash
git clone https://github.com/2x2devcode/2x2Coin.git
cd 2x2Coin/wallet
./gradlew :x2x-core:test :x2x-api:build :x2x-server:build
```

Atalho Ubuntu 22.04 (instala JDK, compila, testa e sobe a API):

```bash
bash scripts/ubuntu-22.04-api.sh          # na raiz do repositório
# ou
cd wallet && bash scripts/ubuntu-22.04-api.sh
```

> **Nota:** `:x2x-core:test` funciona sem Android SDK. O modulo `:x2x-android` so entra no build quando `local.properties` ou `ANDROID_HOME` apontam para um SDK valido.

## 2. Configurar o daemon 2x2Coin

Edite `~/.2x2coin/2x2coin.conf`:

```ini
server=1
rpcuser=x2xrpc
rpcpassword=<senha-forte>
rpcport=15189
rpcallowip=127.0.0.1
```

Inicie o daemon e aguarde sincronização. **Após alterar `rpcuser`/`rpcpassword`, reinicie o daemon:**

```bash
2x2coind stop
sleep 2
2x2coind -daemon
```

Confirme que `2x2coin-cli getinfo` funciona na VPS. A API JSON chama esse binário (não abre HTTP RPC por conta própria). Os scripts leem `rpcuser`/`rpcpassword` de `~/.2x2coin/2x2coin.conf` e passam para o CLI.

## 3. Subir API e explorer (mesmo servidor)

Compile:

```bash
bash scripts/build-server-services.sh
```

Inicie ambos (JSON apenas, sem interface web):

```bash
# Atualizar codigo, recompilar e reiniciar em segundo plano (recomendado na VPS)
git pull origin main
bash scripts/restart-server-services.sh
```

Para parar API e explorer:

```bash
bash scripts/stop-server-services.sh
```

O script mata os PIDs em `wallet/.run/` e, se ainda houver processo nas portas `50012`/`50011`, encerra quem estiver escutando. Nao para o daemon `2x2coind`.

Para teste interativo (encerra ao pressionar Ctrl+C):

```bash
bash scripts/run-server-services.sh
```

| Serviço | URL pública | Bind local (VPS) | Endpoints |
|---|---|---|---|
| API oficial | `https://server.2x2coin.com` | `127.0.0.1:50012` | `/api/*` |
| Explorer fallback | `https://serverexplorer.2x2coin.com` | `127.0.0.1:50011` | `/ext/*` |

Exemplo nginx (API) — use `scripts/nginx-x2x-api.conf.example` (inclui `proxy_read_timeout 30s`):

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

Exemplo nginx (explorer):

```nginx
server {
    listen 443 ssl;
    server_name serverexplorer.2x2coin.com;
    location / {
        proxy_pass http://127.0.0.1:50011;
    }
}
```

## 4. Testar de fora da VPS

API e explorer escutam só em `127.0.0.1`. Não abra as portas `50012`/`50011` no firewall: o acesso externo é HTTPS `:443` via nginx.

### Pré-requisitos

1. Serviços no ar na VPS (`bash scripts/restart-server-services.sh`)
2. Teste **local** ok:

```bash
curl -s http://127.0.0.1:50012/api/health
curl -s http://127.0.0.1:50011/ext/health
```

Resposta esperada: `{"api":"ok","rpc":"ok"}` e `{"explorer":"ok","rpc":"ok"}`.

3. DNS: registros **A** de `server.2x2coin.com` e `serverexplorer.2x2coin.com` apontando para o IP da VPS
4. nginx + certificado TLS (Let's Encrypt). Exemplo: `scripts/nginx-x2x-api.conf.example`

```bash
sudo apt-get install -y nginx certbot python3-certbot-nginx
sudo cp scripts/nginx-x2x-api.conf.example /etc/nginx/sites-available/x2x-api
sudo ln -sf /etc/nginx/sites-available/x2x-api /etc/nginx/sites-enabled/x2x-api
sudo nginx -t && sudo systemctl reload nginx
sudo certbot --nginx -d server.2x2coin.com -d serverexplorer.2x2coin.com
```

### De qualquer máquina (laptop, celular, CI)

```bash
# atalho
bash scripts/test-server-external.sh

# ou curls manuais
curl -sS https://server.2x2coin.com/api/health
curl -sS https://server.2x2coin.com/api/status
curl -sS https://server.2x2coin.com/api/fee
curl -sS https://server.2x2coin.com/api/address/2NHBXKyRY4ZBvyfyuZ2fZvqaGyo89vMGFW/balance

curl -sS https://serverexplorer.2x2coin.com/ext/health
curl -sS https://serverexplorer.2x2coin.com/ext/getsummary
curl -sS https://serverexplorer.2x2coin.com/ext/getaddress/2NHBXKyRY4ZBvyfyuZ2fZvqaGyo89vMGFW
```

Hosts customizados:

```bash
API_BASE=https://seu-host EXPLORER_BASE=https://seu-explorer bash scripts/test-server-external.sh
```

### Sem DNS ainda (túnel SSH)

Do laptop, sem expor as portas na internet:

```bash
ssh -L 50012:127.0.0.1:50012 -L 50011:127.0.0.1:50011 usuario@IP_DA_VPS
```

Em outro terminal no laptop:

```bash
curl -s http://127.0.0.1:50012/api/health
curl -s http://127.0.0.1:50011/ext/health
```

| Sintoma | Causa |
|---|---|
| `Could not resolve host` | Falta registro A no DNS |
| `Connection refused` / timeout na :443 | nginx parado, firewall ou IP errado |
| `502` / `504` | nginx ok, mas API/explorer local parado ou lento |
| Local ok, HTTPS falha | nginx `proxy_pass` apontando para porta errada |

## 5. Gerar APK

Configure o SDK (uma das opcoes):

```bash
# Opcao A: variavel de ambiente (local.properties e gerado automaticamente)
export ANDROID_HOME=$HOME/Android/Sdk

# Opcao B: script auxiliar
bash scripts/setup-android-sdk.sh

# Opcao C: arquivo manual
cp local.properties.example local.properties
# edite sdk.dir no arquivo
```

Depois compile:

```bash
./gradlew :x2x-android:assembleRelease
```

APK: `x2x-android/build/outputs/apk/release/x2x-android-release.apk`

## 6. Assinatura de release

```bash
bash scripts/generate-release-keystore.sh
```

O script cria `release/x2x-wallet.jks` e `keystore.properties` (ambos gitignored). O Gradle lê essas propriedades automaticamente para `assembleRelease`.

Para senhas personalizadas:

```bash
STORE_PASS='sua-senha' KEY_PASS='sua-senha' bash scripts/generate-release-keystore.sh
```

## 7. Publicar explorer fallback

O endpoint `https://serverexplorer.2x2coin.com/ext/getsummary` deve estar acessível para fallback de rede. O explorer não possui interface web.

## 8. Diagnostico de erros

Se `curl` publico retornar `Server Error` ou JSON `{"error":"..."}`:

```bash
bash scripts/diagnose-server.sh          # na VPS (local)
bash scripts/test-server-external.sh     # de qualquer máquina (HTTPS)
```

`run-server-services.sh` carrega credenciais de `~/.2x2coin/2x2coin.conf` automaticamente.

Causas comuns:

| Sintoma | Causa |
|---|---|
| `2x2coin-cli ... authorization failed` | `X2X_RPC_USER` / `X2X_RPC_PASSWORD` diferentes do daemon, ou `2x2coin-cli` ausente |
| `failed to start 2x2coin-cli` | binario nao esta no PATH — defina `X2X_CLI=/usr/local/bin/2x2coin-cli` |
| `Connection refused` / CLI error | `2x2coind` nao esta rodando ou `server=1` ausente |
| `502` com mensagem do CLI | API/explorer rodando, mas `2x2coin-cli` nao consegue falar com o daemon |
| `Server Error` (texto puro) | Versao antiga sem tratamento JSON — atualize com `git pull` |

Teste local antes do HTTPS:

```bash
curl -s http://127.0.0.1:50012/api/health
curl -s http://127.0.0.1:50011/ext/health
```

