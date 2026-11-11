---
name: publish-android
description: Saca una versión de la app Android - comprueba que main está en verde, propone el número de versión, crea el tag vX.Y.Z y sigue el workflow que firma el APK y lo publica en GitHub Releases. Úsala cuando el usuario escriba /publish-android, pida "saca una versión", "publica la app", "haz un release", o después de mergear las features de una versión.
---

# /publish-android

Publica una versión de lo que hay en `main`. Tú solo creas y subes el tag; el
workflow `.github/workflows/android-release.yml` hace el resto:
`verify.sh` → APK de release firmado → GitHub Release con notas.

## Requisitos (si falta uno, para y di cómo resolverlo)

1. **Hay remoto.** Si no, propone `/github`.
2. **`main` al día y limpio:** `git switch main && git pull --ff-only` y
   `git status` vacío.
3. **CI de `main` en verde para este commit:**
   `gh run list --workflow android-ci.yml --branch main --limit 1 --json conclusion,headSha`.
   El `headSha` debe ser el de `git rev-parse HEAD` y `conclusion` debe ser
   `success`. Si está en curso, ofrece `gh run watch`.
4. **Secrets de firma:** `gh secret list` debe mostrar
   `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` y
   `ANDROID_KEY_PASSWORD`. Si faltan, **para** y di:
   *"Falta la llave de firma (una sola vez). Corre en tu propia terminal:
   `scripts/setup-signing.sh`. Avísame cuando termine."*
   **Nunca** generes ni leas la llave tú.

## Pasos

1. **Proponer la versión.** Toma el último tag
   (`git describe --tags --abbrev=0`; si no hay ninguno, la primera es
   `v0.1.0`). Lee los commits desde ese tag (`git log <tag>..HEAD --oneline`):
   - si hay algún `feat(` → sube el **minor**;
   - si solo hay `fix(` o `chore(` → sube el **patch**;
   - el **major** solo lo sube el humano.
2. **⏸ Confirmar.** Muestra la versión propuesta y las features que entran,
   que son los `feat(<name>)` desde el último tag. Espera un sí, o el número
   que el humano prefiera.
3. **Tag y push:**
   `git tag -a vX.Y.Z -m "Release vX.Y.Z"` y `git push origin vX.Y.Z`.
4. **Seguir el CD:** toma el id de la ejecución con
   `gh run list --workflow android-release.yml --limit 1 --json databaseId,headBranch`
   (su `headBranch` es el tag) y corre `gh run watch <id>`. Si falla, muestra
   `gh run view <id> --log-failed` y para. **No borres el tag sin preguntar.**
5. **Resultado:** `gh release view vX.Y.Z --json url,assets` → link de la
   Release y nombre del APK.

Al terminar, una línea con la versión, el link de la Release y el APK. Ofrece,
sin hacerlo, probar el APK de release en el emulador. Para eso hay que
desinstalar primero la de debug, porque tienen firmas distintas:
`gh release download vX.Y.Z -p '*.apk' -D build/release`, y luego
`adb uninstall <applicationId>` y `adb install build/release/*.apk`.

## Por qué así

- **El release es una decisión humana, no un efecto de cada merge.** Cada
  merge a `main` ya corre el CI completo y deja un APK de debug descargable.
  Una versión se publica cuando el humano decide qué features entran.
- **Versión desde el tag, no editando archivos:** `versionName` sale del tag y
  `versionCode` del número de ejecución del workflow. No hay commits de
  "bump version" ni conflictos.
- **La llave de firma solo la toca el humano:** `setup-signing.sh` se niega a
  correr dentro de Claude y deja la llave fuera del repo
  (`~/.android-keystores/`).

## Reglas

- ❌ No publiques con el CI de `main` en rojo o sin terminar.
- ❌ No muevas ni reescribas tags ya publicados: si algo salió mal, se publica
  una versión nueva.
- ❌ No toques la llave, los secrets ni `keystore.properties`.
