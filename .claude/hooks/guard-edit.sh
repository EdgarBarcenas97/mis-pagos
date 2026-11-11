#!/usr/bin/env bash
# PreToolUse (Edit|Write): bloquea ediciones que no tocan en la fase actual.
# La fase vive en .harness/phase; las rutas del stack en .harness/config.
set -u
set -f # los patrones son globs para `case`, no para expandir contra el disco
ROOT="${CLAUDE_PROJECT_DIR:-$(pwd)}"
# shellcheck source=/dev/null
source "$ROOT/.harness/config"

FILE=$(jq -r '.tool_input.file_path // empty')
[ -z "$FILE" ] && exit 0
REL="${FILE#"$ROOT"/}"
PHASE=$(tr -d '[:space:]' < "$ROOT/.harness/phase" 2>/dev/null || true)

matches() { # matches "<ruta>" "<patrones>"
  local p
  for p in $2; do
    # shellcheck disable=SC2254
    case "$1" in $p) return 0 ;; esac
  done
  return 1
}
block() {
  echo "[harness] Bloqueado en fase '$PHASE': $1 → $REL" >&2
  exit 2
}

case "$PHASE" in
  spec | review | demo)
    matches "$REL" "$CODE_PATTERNS" && block "en esta fase no se toca código"
    ;;
  contract | impl)
    matches "$REL" "$TEST_PATTERNS" && block "los tests están congelados; si uno está mal, repórtalo"
    ;;
  tests)
    matches "$REL" "$PROD_PATTERNS" && block "el tester no toca código de producción"
    ;;
esac
exit 0
