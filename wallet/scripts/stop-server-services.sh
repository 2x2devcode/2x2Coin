#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

BIND_HOST="${BIND_HOST:-127.0.0.1}"
API_PORT="${API_PORT:-50012}"
EXPLORER_PORT="${EXPLORER_PORT:-50011}"
PID_DIR="${PID_DIR:-${ROOT_DIR}/.run}"

stop_port() {
  local port=$1
  local label=$2
  if command -v fuser >/dev/null 2>&1; then
    if fuser -n tcp "${port}" >/dev/null 2>&1; then
      echo "Encerrando ${label} na porta ${port}..."
      fuser -k -n tcp "${port}" >/dev/null 2>&1 || true
      sleep 1
    fi
    return
  fi
  if command -v lsof >/dev/null 2>&1; then
    local pids
    pids="$(lsof -ti tcp:"${port}" -sTCP:LISTEN 2>/dev/null || true)"
    if [[ -n "${pids}" ]]; then
      echo "Encerrando ${label} na porta ${port} (PID ${pids})..."
      # shellcheck disable=SC2086
      kill ${pids} 2>/dev/null || true
      sleep 1
    fi
  fi
}

stop_pid_file() {
  local pid_file=$1
  local label=$2
  if [[ -f "${pid_file}" ]]; then
    local pid
    pid="$(cat "${pid_file}")"
    if [[ -n "${pid}" ]] && kill -0 "${pid}" 2>/dev/null; then
      echo "Encerrando ${label} (PID ${pid})..."
      kill "${pid}" 2>/dev/null || true
      sleep 1
      if kill -0 "${pid}" 2>/dev/null; then
        echo "  ${label} ainda ativo, enviando SIGKILL..."
        kill -9 "${pid}" 2>/dev/null || true
        sleep 1
      fi
    fi
    rm -f "${pid_file}"
  fi
}

port_in_use() {
  local port=$1
  if command -v fuser >/dev/null 2>&1; then
    fuser -n tcp "${port}" >/dev/null 2>&1
    return $?
  fi
  if command -v lsof >/dev/null 2>&1; then
    [[ -n "$(lsof -ti tcp:"${port}" -sTCP:LISTEN 2>/dev/null || true)" ]]
    return $?
  fi
  if command -v ss >/dev/null 2>&1; then
    ss -lnt "sport = :${port}" 2>/dev/null | grep -q ":${port}"
    return $?
  fi
  return 1
}

echo "=== Encerrando servicos 2x2Coin ==="
echo ""

stop_pid_file "${PID_DIR}/x2x-api.pid" "API"
stop_pid_file "${PID_DIR}/x2x-explorer.pid" "Explorer"
stop_port "${API_PORT}" "API"
stop_port "${EXPLORER_PORT}" "Explorer"

echo ""
if port_in_use "${API_PORT}"; then
  echo "  API:      ainda escutando em ${BIND_HOST}:${API_PORT}"
else
  echo "  API:      parado (porta ${API_PORT} livre)"
fi
if port_in_use "${EXPLORER_PORT}"; then
  echo "  Explorer: ainda escutando em ${BIND_HOST}:${EXPLORER_PORT}"
else
  echo "  Explorer: parado (porta ${EXPLORER_PORT} livre)"
fi
