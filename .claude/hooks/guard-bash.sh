#!/usr/bin/env bash
# PreToolUse (Bash): bloquea comandos peligrosos o que saltan los gates.
set -u
ROOT="${CLAUDE_PROJECT_DIR:-$(pwd)}"
# shellcheck source=/dev/null
source "$ROOT/.harness/config"

CMD=$(jq -r '.tool_input.command // empty')
[ -z "$CMD" ] && exit 0

block() {
  echo "[harness] Comando bloqueado: $1" >&2
  exit 2
}

if echo "$CMD" | grep -Eq -- '--no-verify'; then
  block "no se saltan los hooks de git (--no-verify)"
fi
if echo "$CMD" | grep -Eq 'git[[:space:]]+push.*(--force|[[:space:]]-f([[:space:]]|$))'; then
  block "push forzado"
fi
# Subir solo un tag de versión (vX.Y.Z) está permitido: así se publica un release.
if echo "$CMD" | grep -Eq '^git[[:space:]]+push[[:space:]]+origin[[:space:]]+v[0-9]+\.[0-9]+\.[0-9]+[[:space:]]*$'; then
  exit 0
fi
if echo "$CMD" | grep -Eq 'git[[:space:]]+push'; then
  BRANCH=$(git -C "$ROOT" branch --show-current 2>/dev/null || true)
  if echo "$CMD" | grep -Eq '(^|[[:space:]:])(main|master)([[:space:]]|$)' || [ "$BRANCH" = "main" ] || [ "$BRANCH" = "master" ]; then
    block "push a main/master: se integra por PR con revisión humana"
  fi
fi
if [ -n "${SECRET_REGEX:-}" ] && echo "$CMD" | grep -Eq "$SECRET_REGEX"; then
  block "el comando toca un archivo de secretos"
fi
exit 0
