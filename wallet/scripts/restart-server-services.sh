#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=load-rpc-env.sh
source "$ROOT_DIR/scripts/load-rpc-env.sh"

BIND_HOST="${BIND_HOST:-127.0.0.1}"
API_PORT="${API_PORT:-50012}"
EXPLORER_PORT="${EXPLORER_PORT:-50011}"
LOG_DIR="${LOG_DIR:-${ROOT_DIR}/logs}"
PID_DIR="${PID_DIR:-${ROOT_DIR}/.run}"

if ! command -v "${X2X_CLI}" >/dev/null 2>&1; then
  echo "ERRO: ${X2X_CLI} nao encontrado no PATH."
  echo "Instale 2x2coin-cli ou defina X2X_CLI=/caminho/2x2coin-cli"
  exit 1
fi

echo "=== Reiniciando servicos 2x2Coin ==="
echo ""

BIND_HOST="${BIND_HOST}" API_PORT="${API_PORT}" EXPLORER_PORT="${EXPLORER_PORT}" PID_DIR="${PID_DIR}" \
  bash "$ROOT_DIR/scripts/stop-server-services.sh"

echo "Compilando API e explorer..."
"$ROOT_DIR/scripts/build-server-services.sh"

LIB_DIR="${ROOT_DIR}/x2x-server/build/install/x2x-server/lib"
mkdir -p "${LOG_DIR}" "${PID_DIR}"

export BIND_HOST X2X_CLI X2X_RPC_HOST X2X_RPC_PORT X2X_RPC_USER X2X_RPC_PASSWORD X2XCOIN_CONF X2X_DATADIR

echo "Iniciando API em ${BIND_HOST}:${API_PORT}..."
nohup env PORT="${API_PORT}" java -cp "${LIB_DIR}/*" com.x2xcoin.wallet.server.X2xServer \
  >"${LOG_DIR}/x2x-api.log" 2>&1 &
echo $! > "${PID_DIR}/x2x-api.pid"

echo "Iniciando explorer em ${BIND_HOST}:${EXPLORER_PORT}..."
nohup env EXPLORER_PORT="${EXPLORER_PORT}" java -cp "${LIB_DIR}/*" com.x2xcoin.wallet.server.X2xExplorerServer \
  >"${LOG_DIR}/x2x-explorer.log" 2>&1 &
echo $! > "${PID_DIR}/x2x-explorer.pid"

sleep 2

echo ""
echo "Testando saude local..."
API_BODY="$(curl -sS "http://${BIND_HOST}:${API_PORT}/api/health" 2>/dev/null || true)"
EXPLORER_BODY="$(curl -sS "http://${BIND_HOST}:${EXPLORER_PORT}/ext/health" 2>/dev/null || true)"

if echo "${API_BODY}" | grep -q '"rpc":"ok"'; then
  echo "  API:      OK"
  BALANCE_START="$(date +%s%3N 2>/dev/null || python3 -c 'import time; print(int(time.time()*1000))')"
  BALANCE_BODY="$(curl -sS --max-time 5 "http://${BIND_HOST}:${API_PORT}/api/address/2NHBXKyRY4ZBvyfyuZ2fZvqaGyo89vMGFW/balance" 2>/dev/null || true)"
  BALANCE_END="$(date +%s%3N 2>/dev/null || python3 -c 'import time; print(int(time.time()*1000))')"
  if echo "${BALANCE_BODY}" | grep -q '"address"'; then
    ELAPSED=$((BALANCE_END - BALANCE_START))
    echo "  Balance:  OK (${ELAPSED}ms, local bypass nginx)"
    echo "            ${BALANCE_BODY}"
  else
    echo "  Balance:  FALHA (deve responder em <5s apos este deploy)"
    echo "            ${BALANCE_BODY}"
  fi
else
  echo "  API:      FALHA"
  echo "            ${API_BODY}"
  echo "  Log:      ${LOG_DIR}/x2x-api.log"
fi

if echo "${EXPLORER_BODY}" | grep -q '"rpc":"ok"'; then
  echo "  Explorer: OK"
else
  echo "  Explorer: FALHA"
  echo "            ${EXPLORER_BODY}"
  echo "  Log:      ${LOG_DIR}/x2x-explorer.log"
fi

echo ""
echo "PIDs: API=$(cat "${PID_DIR}/x2x-api.pid"), Explorer=$(cat "${PID_DIR}/x2x-explorer.pid")"
echo "Para diagnostico completo: bash scripts/diagnose-server.sh"
