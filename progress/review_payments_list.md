# Review payments_list — ronda 1
Veredicto: APPROVED

Rama `feat/payments_list` en HEAD `c1ccdca`. red_sha `a1d9e5d`. Implementación revisada contra `fe9baea`.

## Objetivas    1 [x] 2 [x] 3 [x] 4 [x]

1. `scripts/trace.sh payments_list`: R1–R10 en `[OK]`, todos con al menos un test. No hay requisitos huérfanos (exit 0).
2. Tests congelados: `git diff a1d9e5d HEAD -- app/src/test app/src/androidTest` sale vacío.
3. Tasks: T1–T4 en `[x]` en `specs/payments_list/tasks.md`.
4. `scripts/verify.sh`: `[OK] verify`, exit 0 (ktlint + Android Lint + unitarios + assembleDebug + assembleDebugAndroidTest).
   **Riesgo:** no se corrió `--ui`. `PaymentsScreenTest` compila pero no se ejecutó en emulador (esto también lo dice `impl_payments_list.md`).

## Contraste con design.md (diff fe9baea..HEAD en feature/payments)

- Firmas: `Payment`, `PaymentStatus` (+ `isPayable`, `fromApi`), `Loan`, `PaymentsRepository`, `PaymentsApi` (`@GET("loans/payments")`), DTOs, `BigDecimalSerializer`, `toDomain`, `FakeBackendInterceptor` (con sus parámetros por defecto), `PaymentsRepositoryImpl`, `formatAmount`, `formatDueDate`, `PaymentsUiState`, `PaymentsViewModel` (init + `retry`), `PaymentsRoute`, `PaymentsScreen` y `PaymentsTags`: coinciden con el contrato.
- BigDecimal sin Double: el serializer lee `decodeJsonElement().jsonPrimitive.content` (texto crudo) y hace `BigDecimal(text)`. Nunca pasa por `Double`.
- Fecha UTC: `Instant.parse(dueDate).atZone(ZoneOffset.UTC).toLocalDate()` (PaymentsMapper.kt:19).
- safeApiCall: `PaymentsRepositoryImpl` usa `safeApiCall` de `core/network`. Si el mapeo lanza `DateTimeParseException`, se convierte en `AppError.Serialization`.
- Orden asc: `sortedBy { it.dueDate }` (PaymentsRepositoryImpl.kt:20).
- Textos: `strings.xml` coincide exactamente con la tabla de design.md (título, `Crédito %1$s`, error, vacío y los 5 estados). "Reintentar" sale de `state_retry` de StateViews. "Vence el"/"Venció el" están en `PaymentsFormat.kt`, como pide el diseño.
- testTags: `payments_list`, `payment_item_/amount_/due_/status_<id>`. Loading/Empty/Error reutilizan `StateViews` (tags `state_*`).
- Interceptor: espera con `sleeper(latencyMs)`, y si `random.nextDouble() < failureRate` responde 503; si no, 200 con `body`. No llama a `chain.proceed`, así que nada sale a la red.
- Hilo: el ViewModel llama al repositorio con `withContext(dispatchers.io)` (DispatcherProvider inyectado). No hay `Dispatchers.*` directo.
- Design system: usa `Spacing.*`, `MaterialTheme.typography/colorScheme`. No hay colores ni dp hardcodeados en `feature/payments`.

## Hallazgos    (archivo:línea · problema · cambio pedido)

Ninguno bloqueante.

- PaymentsScreen.kt (`statusAppearance`, chip Pagado) · Desviación ya conocida: usa `colorScheme.secondary` en vez de un verde propio, porque no hay token de éxito en el tema. · Va al humano en la demo. Si quiere verde, hay que agregar un token `success` en `core/designsystem` (feature aparte).
- PaymentsScreen.kt (`StatusChip`) · `AssistChip(onClick = {})`: el chip se puede pulsar y TalkBack lo anuncia como botón, aunque no hace nada. · Opcional, no bloquea: usar un elemento no interactivo (p. ej. `Surface` + `Text` con forma de chip), o dejarlo así hasta `pay_payment`.
- PaymentsFormat.kt · Las abreviaturas de mes están fijadas a mano (`sep` en vez de `sept.` de CLDR). Está justificado y testeado. · Ningún cambio.

## Observaciones aparte: feat(splash) (6a104c2, sdd: false)

- No rompe payments_list: no toca `feature/payments` ni los tests. `verify.sh` pasa con el splash incluido. `MainActivity` solo agrega `installSplashScreen()` antes de `super.onCreate`, y el inicio sigue siendo `PaymentsDestination`.
- `res/values/themes.xml` y `res/values-v31/themes.xml` · `windowSplashScreenBackground` lleva `#FF1F6F5C` hardcodeado y duplicado. Es el valor de primary de AppTheme, pero escrito a mano: si el tema cambia, el splash queda desfasado. Lo aceptable sería un `colors.xml` con un comentario que lo enlace al token. Es menor y está fuera del alcance de esta feature.
- `splash_brand` en `strings.xml` no se usa (lint `UnusedResources`). El texto "Mis pagos" del splash está dibujado con trazos en un vector, así que no se traduce y no es accesible. Además el branding solo aparece desde API 31.
- La app no tiene ícono de launcher (no hay `android:icon`). Ya está anotado en `impl_splash.md`.
