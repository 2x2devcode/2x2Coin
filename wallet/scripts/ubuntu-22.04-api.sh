#!/usr/bin/env bash
# Compile and run the 2x2 wallet API on Ubuntu 22.04.
# Usage:
#   bash scripts/ubuntu-22.04-api.sh
#   bash wallet/scripts/ubuntu-22.04-api.sh
#
# The script installs JDK 17 if needed, compiles x2x-server, runs tests,
# then talks to the node through 2x2coin-cli (or mock-2x2coin-cli.sh).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [[ -d "$SCRIPT_DIR/../x2x-server" ]]; then
  WALLET_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
elif [[ -d "$SCRIPT_DIR/../wallet/x2x-server" ]]; then
  WALLET_DIR="$(cd "$SCRIPT_DIR/../wallet" && pwd)"
else
  echo "ERRO: nao encontrei o projeto wallet/ (x2x-server)."
  exit 1
fi
cd "$WALLET_DIR"

BIND_HOST="${BIND_HOST:-127.0.0.1}"
API_PORT="${API_PORT:-40012}"
EXPLORER_PORT="${EXPLORER_PORT:-40061}"
MOCK_RPC_PORT="${MOCK_RPC_PORT:-18589}"
KEEP_RUNNING="${KEEP_RUNNING:-0}"
SKIP_APT="${SKIP_APT:-0}"

echo "=== 2x2 Wallet API — Ubuntu 22.04 ==="
echo "Pasta: $WALLET_DIR"
echo ""

if [[ "$SKIP_APT" != "1" ]] && command -v apt-get >/dev/null 2>&1; then
  echo "1) Dependencias (openjdk-17, curl)..."
  if ! command -v java >/dev/null 2>&1 || ! java -version 2>&1 | grep -q '17\|18\|19\|20\|21\|22\|23\|24'; then
    if command -v sudo >/dev/null 2>&1 && sudo -n true 2>/dev/null; then
      sudo apt-get update -y
      sudo DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-17-jdk-headless curl ca-certificates
    elif [[ "$(id -u)" -eq 0 ]]; then
      apt-get update -y
      DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-17-jdk-headless curl ca-certificates
    else
      echo "   AVISO: sem permissao para apt-get. Usando o JDK ja instalado."
    fi
  else
    echo "   JDK ja presente."
  fi
  command -v curl >/dev/null 2>&1 || {
    if command -v sudo >/dev/null 2>&1 && sudo -n true 2>/dev/null; then
      sudo DEBIAN_FRONTEND=noninteractive apt-get install -y curl
    elif [[ "$(id -u)" -eq 0 ]]; then
      DEBIAN_FRONTEND=noninteractive apt-get install -y curl
    fi
  }
else
  echo "1) Pulando apt-get (SKIP_APT=$SKIP_APT)."
fi

if ! command -v java >/dev/null 2>&1; then
  echo "ERRO: Java nao encontrado. Instale openjdk-17-jdk e rode de novo."
  exit 1
fi
echo "   $(java -version 2>&1 | head -1)"
echo ""

echo "2) Compilando e testando..."
chmod +x "$WALLET_DIR/gradlew"
./gradlew --no-daemon :x2x-core:test :x2x-api:build :x2x-server:test :x2x-server:installDist
echo ""

# shellcheck source=load-rpc-env.sh
source "$WALLET_DIR/scripts/load-rpc-env.sh"

USE_MOCK=0
if [[ "${X2X_FORCE_MOCK:-}" == "1" ]]; then
  USE_MOCK=1
elif ! command -v "${X2X_CLI}" >/dev/null 2>&1; then
  USE_MOCK=1
elif ! "${X2X_CLI}" ${X2X_RPC_HOST:+-rpcconnect="$X2X_RPC_HOST"} ${X2X_RPC_PORT:+-rpcport="$X2X_RPC_PORT"} \
      ${X2X_RPC_USER:+-rpcuser="$X2X_RPC_USER"} ${X2X_RPC_PASSWORD:+-rpcpassword="$X2X_RPC_PASSWORD"} \
      getblockcount >/dev/null 2>&1; then
  USE_MOCK=1
fi

MOCK_PID=""
API_PID=""
EXPLORER_PID=""
cleanup() {
  if [[ -n "${API_PID}" ]]; then kill "$API_PID" 2>/dev/null || true; fi
  if [[ -n "${EXPLORER_PID}" ]]; then kill "$EXPLORER_PID" 2>/dev/null || true; fi
  if [[ -n "${MOCK_PID}" ]]; then kill "$MOCK_PID" 2>/dev/null || true; fi
}
trap cleanup EXIT INT TERM

