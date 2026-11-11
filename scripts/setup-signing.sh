#!/usr/bin/env bash
# Crea la llave de firma de release (una vez por app) y la guarda como secrets
# del repo en GitHub. Lo corre el HUMANO en su propia terminal: el agente nunca
# ve la llave ni las contraseñas.
set -euo pipefail

if [ -n "${CLAUDECODE:-}" ]; then
  echo "Este script maneja secretos: córrelo tú en tu propia terminal, no desde Claude." >&2
  exit 1
fi
for c in keytool gh openssl; do
  command -v "$c" > /dev/null || { echo "Falta $c" >&2; exit 1; }
done

REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)
DIR="$HOME/.android-keystores/${REPO//\//_}"
KS="$DIR/upload.jks"
PROPS="$DIR/upload.properties"
mkdir -p "$DIR" && chmod 700 "$DIR"

if [ -f "$KS" ]; then
  echo "Uso la llave que ya existe: $KS"
  PW=$(sed -n 's/^password=//p' "$PROPS")
else
  PW=$(openssl rand -base64 24 | tr -d '/+=')
  keytool -genkeypair -keystore "$KS" -storetype PKCS12 -alias upload \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass "$PW" -keypass "$PW" -dname "CN=$REPO" > /dev/null 2>&1
  printf 'password=%s\nalias=upload\n' "$PW" > "$PROPS"
  chmod 600 "$KS" "$PROPS"
  echo "Llave creada: $KS"
fi

openssl base64 -A -in "$KS" | gh secret set ANDROID_KEYSTORE_BASE64
printf %s "$PW" | gh secret set ANDROID_KEYSTORE_PASSWORD
printf %s "$PW" | gh secret set ANDROID_KEY_PASSWORD
printf %s upload | gh secret set ANDROID_KEY_ALIAS

echo "Secrets de firma guardados en $REPO."
echo "IMPORTANTE: respalda $DIR (por ejemplo en tu gestor de contraseñas)."
echo "Sin esa llave no podrás publicar actualizaciones firmadas de la misma app."
