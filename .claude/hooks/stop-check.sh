#!/usr/bin/env bash
# Stop: antes de que Claude termine su turno, revisa que el estado del harness
# sea coherente (rápido, sin compilar). Si no lo es, Claude debe arreglarlo.
set -u
ROOT="${CLAUDE_PROJECT_DIR:-$(pwd)}"

# Evita un ciclo infinito si ya estamos continuando por este mismo hook.
[ "$(jq -r '.stop_hook_active // false')" = "true" ] && exit 0

if ! OUT=$("$ROOT/init.sh" --quick 2>&1); then
  echo "[harness] El estado del harness no es coherente:" >&2
  echo "$OUT" | grep -E '\[FAIL\]' >&2
  exit 2
fi
exit 0
