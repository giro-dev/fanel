#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <lan-ip> [extra-dns-or-ip ...]" >&2
  exit 2
fi

lan_ip="$1"
shift

if [[ -z "${FANEL_TLS_KEYSTORE_PASSWORD:-}" ]]; then
  read -r -s -p "PKCS12 password: " FANEL_TLS_KEYSTORE_PASSWORD
  echo
  read -r -s -p "Confirm password: " confirm_password
  echo
  if [[ "$FANEL_TLS_KEYSTORE_PASSWORD" != "$confirm_password" ]]; then
    echo "Passwords do not match" >&2
    exit 1
  fi
  export FANEL_TLS_KEYSTORE_PASSWORD
fi

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT
san="IP:10.8.0.1,IP:$lan_ip"
for name in "$@"; do
  if [[ "$name" =~ ^[0-9]{1,3}(\.[0-9]{1,3}){3}$ || "$name" == *:* ]]; then
    san="$san,IP:$name"
  else
    san="$san,DNS:$name"
  fi
done

openssl req -x509 -newkey rsa:3072 -sha256 -nodes -days 3650 \
  -keyout "$tmp_dir/fanel.key" \
  -out "$tmp_dir/fanel.crt" \
  -subj "/CN=Fanel" \
  -addext "subjectAltName=$san"
openssl pkcs12 -export \
  -name fanel \
  -inkey "$tmp_dir/fanel.key" \
  -in "$tmp_dir/fanel.crt" \
  -out fanel-tls.p12 \
  -passout env:FANEL_TLS_KEYSTORE_PASSWORD

printf 'Created %s\n' "$PWD/fanel-tls.p12"
printf 'FANEL_WG_TLS_SPKI_SHA256='
openssl x509 -in "$tmp_dir/fanel.crt" -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary \
  | base64
