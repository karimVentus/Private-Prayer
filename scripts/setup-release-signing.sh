#!/usr/bin/env bash
# One-time (or idempotent) release signing setup. Writes gitignored keystore.properties.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KEYSTORE="${PRAYERTIME_KEYSTORE:-$HOME/prayertime-upload.jks}"
PROPS="$ROOT/keystore.properties"
ALIAS="${PRAYERTIME_KEY_ALIAS:-prayertime}"

if [[ -f "$PROPS" ]] && [[ -f "$KEYSTORE" ]]; then
    echo "[setup-release-signing] keystore + keystore.properties already exist"
    exit 0
fi

if [[ ! -f "$KEYSTORE" ]]; then
    if [[ -z "${PRAYERTIME_KEYSTORE_PASSWORD:-}" ]]; then
        echo "[setup-release-signing] ERROR: set PRAYERTIME_KEYSTORE_PASSWORD to create a new keystore"
        echo "  Example: PRAYERTIME_KEYSTORE_PASSWORD='your-strong-password' $0"
        exit 1
    fi
    echo "[setup-release-signing] Creating upload keystore at $KEYSTORE"
    mkdir -p "$(dirname "$KEYSTORE")"
    keytool -genkeypair -v \
        -keystore "$KEYSTORE" \
        -alias "$ALIAS" \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass "$PRAYERTIME_KEYSTORE_PASSWORD" \
        -keypass "$PRAYERTIME_KEYSTORE_PASSWORD" \
        -dname "CN=Prayer Time, OU=Mobile, O=PrayerTime, L=Unknown, ST=Unknown, C=DE"
fi

if [[ ! -f "$PROPS" ]]; then
    if [[ -z "${PRAYERTIME_KEYSTORE_PASSWORD:-}" ]]; then
        echo "[setup-release-signing] ERROR: PRAYERTIME_KEYSTORE_PASSWORD required to write keystore.properties"
        exit 1
    fi
    cat >"$PROPS" <<EOF
storeFile=$KEYSTORE
storePassword=$PRAYERTIME_KEYSTORE_PASSWORD
keyAlias=$ALIAS
keyPassword=$PRAYERTIME_KEYSTORE_PASSWORD
EOF
    chmod 600 "$PROPS"
    echo "[setup-release-signing] Wrote $PROPS (gitignored)"
fi

echo "[setup-release-signing] Done — backup $KEYSTORE and your password; loss blocks Play Store updates."
