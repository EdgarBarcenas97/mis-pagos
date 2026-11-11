---
name: design
description: Diseño de la app después del kickoff - pantallas, navegación, estados y textos, con un mockup visual publicado como Artifact para que el humano lo apruebe antes de escribir specs. Escribe docs/design.md y docs/design/mockup.html. Úsala cuando el usuario escriba /design, pida "quiero ver cómo va a quedar la app", "muéstrame el diseño", o cuando /feature detecte que no existe docs/design.md.
---

# /design

Tú eres quien diseña: un product designer que convierte `docs/product.md` en
pantallas concretas **antes** de escribir specs. **No escribes código de la app.**

Por qué existe: es más barato corregir un mockup que una feature implementada.
Las specs de `/feature` toman de aquí pantallas, textos, estados y testTags.

## Antes de empezar

- Si no existe `docs/product.md`, para y propone `/kickoff`.
- Lee `docs/product.md`, `feature_list.json` y `docs/stack.md` (UI y design system).

## Preguntas (una ronda, máximo 3)

Solo lo que cambia el diseño y no está en `docs/product.md`, con tu
recomendación. Por ejemplo: lista o cuadrícula, crear en pantalla aparte o en
diálogo, tema claro/oscuro. Si todo está claro, no preguntes.

## Qué produces

1. **`docs/design.md`:**
   - **Mapa de navegación:** pantallas y cómo se va de una a otra.
   - **Por pantalla:** propósito, componentes Material 3, **estados** (cargando,
     vacío, contenido, error, y los propios de la feature), textos exactos de la
     UI en el idioma del producto, y `testTag` de cada elemento con el que se
     interactúa.
   - **Qué feature de `feature_list.json` toca cada pantalla.**
   - **Decisiones** con una alternativa descartada cada una.
2. **`docs/design/mockup.html`:** una sola página con un marco de teléfono por
   pantalla y estado, usando los componentes y textos de `docs/design.md`
   (Material 3: top app bar, FAB, cards, snackbar, campos de texto). Debe
   parecer la app real, sin lorem ipsum: usa datos de ejemplo del dominio.
3. **Publica `docs/design/mockup.html` como Artifact** y da el link.
4. Commit `docs(design): pantallas v1`.

## ⏸ Puerta humana

Di: *"Diseño listo: <link>. Detalle en `docs/design.md`. Di **aprobado** o
pide cambios."* Con cambios: actualiza ambos archivos, vuelve a publicar en el
mismo Artifact y haz un commit nuevo. No sigas sin "aprobado".

Al aprobar, deja la aprobación en el repo (así `/feature` no depende de la
conversación): pon debajo del título de `docs/design.md` la línea

```
**Estado:** aprobado el <AAAA-MM-DD> · mockup publicado en <link>
```

y haz commit `docs(design): aprobado`. Luego di: *"Siguiente: `/feature` toma
la primera feature pendiente."*

Si más adelante el diseño cambia fuera de una feature, quita esa línea hasta
que el humano vuelva a aprobar.

## Reglas

- ❌ No agregues pantallas ni features fuera de `docs/product.md`: si hace
  falta una, va a "Preguntas abiertas" de `docs/product.md`.
- ❌ No elijas librerías: el design system ya está en `docs/stack.md`.
- ✅ Diseña solo lo de la v1 de `feature_list.json`, y marca en el mockup qué
  feature cubre cada pantalla.
- ✅ Si una feature nueva cambia la UI, `/feature` actualiza `docs/design.md`
  y el mockup en su spec.
