#!/usr/bin/env bash
set -euo pipefail

# shellcheck source=load-rpc-env.sh
source "$(cd "$(dirname "$0")" && pwd)/load-rpc-env.sh"

API_PORT="${API_PORT:-50012}"
EXPLORER_PORT="${EXPLORER_PORT:-50011}"
BIND_HOST="${BIND_HOST:-127.0.0.1}"
CONF="${X2XCOIN_CONF:-${HOME}/.2x2coin/2x2coin.conf}"

read_conf_user() {
  grep -E '^[[:space:]]*rpcuser=' "$CONF" 2>/dev/null | tail -1 | sed -E 's/^[[:space:]]*rpcuser=//' | tr -d '\r' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'
}

rpc_call() {
  local method=$1
  local cli=("${X2X_CLI}")
  if [[ -n "${X2X_RPC_HOST:-}" ]]; then
    cli+=("-rpcconnect=${X2X_RPC_HOST}")
  fi
  if [[ -n "${X2X_RPC_PORT:-}" ]]; then
    cli+=("-rpcport=${X2X_RPC_PORT}")
  fi
  if [[ -n "${X2X_RPC_USER:-}" ]]; then
    cli+=("-rpcuser=${X2X_RPC_USER}")
  fi
  if [[ -n "${X2X_RPC_PASSWORD:-}" ]]; then
    cli+=("-rpcpassword=${X2X_RPC_PASSWORD}")
  fi
  if [[ -f "$CONF" ]]; then
    cli+=("-conf=${CONF}")
  fi
  "${cli[@]}" "${method}"
}

echo "=== Diagnostico 2x2Coin ==="
echo ""

echo "0) Arquivo de configuracao: ${CONF}"
if [[ -f "$CONF" ]]; then
  CONF_USER="$(read_conf_user)"
  echo "   rpcuser no conf: ${CONF_USER}"
  if grep -qE '^[[:space:]]*rpcpassword=' "$CONF"; then
    echo "   rpcpassword no conf: (definido)"
  else
    echo "   rpcpassword no conf: NAO DEFINIDO"
  fi
else
  echo "   FALHA: arquivo nao encontrado"
  CONF_USER=""
fi
echo "   CLI: ${X2X_CLI} -> ${X2X_RPC_HOST}:${X2X_RPC_PORT} user=${X2X_RPC_USER:-<conf/cookie>}"
if [[ -n "${CONF_USER}" && "${X2X_RPC_USER:-}" != "${CONF_USER}" ]]; then
  echo "   AVISO: usuario em uso difere do conf (remova export X2X_RPC_USER antigo do shell)"
fi
echo ""

echo "1) Porta RPC ${X2X_RPC_HOST}:${X2X_RPC_PORT}"
if ! nc -z "$X2X_RPC_HOST" "$X2X_RPC_PORT" 2>/dev/null; then
  echo "   FALHA: nada escutando. Inicie: 2x2coind -daemon"
else
  echo "   OK: porta aberta"
fi
echo ""

echo "2) 2x2coin-cli getinfo"
if ! command -v "${X2X_CLI}" >/dev/null 2>&1; then
  echo "   FALHA: ${X2X_CLI} nao encontrado no PATH"
else
  RPC_RESULT="$(rpc_call getinfo 2>&1 || true)"
  if echo "$RPC_RESULT" | grep -qi 'authorization failed\|incorrect rpcuser'; then
    echo "   FALHA: autenticacao RPC"
    echo "   Confirme rpcuser/rpcpassword em ~/.2x2coin/2x2coin.conf"
    echo "   Reinicie o daemon apos editar o conf:"
    echo "     2x2coind stop ; sleep 2 ; 2x2coind -daemon"
  elif echo "$RPC_RESULT" | grep -q '"blocks"\|blocks'; then
    echo "   OK: $(echo "$RPC_RESULT" | head -c 300)"
  else
    echo "   Resposta: $RPC_RESULT"
  fi
fi
echo ""

check_api() {
  local label=$1
  local url=$2
  echo "$label"
  local body
  body="$(curl -sS --max-time 5 "$url" 2>/dev/null || true)"
  if [[ -z "$body" ]]; then
    echo "   FALHA: sem resposta (servico parado ou porta fechada)"
    echo "   Execute: bash scripts/restart-server-services.sh"
  elif echo "$body" | grep -q 'object mapper configured'; then
    echo "   FALHA: build antigo ainda em execucao (erro Jackson/Javalin)"
    echo "   Execute:"
    echo "     git pull origin main"
    echo "     bash scripts/restart-server-services.sh"
  elif echo "$body" | grep -q '"rpc":"ok"'; then
    echo "   OK: $body"
  elif echo "$body" | grep -q '"error"'; then
    echo "   RPC indisponivel (esperado ate passo 2 OK): $body"
  else
    echo "   Resposta inesperada: $body"
    echo "   Tente: bash scripts/restart-server-services.sh"
  fi
  echo ""
}

check_api "3) API local ${BIND_HOST}:${API_PORT}/api/health" "http://${BIND_HOST}:${API_PORT}/api/health"
check_api "4) Explorer local ${BIND_HOST}:${EXPLORER_PORT}/ext/health" "http://${BIND_HOST}:${EXPLORER_PORT}/ext/health"

TEST_ADDRESS="2NHBXKyRY4ZBvyfyuZ2fZvqaGyo89vMGFW"
echo "5) Balance local (deve responder em <3s, sem nginx)"
BALANCE_START="$(date +%s%3N 2>/dev/null || python3 -c 'import time; print(int(time.time()*1000))')"
BALANCE_BODY="$(curl -sS --max-time 3 "http://${BIND_HOST}:${API_PORT}/api/address/${TEST_ADDRESS}/balance" 2>/dev/null || true)"
BALANCE_END="$(date +%s%3N 2>/dev/null || python3 -c 'import time; print(int(time.time()*1000))')"
ELAPSED=$((BALANCE_END - BALANCE_START))
if echo "$BALANCE_BODY" | grep -q '"address"'; then
  echo "   OK (${ELAPSED}ms): $BALANCE_BODY"
  if (( ELAPSED > 3000 )); then
    echo "   AVISO: lento — confirme git pull + bash scripts/restart-server-services.sh"
  fi
elif [[ -z "$BALANCE_BODY" ]]; then
  echo "   FALHA: sem resposta em 3s (API travada ou build antigo)"
  echo "   Execute na VPS:"
  echo "     git pull origin main"
  echo "     bash scripts/restart-server-services.sh"
else
  echo "   FALHA (${ELAPSED}ms): $BALANCE_BODY"
fi
echo ""

echo "6) Se o passo 5 for OK mas https://server.2x2coin.com der 504,"
echo "   o problema e nginx/proxy — confirme proxy_pass http://127.0.0.1:${API_PORT};"
echo "   Teste HTTPS de fora da VPS: bash scripts/test-server-external.sh"
