#!/usr/bin/env bash
# Stand-in for 2x2coin-cli used by tests/smoke when the real binary is absent.
# Same flags as 2x2coin-cli; forwards JSON-RPC to the daemon or MockRpcServer.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LIB_DIR="${X2X_SERVER_LIB:-${ROOT_DIR}/x2x-server/build/install/x2x-server/lib}"
if [[ ! -d "$LIB_DIR" ]]; then
  echo "error: x2x-server lib not found at $LIB_DIR — run scripts/build-server-services.sh" >&2
  exit 1
fi
exec java -cp "${LIB_DIR}/*" com.x2xcoin.wallet.server.MockCoinCli "$@"
