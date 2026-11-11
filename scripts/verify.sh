#!/usr/bin/env bash
# verify.sh — la definición de "terminado" del proyecto.
#   scripts/verify.sh        ktlint + Android Lint + tests unitarios + build (app y tests de UI)
#   scripts/verify.sh --ui   además, tests de UI en el emulador/dispositivo
set -euo pipefail
cd "$(dirname "$0")/.."

./gradlew --console=plain --quiet ktlintCheck lintDebug testDebugUnitTest assembleDebug assembleDebugAndroidTest

if [ "${1:-}" = "--ui" ]; then
  if ! adb get-state > /dev/null 2>&1; then
    echo "[FAIL] no hay emulador ni dispositivo conectado (adb devices)"
    exit 1
  fi
  ./gradlew --console=plain --quiet connectedDebugAndroidTest
fi
echo "[OK] verify"
