---
name: ship
description: Integra una feature cerrada a main - corre la verificación, sube la rama, crea la PR con la spec, el review y la evidencia, espera el CI y hace merge con confirmación humana. Úsala cuando el usuario escriba /ship, pida "crea la PR", "haz merge", "súbelo a main", o al terminar la fase 7 de /feature.
---

# /ship [<feature>]

Lleva la rama `feat/<name>` a `main`. Por defecto toma la feature de la rama
actual. **No cambias código:** si algo falla, paras y reportas.

## Requisitos (si falta uno, para y di qué falta)

- Estás en una rama `feat/<name>`, no en `main`.
- La feature está `done` en `feature_list.json` (pasó review y demo) y
  `git status` está limpio.

## Pasos

1. **Verificar en local:** `./init.sh` (incluye `scripts/verify.sh`) y
   `scripts/trace.sh <name>`. Si falla, para.
2. **¿Hay remoto?** `git remote -v`.
   - **Sí** → sigue con GitHub (pasos 3–6).
   - **No** → di que no hay remoto y ofrece dos caminos:
     - correr **`/github`** (crea el repo y lo conecta) y volver a `/ship`;
     - o **merge local** (paso 7).
3. **Subir la rama:** `git push -u origin feat/<name>`.
4. **Crear la PR** con `gh pr create --base main --title "feat(<name>): <title>"`
   y un cuerpo que enlace, sin copiar el contenido:
   - Qué hace: la `description` de la feature.
   - Requisitos: `specs/<name>/requirements.md` y el resultado de `trace.sh`.
   - Review: `progress/review_<name>.md`.
   - Evidencia: las capturas de `progress/evidence/<name>/`.
   - Cómo probar: `scripts/verify.sh` y `/compile-app`.
5. **Esperar el CI:** `gh pr checks --watch`. Si falla, muestra el log con
   `gh run view --log-failed`, para y pregunta. **No hagas merge en rojo.**
6. **⏸ Merge con confirmación.** Di: *"CI en verde: <link de la PR>. ¿Hago
   merge a main?"* Solo con un sí explícito:
   `gh pr merge --merge --delete-branch`, luego `git switch main && git pull`.
7. **Merge local (sin remoto):** con un sí explícito:
   `git switch main && git merge --no-ff feat/<name>` y después
   `git branch -d feat/<name>`.

Al terminar, una línea: PR, merge commit y la siguiente feature `pending`.

## Por qué así

- **Merge commit, no squash:** el historial de `main` conserva el commit
  `test(<name>): red` antes del `feat(<name>)`. Es la prueba de que los tests
  se escribieron primero.
- **El CI corre el mismo `scripts/verify.sh`** que en local: un solo criterio
  de "terminado".
- Nada llega a `main` sin PR (`guard-bash` bloquea el push directo) ni sin un
  sí humano.

## Reglas

- ❌ No hagas push a `main`, ni push forzado, ni `--no-verify`.
- ❌ No hagas merge con el CI en rojo o pendiente.
- ❌ No crees repos remotos aquí: eso es `/github`.
- ✅ Si hay conflictos con `main`, para y pregunta. No los resuelvas solo.
