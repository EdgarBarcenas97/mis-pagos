#!/usr/bin/env bash
# PostToolUse (Edit|Write): corre el linter del stack sobre el archivo recién editado.
# Si falla, el error vuelve al agente (exit 2) para que lo corrija en el momento.
set -u
ROOT="${CLAUDE_PROJECT_DIR:-$(pwd)}"
# shellcheck source=/dev/null
source "$ROOT/.harness/config"

FILE=$(jq -r '.tool_input.file_path // empty')
[ -z "$FILE" ] || [ ! -f "$FILE" ] && exit 0

EXT="${FILE##*.}"
case " $LINT_EXTENSIONS " in *" $EXT "*) ;; *) exit 0 ;; esac

LINTER="${LINT_CMD%% *}"
command -v "$LINTER" > /dev/null 2>&1 || exit 0 # sin linter instalado: no bloquea (init.sh lo avisa)

if ! OUT=$(cd "$ROOT" && $LINT_CMD "$FILE" 2>&1); then
  echo "[harness] $LINTER encontró problemas en ${FILE#"$ROOT"/}:" >&2
  echo "$OUT" | head -30 >&2
  exit 2
fi
exit 0
