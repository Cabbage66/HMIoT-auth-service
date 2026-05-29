#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 4 ]]; then
  echo "Usage: $0 <ldap_uri> <bind_dn> <password> <ldif_file>"
  exit 1
fi

LDAP_URI="$1"
BIND_DN="$2"
PASSWORD="$3"
LDIF_FILE="$4"

ldapadd -H "$LDAP_URI" -x -D "$BIND_DN" -w "$PASSWORD" -f "$LDIF_FILE"
