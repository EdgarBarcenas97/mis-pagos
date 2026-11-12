# impl: payments_list

Estado: ok. `scripts/verify.sh` -> `[OK] verify` (ktlint + lint + unitarios + assembleDebug + assembleDebugAndroidTest).
Unitarios: 48/48 (43 de payments_list + 5 de SafeApiCallTest). Tests sin tocar desde `a1d9e5d`.
`PaymentsScreenTest` (androidTest) compila; no se corrió en emulador (`--ui` no ejecutado).

## Qué se hizo (T1-T4)

- T1 `domain/Payment.kt`: `isPayable` (solo Pending/Overdue), `fromApi` con match exacto; cualquier otro texto (incluida cadena vacía) -> `Unknown(raw)`.
  `data/BigDecimalSerializer.kt`: con `JsonDecoder` lee `decodeJsonElement().jsonPrimitive.content` (texto crudo, conserva escala y precisión); `serialize` emite `JsonUnquotedLiteral` (opt-in `ExperimentalSerializationApi`, kotlinx.serialization 1.9.0).
  `data/PaymentsMapper.kt`: `Instant.parse(..).atZone(ZoneOffset.UTC).toLocalDate()`.
- T2 `data/FakeBackendInterceptor.kt`: `sleeper(latencyMs)` siempre primero; `random.nextDouble() < failureRate` -> 503 con cuerpo vacío, si no 200 con `body` (`application/json`). Sin `chain.proceed`: nunca sale a la red.
  `data/PaymentsRepositoryImpl.kt`: `safeApiCall { api.getPayments() }`; en éxito mapea y ordena por `dueDate`; `DateTimeParseException` del mapeo -> `AppError.Serialization`.
- T3 `presentation/PaymentsViewModel.kt`: `load()` en `init` y en `retry()`; pone `Loading` de forma síncrona, luego `withContext(dispatchers.io) { repository.getPayments() }` -> Error / Empty (lista vacía) / Content.
- T4 `presentation/PaymentsFormat.kt` y `PaymentsScreen.kt` (Scaffold + TopAppBar con título y subtítulo `Crédito <loanId>` solo en Content; Loading/Empty/Error con `StateViews`; `LazyColumn` de `Card` con tags de monto, fecha y chip; preview).
  Strings, DI (`PaymentsModule`) y navegación (`MainActivity`) ya estaban del contrato: no se tocaron.

## Decisiones

- Abreviaturas de mes propias (`ene ... sep ... dic`) en `PaymentsFormat.kt`: CLDR es-MX da "sept." y el test pide "sep". Día sin cero a la izquierda (`5 ene`), hueco no testeado.
- `formatAmount`: `DecimalFormat("#,##0.00")` con `DecimalFormatSymbols` de `es-MX` (independiente del Locale por defecto), `setScale(2, HALF_EVEN)`; prefijo `$` y sufijo ` <currency>`.
- Fecha "Venció el" solo para `Paid` (R7).
- Error de UI: siempre `payments_error` (R2 fija un único texto), sin distinguir el `AppError`.

## Desviaciones de design.md

- Color "Pagado = verde": el tema (`core/designsystem/Theme.kt`) no tiene token de éxito/verde distinto de `primary` (que ya usa Pendiente). Uso `colorScheme.secondary` (verde grisáceo) para no hardcodear colores ni tocar core fuera de alcance. Si se quiere un verde propio, hace falta añadir un token al tema (cambio de designsystem).
- Desconocido = `onSurfaceVariant` (neutro).

## Incidencias

- Intento 1 de verify falló por colisión con el build concurrente del subagente del splash (dos daemons Gradle sobre `app/build`: caché incremental de Kotlin corrupta y `return-value-debug.txt` de lint ausente). No era del código. Esperé a que no hubiera cliente Gradle activo y el intento 2 pasó.
