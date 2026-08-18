#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
KEYSTORE_PATH="$ROOT_DIR/release/x2x-wallet.jks"
PROPS_PATH="$ROOT_DIR/keystore.properties"

mkdir -p "$ROOT_DIR/release"

if [[ -f "$KEYSTORE_PATH" ]]; then
  echo "Keystore já existe em $KEYSTORE_PATH"
  exit 0
fi

STORE_PASS="${STORE_PASS:-changeit}"
KEY_PASS="${KEY_PASS:-changeit}"

keytool -genkeypair -v \
  -keystore "$KEYSTORE_PATH" \
  -alias x2xcoin \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$STORE_PASS" \
  -keypass "$KEY_PASS" \
  -dname "CN=2x2 Wallet, OU=2x2Coin, O=2x2Coin, L=BR, ST=BR, C=BR"

cat > "$PROPS_PATH" <<EOF
storeFile=release/x2x-wallet.jks
storePassword=$STORE_PASS
keyAlias=x2xcoin
keyPassword=$KEY_PASS
EOF

echo "Keystore criado: $KEYSTORE_PATH"
echo "Propriedades salvas em: $PROPS_PATH"
