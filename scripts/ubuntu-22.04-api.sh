#!/usr/bin/env bash
# Wrapper na raiz do repositorio 2x2Coin.
exec bash "$(cd "$(dirname "$0")/../wallet/scripts" && pwd)/ubuntu-22.04-api.sh" "$@"
