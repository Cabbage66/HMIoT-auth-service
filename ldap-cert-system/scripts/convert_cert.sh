#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 3 ]]; then
  echo "Usage: $0 <pem2der|der2pem|pem2b64|b64tofile> <input> <output>"
  exit 1
fi

MODE="$1"
INPUT="$2"
OUTPUT="$3"

case "$MODE" in
  pem2der)
    openssl x509 -in "$INPUT" -outform DER -out "$OUTPUT"
    ;;
  der2pem)
    openssl x509 -inform DER -in "$INPUT" -out "$OUTPUT"
    ;;
  pem2b64)
    base64 -w 0 "$INPUT" > "$OUTPUT"
    ;;
  b64tofile)
    base64 -d "$INPUT" > "$OUTPUT"
    ;;
  *)
    echo "Unsupported mode: $MODE"
    exit 1
    ;;
esac

echo "Done: $MODE $INPUT -> $OUTPUT"
