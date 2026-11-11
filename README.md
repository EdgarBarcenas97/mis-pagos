# Mis pagos

Pantalla Android (Kotlin + Jetpack Compose) de una fintech de créditos: lista los pagos
del crédito con monto, fecha límite y estado, y permite pagar **sin que un pago se
duplique nunca**. El backend es simulado (latencia, fallos aleatorios y timeouts).

**📦 Descarga del APK:** https://drive.google.com/drive/folders/1z8W5d5ouUthGlfWyAR9soKXMzD9OYbhN?usp=sharing

**🎨 Mockup de pantallas (aprobado):** https://claude.ai/artifact/JdDrU7CGZ8uvQd6WTMFc5X — todos los estados de la pantalla; detalle en `docs/design.md`.

## Cómo correrlo

```bash
./init.sh              # entorno + estado del harness + verificación completa
scripts/verify.sh      # la definición de "terminado" (ktlint, lint, tests, build)
scripts/run-app.sh     # instala y abre la app en el emulador
```

## Decisiones de arquitectura

Resumen en [`DECISIONS.md`](DECISIONS.md); el stack completo y las alternativas descartadas están en `docs/stack.md` y `docs/adr/0001-stack.md`.

| Tema | Decisión |
|---|---|
| **Capas** | Clean + MVVM por paquetes, en `feature/payments/{domain,data,di,presentation}`. Las dependencias van `presentation → domain ← data`, y `domain` no importa Android. |
| **Modelo de estado** | `PaymentsUiState` es una `sealed interface` (Loading, Empty, Content, Error) que el ViewModel expone como `StateFlow`. `PaymentsScreen` no tiene estado: recibe el estado y las lambdas, y es lo que se testea. |
| **Manejo de errores** | `safeApiCall` convierte cualquier excepción en un `AppError` (Network, Server, NotFound, Serialization), así que la UI nunca ve excepciones. Si falla la carga se muestra el estado de error con "Reintentar". Un timeout al pagar **no es un error**: se muestra "Verificando tu pago…" y se concilia con el servidor. |
| **Fuente de verdad** | El servidor, a través de Retrofit (`PaymentsApi`) detrás de `PaymentsRepository`. En la app, un interceptor de OkHttp simula el backend (~1.5 s y 30% de fallos). En los tests, el repositorio se prueba contra MockWebServer. |
| **Dinero y fechas** | Los montos son `BigDecimal` leídos como texto del JSON, nunca `Double`. La fecha límite se toma como fecha de calendario del instante UTC (30 nov 2026, en cualquier zona horaria). |
| **Pagos sin duplicar** | Cada pago tiene una llave de idempotencia que se reutiliza en todos los reintentos. El botón se bloquea mientras procesa, y tras un timeout el pago queda en "Verificando" hasta conciliar. |
| **Fuera por tiempo** | Caché offline con "Actualizado hace X min" (stretch), pagos parciales, red real y tests de UI en CI. |

## Harness (cómo se trabajó con IA)

- **Archivo de reglas:** [`CLAUDE.md`](CLAUDE.md). Tiene el producto, el flujo de trabajo y las reglas duras, y remite a `docs/product.md`, `docs/design.md`, `docs/stack.md` y `docs/specs.md`.
- **Flujo SDD por feature (`/feature`):**
  1. Spec en EARS (`specs/<feature>/`), aprobada por un humano.
  2. Plan de ejecución, aprobado por un humano.
  3. Contrato (firmas).
  4. Tests en rojo, que escribe un subagente que no ve la implementación.
  5. Implementación, con los tests congelados.
  6. Review independiente.
  7. Demo en el emulador, comparada con el mockup.
- **Gates automáticos** (`.claude/hooks/`):
  - Cada fase limita qué se puede editar: el tester no toca producción y el implementer no toca tests.
  - Se bloquean los comandos peligrosos.
  - Se corre lint al editar.
- **Loop de verificación:** `scripts/verify.sh` es lo único que da algo por terminado. Corre ktlint, Android Lint, los tests unitarios y el build de la app y de los tests de UI.
- **Trazabilidad:** cada test lleva `// Cubre: <feature>/R<n>`, y `scripts/trace.sh <feature>` comprueba que cada requisito tenga al menos un test.
- **Mockup aprobado:** https://claude.ai/artifact/JdDrU7CGZ8uvQd6WTMFc5X

## Entregables

| Entregable | Dónde |
|---|---|
| Código de la app compilando | `app/`, verificado por `scripts/verify.sh` |
| Archivo de reglas / contexto del agente | [`CLAUDE.md`](CLAUDE.md), más `docs/` y `.claude/` (agentes, hooks y skills) |
| Tests que corran | `app/src/test/` (unitarios, JUnit + Turbine + MockWebServer) y `app/src/androidTest/` (Compose UI) |
| `DECISIONS.md` (≤ 5 líneas) | [`DECISIONS.md`](DECISIONS.md) |
| Mockup de pantallas | [Artifact](https://claude.ai/artifact/JdDrU7CGZ8uvQd6WTMFc5X) · `docs/design/mockup.html` |
| APK | [Google Drive](https://drive.google.com/drive/folders/1z8W5d5ouUthGlfWyAR9soKXMzD9OYbhN?usp=sharing) |

## Features

| Feature | Estado | Spec |
|---|---|---|
| `payments_list`: lista y estados de UI | en curso | `specs/payments_list/` |
| `pay_payment`: pagar sin duplicar | pendiente | — |
| `offline_last_known`: último dato bueno (stretch) | pendiente | — |
| `splash`: splash con ícono y "Mis pagos" | pendiente | — |
