#!/usr/bin/env bash
# Print SHA-256 pins for api.aladhan.com — paste into network_security_config.xml pin-set.
# Usage: ./scripts/verify-aladhan-pins.sh
set -euo pipefail

HOST="api.aladhan.com"
CONFIG="app/src/main/res/xml/network_security_config.xml"

echo "=== TLS pins for ${HOST} ==="
echo

cert_pem="$(echo | openssl s_client -connect "${HOST}:443" -servername "${HOST}" 2>/dev/null \
  | awk '/BEGIN CERTIFICATE/,/END CERTIFICATE/{print}' | head -n 50)"

leaf_subject="$(printf '%s\n' "$cert_pem" | openssl x509 -noout -subject 2>/dev/null)"
leaf_expiry="$(printf '%s\n' "$cert_pem" | openssl x509 -noout -enddate 2>/dev/null)"

leaf_cert_pin="$(printf '%s\n' "$cert_pem" | openssl x509 -outform der \
  | openssl dgst -sha256 -binary | openssl base64)"
leaf_spki_pin="$(printf '%s\n' "$cert_pem" | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl base64)"

echo "Leaf: ${leaf_subject}"
echo "Expiry: ${leaf_expiry}"
echo
echo "Leaf certificate pin (primary):"
echo "  ${leaf_cert_pin}"
echo
echo "Leaf SPKI pin (backup — same key re-issue):"
echo "  ${leaf_spki_pin}"
echo

if [[ -f "$CONFIG" ]]; then
  if grep -q "${leaf_cert_pin}" "$CONFIG" && grep -q "${leaf_spki_pin}" "$CONFIG"; then
    echo "OK: ${CONFIG} matches live pins."
  else
    echo "ACTION: update pin-set in ${CONFIG} before leaf expiry."
    exit 1
  fi
fi
