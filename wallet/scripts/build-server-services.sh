#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

BIND_HOST="${BIND_HOST:-127.0.0.1}"
API_PORT="${API_PORT:-40012}"
EXPLORER_PORT="${EXPLORER_PORT:-40061}"

echo "Compilando API e explorer (bind local ${BIND_HOST})..."
./gradlew :x2x-server:installDist

echo ""
echo "Build concluido."
echo "  API publica:      https://server.2x2coin.com"
echo "  API local (VPS):  ${BIND_HOST}:${API_PORT}  (/api/*)"
echo "  Explorer publico: https://serverexplorer.2x2coin.com"
echo "  Explorer local:   ${BIND_HOST}:${EXPLORER_PORT}  (/ext/*)"
echo "  Sem interface web — somente JSON para o aplicativo."
echo ""
echo "Configure o reverse proxy (nginx/caddy) :443 -> ${BIND_HOST}:port"
echo "Para iniciar: bash scripts/run-server-services.sh"
