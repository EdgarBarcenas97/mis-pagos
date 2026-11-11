# ADR 0002: Cómo se trabaja con IA en este repo (harness)

- **Estado:** Aceptado
- **Fecha:** 2026-11-11

## Contexto

Se quiere usar IA para construir software sin perder control: que cada
resultado sea verificable, trazable a un requisito y repetible por otra
persona del equipo que clone el repo.

## Decisiones

| Tema | Elegido | Alternativas descartadas | Por qué |
|---|---|---|---|
| Proceso | **SDD**: `/kickoff` → `/github` → `/design` → `/feature` (spec EARS → aprobación humana → plan en modo plan → contrato → tests en rojo → implementación → review → demo → cierre) → `/ship` (una PR por feature) | Pedirle código directo al modelo | La spec es el contrato: si un criterio no es verificable, el agente no sabe cuándo terminó |
| Modelo | **Opus 5.5 con esfuerzo `medium`** fijado en `.claude/settings.json`; `/feature` lo repite en su frontmatter; los subagentes heredan la sesión | Esfuerzo `xhigh`; Sonnet para el `implementer` | Opus da el criterio que piden la spec y el plan; `medium` mantiene el flujo rápido y con costo contenido. Un solo nivel para todo el flujo es predecible |
| Orquestación | **Una sesión principal + subagentes** (`tester`, `implementer`, `reviewer`) | Agent teams; varias sesiones en paralelo | El flujo es una cadena secuencial. Los subagentes arrancan con contexto limpio (independencia del tester y del reviewer) y el humano no hace de mensajero |
| Arranque | **Plantilla probada + script** (`/harness`) | Que la IA genere el proyecto | Lo mecánico no se le pide a la IA: versiones inventadas rompen el build |
| Repo | **`/github`**: crea el repo y sube `main` una sola vez | Crear el repo a mano | Un solo comando deja `origin` y el upstream listos; después `main` solo recibe PRs |
| Integración | **`/ship`**: PR con spec, review y evidencia, CI en verde, merge commit con un sí humano | Push directo a main; squash | Nada llega a main sin PR. El merge commit conserva `test: red` antes de `feat`, la prueba de tests primero |
| Gates | **Hooks** (fase, comandos peligrosos, lint al editar, estado al terminar) + **scripts** (`verify.sh`, `trace.sh`) | Solo instrucciones en texto | Las reglas las aplica el sistema; lo verificable no se deja al juicio del modelo |
| Tests | El `tester` escribe los tests **antes** y sin ver la implementación; quedan congelados desde `red_sha` | El mismo agente escribe código y tests | La IA no se califica a sí misma |
| Skills vs agentes | **Skill** para procedimientos fijos (`/harness`, `/kickoff`, `/feature`, `/compile-app`); **agente** cuando hace falta contexto propio y criterio | Todo como agentes | Un procedimiento repetible no necesita criterio ni contexto aislado |
| MCP | **`mobile`** para manejar el emulador en la demo | MCP de GitHub | `gh` CLI ya cubre GitHub; el emulador no tiene alternativa por CLI tan completa |

## Límites conocidos

- Que la sesión principal no edite código es una instrucción, no un hook: los
  hooks no distinguen al orquestador de un subagente. El respaldo es el
  reviewer, que revisa el diff completo contra la spec.
- Una edición hecha por Bash (p. ej. `sed`) no pasa por `guard-edit.sh`. El
  respaldo es el `git diff` del reviewer desde `red_sha`.

## Consecuencias

- Cada feature deja rastro en disco: spec, reportes de agentes, review,
  capturas de la demo y métricas.
- Más pasos que "pídele el código a la IA", a cambio de resultados que se
  pueden auditar.
