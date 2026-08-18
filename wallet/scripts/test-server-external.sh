#!/usr/bin/env bash
set -euo pipefail

# Testa API e explorer como um cliente externo (laptop, celular, CI).
# As portas 50012/50011 nao sao publicas: o acesso externo e HTTPS :443 via nginx.

API_BASE="${API_BASE:-https://server.2x2coin.com}"
EXPLORER_BASE="${EXPLORER_BASE:-https://serverexplorer.2x2coin.com}"
TEST_ADDRESS="${TEST_ADDRESS:-2NHBXKyRY4ZBvyfyuZ2fZvqaGyo89vMGFW}"
TIMEOUT="${TIMEOUT:-15}"

host_of() {
  local url=$1
  url="${url#https://}"
  url="${url#http://}"
  echo "${url%%/*}"
}

check() {
  local label=$1
  local url=$2
  local expect=$3
  local body http elapsed
  local start end
  start="$(date +%s%3N 2>/dev/null || python3 -c 'import time; print(int(time.time()*1000))')"
  http="$(curl -sS --max-time "${TIMEOUT}" -o /tmp/x2x-ext-body.txt -w '%{http_code}' "${url}" 2>/tmp/x2x-ext-err.txt || true)"
  end="$(date +%s%3N 2>/dev/null || python3 -c 'import time; print(int(time.time()*1000))')"
  elapsed=$((end - start))
  body="$(cat /tmp/x2x-ext-body.txt 2>/dev/null || true)"
  if [[ -z "${http}" || "${http}" == "000" ]]; then
    echo "  FALHA  ${label}"
    echo "         ${url}"
    echo "         $(tr '\n' ' ' < /tmp/x2x-ext-err.txt 2>/dev/null | sed 's/[[:space:]]*$//')"
    echo "         Sem DNS, TLS ou nginx? Veja wallet/docs/INSTALLATION.md"
    return 1
  fi
  if [[ "${http}" != "200" ]]; then
    echo "  FALHA  ${label}  HTTP ${http} (${elapsed}ms)"
    echo "         ${url}"
    echo "         ${body}"
    return 1
  fi
  if ! echo "${body}" | grep -q "${expect}"; then
    echo "  FALHA  ${label}  HTTP ${http} (${elapsed}ms) — JSON inesperado"
    echo "         ${url}"
    echo "         ${body}"
    return 1
  fi
  echo "  OK     ${label}  HTTP ${http} (${elapsed}ms)"
  echo "         ${body}"
}

echo "=== Teste externo 2x2Coin ==="
echo "  API:      ${API_BASE}"
echo "  Explorer: ${EXPLORER_BASE}"
echo ""

API_HOST="$(host_of "${API_BASE}")"
EXPLORER_HOST="$(host_of "${EXPLORER_BASE}")"

echo "0) DNS"
if getent hosts "${API_HOST}" >/dev/null 2>&1; then
  echo "  OK     ${API_HOST} -> $(getent hosts "${API_HOST}" | awk '{print $1}' | head -1)"
else
  echo "  FALHA  ${API_HOST} nao resolve"
  echo "         Crie um registro A apontando para o IP da VPS."
fi
if getent hosts "${EXPLORER_HOST}" >/dev/null 2>&1; then
  echo "  OK     ${EXPLORER_HOST} -> $(getent hosts "${EXPLORER_HOST}" | awk '{print $1}' | head -1)"
else
  echo "  FALHA  ${EXPLORER_HOST} nao resolve"
  echo "         Crie um registro A apontando para o IP da VPS."
fi
echo ""

FAILS=0
echo "1) API"
check "health"  "${API_BASE}/api/health" '"rpc":"ok"' || FAILS=$((FAILS + 1))
check "status"  "${API_BASE}/api/status" '"online":true' || FAILS=$((FAILS + 1))
check "fee"     "${API_BASE}/api/fee" 'feePerKbSatoshis' || FAILS=$((FAILS + 1))
check "balance" "${API_BASE}/api/address/${TEST_ADDRESS}/balance" '"address"' || FAILS=$((FAILS + 1))
echo ""
echo "2) Explorer"
check "health"  "${EXPLORER_BASE}/ext/health" '"rpc":"ok"' || FAILS=$((FAILS + 1))
check "summary" "${EXPLORER_BASE}/ext/getsummary" 'blockcount' || FAILS=$((FAILS + 1))
check "address" "${EXPLORER_BASE}/ext/getaddress/${TEST_ADDRESS}" '"address"' || FAILS=$((FAILS + 1))
echo ""

if (( FAILS > 0 )); then
  echo "${FAILS} teste(s) falharam."
  echo "Na VPS, confirme primeiro o teste local:"
  echo "  curl -s http://127.0.0.1:50012/api/health"
  echo "  curl -s http://127.0.0.1:50011/ext/health"
  echo "Depois DNS + nginx HTTPS (scripts/nginx-x2x-api.conf.example)."
  exit 1
fi

echo "Todos os testes externos passaram."
