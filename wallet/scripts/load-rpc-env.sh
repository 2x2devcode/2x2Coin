#!/usr/bin/env bash
# Load RPC connection settings from ~/.2x2coin/2x2coin.conf.
#
# Production Java processes should receive X2XCOIN_CONF (and host/port), not
# X2X_RPC_PASSWORD. 2x2coin-cli reads rpcuser/rpcpassword from the conf file,
# so the password never appears in `ps` or /proc/<pid>/environ.

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
  export X2XCOIN_CONF="$CONF"
  if port="$(read_conf_value rpcport)"; then
    export X2X_RPC_PORT="$port"
  else
    export X2X_RPC_PORT="${X2X_RPC_PORT:-15189}"
  fi
  # Intentionally not exporting X2X_RPC_USER / X2X_RPC_PASSWORD.
else
  export X2X_RPC_PORT="${X2X_RPC_PORT:-15189}"
fi