LIB_DIR="$WALLET_DIR/x2x-server/build/install/x2x-server/lib"
INDEX_DIR="${INDEX_DIR:-$WALLET_DIR/.run/x2x-wallet-index}"
mkdir -p "$INDEX_DIR" "$WALLET_DIR/.run"

if [[ "$USE_MOCK" -eq 1 ]]; then
  echo "3) 2x2coin-cli/daemon indisponivel — subindo MockRpcServer + mock-2x2coin-cli"
  export X2X_RPC_HOST="${BIND_HOST}"
  export X2X_RPC_PORT="${MOCK_RPC_PORT}"
  export X2X_RPC_USER="${X2X_RPC_USER:-x2xrpc}"
  export X2X_RPC_PASSWORD="${X2X_RPC_PASSWORD:-x2xrpc}"
  export EXPLORER_FALLBACK_ENABLED="${EXPLORER_FALLBACK_ENABLED:-false}"
  export X2X_SERVER_LIB="$LIB_DIR"
  export X2X_CLI="$WALLET_DIR/scripts/mock-2x2coin-cli.sh"
  chmod +x "$X2X_CLI"
  nohup java -cp "$LIB_DIR/*" com.x2xcoin.wallet.server.MockRpcServer \
    >"$WALLET_DIR/.run/x2x-mock-rpc.log" 2>&1 &
  MOCK_PID=$!
  sleep 1
else
  echo "3) Usando ${X2X_CLI} -> ${X2X_RPC_HOST}:${X2X_RPC_PORT}"
fi

export BIND_HOST INDEX_DIR EXPLORER_FALLBACK_ENABLED
export X2X_CLI X2X_SERVER_LIB X2X_RPC_HOST X2X_RPC_PORT X2X_RPC_USER X2X_RPC_PASSWORD X2XCOIN_CONF X2X_DATADIR

echo "4) Iniciando API ${BIND_HOST}:${API_PORT} e explorer ${BIND_HOST}:${EXPLORER_PORT}..."
nohup env PORT="$API_PORT" java -cp "$LIB_DIR/*" com.x2xcoin.wallet.server.X2xServer \
  >"$WALLET_DIR/.run/x2x-api.log" 2>&1 &
API_PID=$!
nohup env EXPLORER_PORT="$EXPLORER_PORT" java -cp "$LIB_DIR/*" com.x2xcoin.wallet.server.X2xExplorerServer \
  >"$WALLET_DIR/.run/x2x-explorer.log" 2>&1 &
EXPLORER_PID=$!

for _ in 1 2 3 4 5 6 7 8 9 10; do
  if curl -sf --max-time 1 "http://${BIND_HOST}:${API_PORT}/api/health" >/dev/null 2>&1; then
    break
  fi
  sleep 0.5
done

echo ""
echo "5) Testes HTTP locais:"
fail=0
check() {
  local label=$1
  local url=$2
  local expect=$3
  local body
  body="$(curl -sS --max-time 5 "$url" || true)"
  if echo "$body" | grep -q "$expect"; then
    echo "   OK  $label -> $body"
  else
    echo "   FALHA $label -> $body"
    fail=1
  fi
}

check "API health" "http://${BIND_HOST}:${API_PORT}/api/health" '"rpc":"ok"'
check "API status" "http://${BIND_HOST}:${API_PORT}/api/status" '"online":true'
check "API fee" "http://${BIND_HOST}:${API_PORT}/api/fee" 'feePerKbSatoshis'
check "API balance" "http://${BIND_HOST}:${API_PORT}/api/address/2NHBXKyRY4ZBvyfyuZ2fZvqaGyo89vMGFW/balance" '"address"'
check "Explorer health" "http://${BIND_HOST}:${EXPLORER_PORT}/ext/health" '"rpc":"ok"'
check "Explorer summary" "http://${BIND_HOST}:${EXPLORER_PORT}/ext/getsummary" 'blockcount'

echo ""
if [[ "$fail" -ne 0 ]]; then
  echo "Alguns testes HTTP falharam. Logs:"
  tail -40 "$WALLET_DIR/.run/x2x-api.log" 2>/dev/null || true
  tail -20 "$WALLET_DIR/.run/x2x-mock-rpc.log" 2>/dev/null || true
  exit 1
fi

echo "API 2x2 pronta."
echo "  curl -s http://${BIND_HOST}:${API_PORT}/api/health"
echo "  curl -s http://${BIND_HOST}:${EXPLORER_PORT}/ext/health"
if [[ "$KEEP_RUNNING" == "1" ]]; then
  echo "KEEP_RUNNING=1 — servicos continuam (Ctrl+C encerra)."
  wait
else
  echo "Encerrando processos de smoke test (use KEEP_RUNNING=1 para deixar no ar)."
fi
