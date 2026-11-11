#!/usr/bin/env bash
# trace.sh <feature> — cada requisito R<n> de la spec debe tener al menos un
# test marcado con el comentario "Cubre: <feature>/R<n>". Sin opiniones: o está o no.
set -u
cd "$(dirname "$0")/.."
NAME="${1:?uso: scripts/trace.sh <feature>}"
REQ="specs/$NAME/requirements.md"
[ -f "$REQ" ] || {
  echo "[FAIL]  no existe $REQ"
  exit 1
}

IDS=$(grep -oE '^#+[[:space:]]*R[0-9]+' "$REQ" | grep -oE 'R[0-9]+' | sort -u)
[ -z "$IDS" ] && {
  echo "[FAIL]  $REQ no tiene requisitos (## R1, ## R2…)"
  exit 1
}

EXIT=0
for ID in $IDS; do
  HITS=$(grep -rlE "Cubre:.*${NAME}/${ID}([^0-9]|$)" . \
    --exclude-dir=.git --exclude-dir=build --exclude-dir=.gradle \
    --exclude-dir=specs --exclude-dir=progress --exclude-dir=.harness \
    --exclude-dir=node_modules 2> /dev/null | sed 's|^\./||' | tr '\n' ' ')
  if [ -n "$HITS" ]; then
    echo "[OK]    $ID → $HITS"
  else
    echo "[FAIL]  $ID sin test"
    EXIT=1
  fi
done
exit $EXIT
