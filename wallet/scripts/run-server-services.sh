#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=load-rpc-env.sh
source "$ROOT_DIR/scripts/load-rpc-env.sh"

BIND_HOST="${BIND_HOST:-127.0.0.1}"
API_PORT="${API_PORT:-40012}"
EXPLORER_PORT="${EXPLORER_PORT:-40061}"

if [[ -z "${X2X_RPC_USER:-}" || -z "${X2X_RPC_PASSWORD:-}" ]]; then
  echo "ERRO: credenciais RPC nao encontradas."
  echo "Defina X2X_RPC_USER e X2X_RPC_PASSWORD ou configure ~/.2x2coin/2x2coin.conf"
  exit 1
fi

LIB_DIR="$ROOT_DIR/x2x-server/build/install/x2x-server/lib"
if [[ ! -d "$LIB_DIR" ]]; then
  echo "Distribuicao nao encontrada. Compilando..."
  "$ROOT_DIR/scripts/build-server-services.sh"
fi

cleanup() {
  if [[ -n "${API_PID:-}" ]]; then
    kill "$API_PID" 2>/dev/null || true
  fi
  if [[ -n "${EXPLORER_PID:-}" ]]; then
    kill "$EXPLORER_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

echo "Iniciando servicos JSON em ${BIND_HOST} (sem interface web)..."
echo "  API:      ${BIND_HOST}:${API_PORT}  -> https://server.2x2coin.com"
echo "  Explorer: ${BIND_HOST}:${EXPLORER_PORT}  -> https://serverexplorer.2x2coin.com"
echo "  RPC:      ${X2X_RPC_HOST}:${X2X_RPC_PORT} (user=${X2X_RPC_USER})"

export BIND_HOST X2X_RPC_HOST X2X_RPC_PORT X2X_RPC_USER X2X_RPC_PASSWORD
PORT="$API_PORT" java -cp "$LIB_DIR/*" com.x2xcoin.wallet.server.X2xServer &
API_PID=$!

EXPLORER_PORT="$EXPLORER_PORT" java -cp "$LIB_DIR/*" com.x2xcoin.wallet.server.X2xExplorerServer &
EXPLORER_PID=$!

echo "API PID ${API_PID}, Explorer PID ${EXPLORER_PID}"
echo "Teste: curl -s http://${BIND_HOST}:${API_PORT}/api/health"
echo "Pressione Ctrl+C para encerrar."

wait
