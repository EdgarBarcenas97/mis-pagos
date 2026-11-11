#!/usr/bin/env bash
# init.sh — ¿está el proyecto listo para trabajar?
#   ./init.sh          entorno + estado del harness + verificación completa del stack
#   ./init.sh --quick  solo estado del harness (sin compilar); lo usa el hook Stop
set -u
cd "$(dirname "$0")"
QUICK=0
[ "${1:-}" = "--quick" ] && QUICK=1
EXIT=0
ok() { printf "[OK]    %s\n" "$1"; }
warn() { printf "[WARN]  %s\n" "$1"; }
fail() {
  printf "[FAIL]  %s\n" "$1"
  EXIT=1
}
need() { # need <comando> <cómo instalarlo>
  if command -v "$1" > /dev/null 2>&1; then ok "$1"; else fail "falta $1 → $2"; fi
}

# shellcheck source=/dev/null
source .harness/config

echo "── 1. Entorno"
need git "xcode-select --install"
need jq "brew install jq"
need python3 "xcode-select --install"
if [ $QUICK = 0 ] && declare -f harness_check_env > /dev/null; then harness_check_env; fi

echo "── 2. Archivos del harness"
for f in CLAUDE.md feature_list.json docs/specs.md docs/stack.md progress/current.md \
  .harness/config .harness/phase scripts/verify.sh scripts/trace.sh; do
  [ -e "$f" ] && ok "$f" || fail "falta $f"
done

echo "── 3. Estado de features"
python3 - << 'PY' || EXIT=1
import json, os, re, sys
d = json.load(open("feature_list.json"))
feats = d.get("features", [])
valid = {"pending", "spec_ready", "in_progress", "done", "blocked"}
phase = open(".harness/phase").read().strip()
errs = []
if phase not in {"", "spec", "contract", "tests", "impl", "review", "demo"}:
    errs.append(f".harness/phase inválida: '{phase}'")
prog = [f for f in feats if f.get("status") == "in_progress"]
if len(prog) > 1:
    errs.append(f"{len(prog)} features en in_progress (máximo 1)")
if phase in {"contract", "tests", "impl", "review", "demo"} and len(prog) != 1:
    errs.append(f"fase '{phase}' sin una feature in_progress")
for f in feats:
    st, name = f.get("status"), f.get("name")
    if st not in valid:
        errs.append(f"{name}: estado inválido '{st}'")
        continue
    if st in {"spec_ready", "in_progress", "done"}:
        for fn in ("requirements.md", "design.md", "tasks.md"):
            if not os.path.isfile(f"specs/{name}/{fn}"):
                errs.append(f"{name} ({st}) sin specs/{name}/{fn}")
    if st == "done" and os.path.isfile(f"specs/{name}/tasks.md"):
        if re.search(r"^\s*- \[ \]", open(f"specs/{name}/tasks.md").read(), re.M):
            errs.append(f"{name} está done con tasks sin marcar")
for e in errs:
    print(f"[FAIL]  {e}")
if errs:
    sys.exit(1)
print(f"[OK]    {len(feats)} features, fase '{phase or 'ninguna'}'")
PY

echo "── 4. Verificación del stack"
if [ $QUICK = 1 ]; then
  warn "omitida (--quick)"
elif scripts/verify.sh; then
  ok "scripts/verify.sh"
else
  fail "scripts/verify.sh"
fi

echo "──"
[ $EXIT = 0 ] && ok "Listo para trabajar." || fail "Resuelve lo anterior antes de avanzar."
exit $EXIT
