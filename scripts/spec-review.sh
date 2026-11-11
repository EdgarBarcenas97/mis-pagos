#!/usr/bin/env bash
# spec-review.sh <feature> — arma la página de revisión de la spec para el humano.
# Lee specs/<feature>/review.json y lo inserta en .harness/templates/spec-review.html.
# Escribe specs/<feature>/review.html, que /feature publica como Artifact.
set -euo pipefail
cd "$(dirname "$0")/.."
NAME="${1:?uso: scripts/spec-review.sh <feature>}"
JSON="specs/$NAME/review.json"
TPL=".harness/templates/spec-review.html"
OUT="specs/$NAME/review.html"
[ -f "$JSON" ] || { echo "[FAIL] no existe $JSON"; exit 1; }
[ -f "$TPL" ] || { echo "[FAIL] no existe $TPL"; exit 1; }

python3 - "$JSON" "$TPL" "$OUT" "$NAME" << 'PY'
import json, sys
src, tpl, out, name = sys.argv[1:5]
data = json.load(open(src))
for key in ("feature", "title", "round", "status", "branch", "commit", "specPath", "summary",
            "decisions", "requirements", "tests", "outOfScope"):
    if key not in data:
        sys.exit(f"[FAIL] falta '{key}' en {src}")
if data["feature"] != name:
    sys.exit(f"[FAIL] review.json es de '{data['feature']}', no de '{name}'")
for d in data["decisions"]:
    if not any(o.get("recommended") for o in d["options"]):
        sys.exit(f"[FAIL] la decisión '{d['id']}' no marca una opción recomendada")
blob = json.dumps(data, ensure_ascii=False).replace("</", "<\\/")
html = open(tpl).read().replace("__SPEC_JSON__", blob).replace("__FEATURE__", name)
open(out, "w").write(html)
print(f"[OK] {out}")
PY
