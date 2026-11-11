---
name: github
description: Crea el repo del proyecto en GitHub, lo conecta como origin, sube main y confirma que el CI corrió. Se usa una vez por proyecto. Úsala cuando el usuario escriba /github, pida "crea el repo", "súbelo a GitHub", "conecta con GitHub", o cuando /ship o /publish-* detecten que no hay remoto.
---

# /github

Conecta el proyecto local con un repo nuevo en GitHub. Después de esto, todo
cambio entra por rama y PR (`/ship`) y las versiones salen por tag
(`/publish-<stack>`).

## Requisitos (si falta uno, para y di cómo resolverlo)

- `gh auth status` con sesión iniciada. Si no la hay, el humano corre
  `! gh auth login`.
- Repo git con al menos un commit, en `main` y con `git status` limpio.
- **Sin remoto** (`git remote -v` vacío). Si ya hay uno, muéstralo y para.

## Pasos

1. **⏸ Pregunta en una sola ronda:**
   - nombre del repo (propón el nombre de la carpeta);
   - visibilidad: **privado** (recomendado) o público;
   - cuenta u organización, solo si `gh` tiene más de una.

   Repite el comando exacto que vas a correr y espera un sí.
2. **Crear, conectar y subir `main`** en un solo comando:
   `gh repo create <nombre> --private|--public --source . --remote origin --push`.
3. **Confirmar que quedó conectado:**
   - `gh repo view --json url -q .url` devuelve el link;
   - `git status -sb` muestra `main...origin/main`.
4. **Primer CI:** `gh run list --limit 3`. Si hay una ejecución en curso,
   ofrece seguirla con `gh run watch`. Si falla, muestra
   `gh run view --log-failed` y para.

Al terminar, una línea con el link del repo, el estado del CI y el siguiente
paso: *"Siguiente: `/design` para las pantallas. Desde aquí, cada feature
termina en una PR (`/feature` → `/ship`) y las versiones salen con
`/publish-<stack>`."*

## Por qué así

- Un solo `gh repo create --push` evita pasos a mano y deja `origin` y el
  upstream configurados.
- `main` se sube **solo esta vez**. Después, `guard-bash` bloquea el push
  directo a `main`: lo que llega a `main` llega por PR.

## Reglas

- ❌ No crees el repo sin el sí explícito del humano a nombre y visibilidad.
- ❌ No cambies la visibilidad ni borres repos existentes.
- ❌ No subas secretos: `.gitignore` ya excluye `.env`, `*.jks` y
  `keystore.properties`. Si `git status` muestra uno, para.
