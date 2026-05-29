#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
CA_DIR="$ROOT_DIR/certs/ca"
OPENSSL_CNF="$ROOT_DIR/openssl/openssl.cnf"

mkdir -p "$CA_DIR"

if [[ ! -f "$CA_DIR/ca.key.pem" ]]; then
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:4096 -out "$CA_DIR/ca.key.pem"
  chmod 600 "$CA_DIR/ca.key.pem"
fi

if [[ ! -f "$CA_DIR/ca.cert.pem" ]]; then
  openssl req -x509 -new -nodes \
    -key "$CA_DIR/ca.key.pem" \
    -sha256 -days 3650 \
    -out "$CA_DIR/ca.cert.pem" \
    -config "$OPENSSL_CNF"
fi

echo "CA initialized:"
echo "  key : $CA_DIR/ca.key.pem"
echo "  cert: $CA_DIR/ca.cert.pem"
