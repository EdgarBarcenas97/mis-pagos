# Diseño v1: "Mis pagos"

**Estado:** aprobado el 2026-11-11 · mockup publicado en https://claude.ai/artifact/JdDrU7CGZ8uvQd6WTMFc5X

Mockup: `docs/design/mockup.html`. Supuestos de producto pendientes de confirmar
con el PO: ver "Preguntas abiertas" en `docs/product.md`.

## Mapa de navegación

Una sola pantalla, **Mis pagos** (destino de inicio). Pagar se resuelve en un
diálogo encima de la misma pantalla, sin navegar.

```
MisPagos ──(Pagar)──▶ Diálogo "Confirmar pago" ──(Pagar $X)──▶ MisPagos (fila en curso)
```

## Pantalla: Mis pagos

**Propósito:** ver cuánto se debe, para cuándo, qué ya se pagó, y pagar.

**Componentes M3:** `TopAppBar` (título y subtítulo con el crédito), `LazyColumn`
de `Card` (una por pago), `AssistChip` de estado (sin acción, solo etiqueta),
`Button` "Pagar", `AlertDialog` de confirmación y `SnackbarHost`. Los estados de
carga, vacío y error reutilizan `core/designsystem/StateViews.kt`.

### Fila de pago

| Elemento | Formato | Ejemplo |
|---|---|---|
| Monto | `es-MX`, 2 decimales, desde `BigDecimal` | `$1,250.10 MXN` |
| Fecha límite | Fecha de calendario (no se convierte a la zona local) | `Vence el 30 nov 2026` · si está pagado: `Venció el 30 sep 2026` |
| Estado | Chip con color semántico | `Pendiente` · `Vencido` · `Pagado` · `En proceso` |
| Acción | Botón "Pagar" solo si el estado es Pendiente o Vencido | `Pagar` |

Un estado desconocido muestra el chip "En revisión", sin botón.
Orden: por fecha límite ascendente.

### Estados de pantalla (feature `payments_list`)

| Estado | Qué se ve | Texto exacto | testTag |
|---|---|---|---|
| Cargando | Spinner centrado | (a11y) `Cargando` | `state_loading` |
| Error | Mensaje y botón | `No pudimos cargar tus pagos. Revisa tu conexión e inténtalo de nuevo.` · `Reintentar` | `state_error`, `state_retry` |
| Vacío | Mensaje | `Aún no tienes pagos programados para este crédito.` | `state_empty` |
| Contenido | Lista | Título `Mis pagos` · subtítulo `Crédito CR-88213` | `payments_list` |

### Estados de una fila al pagar (feature `pay_payment`)

| Estado | Qué se ve | Texto exacto | testTag |
|---|---|---|---|
| Listo | Botón activo | `Pagar` | `pay_button_<id>` |
| Confirmar | `AlertDialog` | Título `Confirmar pago` · cuerpo `Vas a pagar $1,250.10 MXN de la cuota que vence el 30 nov 2026.` · `Pagar $1,250.10` · `Cancelar` | `pay_confirm`, `pay_cancel` |
| Procesando | Botón deshabilitado con spinner; ningún otro tap cobra | `Procesando…` | `pay_button_<id>` (disabled) |
| Verificando (tras timeout) | Texto en lugar del botón, sin acción | `Verificando tu pago…` · apoyo: `No vuelvas a pagar; te avisaremos en cuanto se confirme.` | `payment_verifying_<id>` |
| Pagado | El chip pasa a `Pagado` y sale un snackbar | `Pago registrado.` | `payment_status_<id>` |
| Rechazado (error definitivo) | Snackbar, y el botón vuelve a Listo | `No se pudo procesar el pago y no se hizo ningún cargo.` | — |

Mientras un pago está en Procesando o Verificando, la pantalla no permite iniciar otro
pago del **mismo** id. Recargar la lista no borra ese estado.

### Estado sin conexión (feature `offline_last_known`, stretch)

| Estado | Qué se ve | Texto exacto | testTag |
|---|---|---|---|
| Último dato bueno | Banner sobre la lista; los botones Pagar se desactivan | `Sin conexión · Actualizado hace 12 min` | `last_updated_banner` |

### testTags de fila

`payment_item_<id>`, `payment_amount_<id>`, `payment_due_<id>`,
`payment_status_<id>`, `pay_button_<id>`, `payment_verifying_<id>`.
Se declaran en `object PaymentsTags`.

## Qué feature toca cada estado

| Feature | Estados |
|---|---|
| `payments_list` | Cargando, Error, Vacío, Contenido, fila (monto, fecha, chip) |
| `pay_payment` | Botón Pagar, Confirmar, Procesando, Verificando, Pagado, Rechazado |
| `offline_last_known` | Banner "Actualizado hace X min" |

## Decisiones

1. **Se confirma antes de pagar** con un `AlertDialog` que repite el monto y la fecha.
   *Descartado:* pagar con un solo tap, que es más rápido pero facilita un cobro por
   error, y en una app de dinero eso le cuesta más confianza al cliente que un tap extra.
2. **Un timeout se muestra como "Verificando tu pago…", nunca como error.**
   *Descartado:* "Error, reintenta", porque invita a un segundo cobro cuando el
   servidor quizá ya cobró.
3. **La fecha límite se muestra como fecha de calendario** (30 nov), sin convertirla
   a la zona horaria local. *Descartado:* convertir a CDMX, que mostraría 29 nov y
   adelantaría el vencimiento un día.
4. **Lista de cards con chip de estado.** *Descartado:* tabla densa, que se lee mal
   en un teléfono y no deja sitio para el botón.
5. **Sin conexión, Pagar se desactiva.** *Descartado:* encolar el pago offline,
   porque hacerlo seguro está fuera del alcance de 45 min.
