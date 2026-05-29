#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 5 ]]; then
  echo "Usage: $0 <ldap_uri> <bind_dn> <password> <base_dn> <filter>"
  exit 1
fi

LDAP_URI="$1"
BIND_DN="$2"
PASSWORD="$3"
BASE_DN="$4"
FILTER="$5"

ldapsearch -LLL -H "$LDAP_URI" -x -D "$BIND_DN" -w "$PASSWORD" -b "$BASE_DN" "$FILTER" \
  uid cn certSerial certFingerprint userCertificate
