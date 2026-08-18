#!/usr/bin/env bash
# Carrega credenciais RPC de ~/.2x2coin/2x2coin.conf.

if [[ -n "${X2XCOIN_CONF:-}" ]]; then
  CONF="$X2XCOIN_CONF"
elif [[ -f "${HOME}/.2x2coin/2x2coin.conf" ]]; then
  CONF="${HOME}/.2x2coin/2x2coin.conf"
elif [[ -f "${HOME}/.2x2Coin/2x2Coin.conf" ]]; then
  CONF="${HOME}/.2x2Coin/2x2Coin.conf"
else
  CONF="${HOME}/.2x2coin/2x2coin.conf"
fi

read_conf_value() {
  local key=$1
  if [[ ! -f "$CONF" ]]; then
    return 1
  fi
  grep -E "^[[:space:]]*${key}=" "$CONF" | tail -1 | sed -E "s/^[[:space:]]*${key}=//" | tr -d '\r' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'
}

export X2X_RPC_HOST="${X2X_RPC_HOST:-127.0.0.1}"
export X2X_CLI="${X2X_CLI:-2x2coin-cli}"

if [[ -f "$CONF" ]]; then
  if port="$(read_conf_value rpcport)"; then
    export X2X_RPC_PORT="$port"
  else
    export X2X_RPC_PORT="${X2X_RPC_PORT:-15189}"
  fi
  if user="$(read_conf_value rpcuser)"; then
    export X2X_RPC_USER="$user"
  fi
  if pass="$(read_conf_value rpcpassword)"; then
    export X2X_RPC_PASSWORD="$pass"
  fi
else
  export X2X_RPC_PORT="${X2X_RPC_PORT:-15189}"
fi
