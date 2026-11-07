#!/usr/bin/env bash
set -euo pipefail

mkdir -p keystore
KEYSTORE_FILE="keystore/cleanyourphone-release.jks"
ALIAS="${KEY_ALIAS:-cleanyourphone}"
PASS="${KEY_STORE_PASSWORD:-cleanyourphone2026}"

if [ ! -f "$KEYSTORE_FILE" ]; then
  echo "Generating Android release keystore at $KEYSTORE_FILE..."
  keytool -genkeypair -v \
    -keystore "$KEYSTORE_FILE" \
    -storetype PKCS12 \
    -alias "$ALIAS" \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass "$PASS" \
    -keypass "$PASS" \
    -dname "CN=dr-liki, OU=CleanYourPhone, O=dr-liki, L=Bengaluru, S=KA, C=IN"
fi

cat > keystore.properties <<EOF
storeFile=keystore/cleanyourphone-release.jks
storePassword=$PASS
keyAlias=$ALIAS
keyPassword=$PASS
EOF

echo ""
echo "=== Certificate Fingerprint (for README / verification) ==="
keytool -list -v -keystore "$KEYSTORE_FILE" -storepass "$PASS" | grep -E "SHA256:"

if command -v gh >/dev/null 2>&1; then
  echo ""
  echo "Uploading signing secrets to GitHub repository dr-liki/cleanyourphone..."
  base64 -w 0 "$KEYSTORE_FILE" | gh secret set SIGNING_KEY_BASE64 --repo dr-liki/cleanyourphone
  gh secret set SIGNING_STORE_PASSWORD --body "$PASS" --repo dr-liki/cleanyourphone
  gh secret set SIGNING_KEY_ALIAS --body "$ALIAS" --repo dr-liki/cleanyourphone
  gh secret set SIGNING_KEY_PASSWORD --body "$PASS" --repo dr-liki/cleanyourphone
  echo "GitHub Actions signing secrets configured!"
fi
