#!/usr/bin/env bash
# run-app.sh — compila, asegura un dispositivo, instala y abre la app.
#   scripts/run-app.sh                         usa el dispositivo conectado o enciende el primer emulador
#   scripts/run-app.sh --avd <nombre>          enciende ese emulador si no hay dispositivo
#   scripts/run-app.sh --no-emulator           falla si no hay dispositivo (no enciende nada)
#   scripts/run-app.sh --screenshot <png>      además guarda una captura de la pantalla
set -euo pipefail
cd "$(dirname "$0")/.."

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$(command -v adb || echo "$SDK/platform-tools/adb")"
EMULATOR="$SDK/emulator/emulator"
AVD=""
START_EMU=1
SHOT=""
while [ $# -gt 0 ]; do
  case "$1" in
    --avd) AVD="${2:?falta el nombre del emulador}"; shift 2 ;;
    --no-emulator) START_EMU=0; shift ;;
    --screenshot) SHOT="${2:?falta la ruta de la captura}"; shift 2 ;;
    *) echo "[FAIL] opción desconocida: $1"; exit 1 ;;
  esac
done

step() { printf "── %s\n" "$1"; }
has_device() { "$ADB" devices | awk 'NR>1 && $2=="device"' | grep -q .; }
booted() { [ "$("$ADB" shell getprop sys.boot_completed 2> /dev/null | tr -d '\r')" = "1" ]; }

PKG=$(sed -nE 's/.*applicationId *= *"([^"]+)".*/\1/p' app/build.gradle.kts | head -1)
[ -n "$PKG" ] || { echo "[FAIL] no encontré applicationId en app/build.gradle.kts"; exit 1; }

step "1. Dispositivo"
if has_device; then
  echo "[OK] ya hay un dispositivo conectado"
elif [ $START_EMU = 0 ]; then
  echo "[FAIL] no hay dispositivo conectado (y se pidió --no-emulator)"
  exit 1
else
  [ -x "$EMULATOR" ] || { echo "[FAIL] no existe $EMULATOR → instala el emulador desde el SDK Manager"; exit 1; }
  [ -n "$AVD" ] || AVD=$("$EMULATOR" -list-avds | head -1)
  [ -n "$AVD" ] || { echo "[FAIL] no hay emuladores creados → Android Studio > Device Manager"; exit 1; }
  echo "encendiendo $AVD (log: /tmp/emulator-$AVD.log)"
  nohup "$EMULATOR" -avd "$AVD" > "/tmp/emulator-$AVD.log" 2>&1 &
  for _ in $(seq 1 90); do
    booted && break
    sleep 2
  done
  booted || { echo "[FAIL] el emulador no terminó de arrancar en 3 min (revisa /tmp/emulator-$AVD.log)"; exit 1; }
  echo "[OK] $AVD encendido"
fi

step "2. Compilar e instalar"
./gradlew --console=plain --quiet installDebug
echo "[OK] instalada ($PKG)"

step "3. Abrir"
"$ADB" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1
echo "[OK] abierta en el dispositivo"

if [ -n "$SHOT" ]; then
  step "4. Captura"
  mkdir -p "$(dirname "$SHOT")"
  sleep 2
  "$ADB" exec-out screencap -p > "$SHOT"
  echo "[OK] $SHOT"
fi
