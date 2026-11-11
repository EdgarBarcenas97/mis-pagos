# Specs (Spec Driven Development)

El código no se escribe hasta que la spec está aprobada por un humano.

## Estados de una feature (`feature_list.json`)

| Estado | Significado |
|---|---|
| `pending` | Registrada, sin spec |
| `spec_ready` | Spec escrita; esperando aprobación humana |
| `in_progress` | Spec aprobada; contrato, tests e implementación en curso |
| `done` | Verify verde, review aprobado y cierre aprobado por el humano |
| `blocked` | Atascada; la razón está en `progress/current.md` |

## Archivos de `specs/<feature>/`

### `requirements.md`: QUÉ (notación EARS)

Un requisito por encabezado `## R<n>`, con un solo `DEBE`, verificable con un test.

| Patrón | Plantilla |
|---|---|
| Ubicuo | `El sistema DEBE <acción>.` |
| Evento | `CUANDO <disparador>, el sistema DEBE <acción>.` |
| Estado | `MIENTRAS <estado>, el sistema DEBE <acción>.` |
| Opcional | `DONDE <condición opcional>, el sistema DEBE <acción>.` |
| No deseado | `SI <evento no deseado> ENTONCES el sistema DEBE <acción>.` |

```markdown
## R1
CUANDO la pantalla de lista se abre, el sistema DEBE mostrar el estado de carga.

## R2
SI la petición de red falla y no hay caché ENTONCES el sistema DEBE mostrar el
estado de error con el botón "Reintentar".
```

Termina con una sección `## Fuera de alcance`.

### `design.md`: CÓMO

- Archivos a crear o modificar.
- **Firmas públicas** (el contrato que crea el implementer y usa el tester).
- Estados de UI, copy exacto, testTags y accesibilidad (si hay UI).
- Al menos una alternativa descartada y por qué.

### `tasks.md`: PASOS

```markdown
- [ ] T1 — Repositorio con caché. Cubre: R3, R4.
- [ ] T2 — ViewModel y UiState. Cubre: R1, R2.
```

## Trazabilidad (regla dura)

- Cada test lleva `// Cubre: <feature>/R<n>`.
- Cada `R<n>` tiene al menos un test: `scripts/trace.sh <feature>` lo comprueba.
- Los tests no cambian después del commit en rojo (`red_sha`): el reviewer lo
  comprueba con `git diff`.
