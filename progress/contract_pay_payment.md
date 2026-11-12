# Contrato: pay_payment

Estado: **ok**. `./gradlew --console=plain --quiet assembleDebug assembleDebugAndroidTest ktlintCheck testDebugUnitTest` en verde; también `lintDebug`.
Tests de `payments_list` sin tocar y en verde (48 tests unitarios, 0 fallos; `PaymentsScreenTest` compila).

## Firmas (cuerpos `TODO()`)
- `feature/payments/domain/Payment.kt`: + `PayResult`, `PaymentGateway`, `IdempotencyKeyFactory`, `PendingPaymentStore` (reales: son tipos).
- `feature/payments/data/PaymentsApi.kt`: + `pay` (POST `loans/payments/{id}/pay`, `@Header("Idempotency-Key")`) y `PayResponseDto`.
- `feature/payments/data/FakePaymentsServer.kt` (nuevo): `registerPayment`, `isPaid`, `chargeCount` en `TODO()`.
  **Excepción:** `paymentsJson()` devuelve `initialJson` (lo que el contrato pide sin pagos registrados). Si fuera `TODO()` el GET con `body = null` (default) rompería la lista de la app. T1 debe reflejar los pagos registrados.
- `feature/payments/data/FakeBackendInterceptor.kt`: parámetros nuevos al final (`server`, `payLatencyMs`, `payTimeoutRate`, `registeredOnTimeoutRate`, `payDeclineRate`); `body: String? = null` → `server.paymentsJson()`. `intercept` enruta POST `.../{id}/pay` → `pay(chain, id)` (`TODO()`), y todo lo demás → camino GET sin cambios (latencia, 503 con `failureRate`).
- `feature/payments/data/PaymentGatewayImpl.kt` (nuevo): `pay` en `TODO()`.
- `feature/payments/data/PrefsPendingPaymentStore.kt` (nuevo): abre `SharedPreferences` "pending_payments"; `save/remove/all` en `TODO()`.
- `feature/payments/presentation/PayViewModel.kt` (nuevo): `PayState`, `PayMessage`, `PayUiState`, `PayViewModel` (uiState inicial `PayUiState()`, constantes `VERIFY_INTERVAL_MS`/`VERIFY_ATTEMPTS`). El `init` aún **no** lee `pendingStore` (lo haría crashear); queda TODO(T3).

## Reales
- `di/PaymentsModule.kt`: `FakePaymentsServer` `@Singleton` inyectado en el `FakeBackendInterceptor` del `PaymentsApi`; `@Binds` gateway y store (`@Singleton`); `IdempotencyKeyFactory` con `UUID.randomUUID().toString()`.
- `res/values/strings.xml`: `app_name` = "Mis pagos" y los 11 textos `pay_*` de design.md.
- `core/designsystem/Theme.kt`: `ExtendedColors` (`success`, `onSuccess`, `successContainer`, `onSuccessContainer`) claro/oscuro vía `CompositionLocal`; acceso `AppColors.current`. Además `surfaceContainer` y `onSurfaceVariant` con tinte verde (ver header).
- `presentation/PaymentsScreen.kt`: params nuevos con default (`payState`, `onPayClick`, `onConfirm`, `onCancel`, `onVerifyAgain`) aún sin usar (TODO T4); tags `payButton`, `verifying`, `verifyAgain`, `PAY_CONFIRM`, `PAY_CANCEL`. Chip sigue siendo `AssistChip` (cambia en T4). `PaymentsRoute` aún usa solo `PaymentsViewModel` (cablear `PayViewModel` + `SnackbarHost` en T4; hoy sus métodos son TODO).

## Arreglo extra (pedido por el humano vía coordinador): header de "Mis pagos"
- `MainActivity.kt`: se quitó el `Scaffold` externo de `AppNavHost` (aplicaba el inset de status bar dos veces). Cada pantalla maneja sus insets con su propio `Scaffold`.
- `PaymentsScreen`: `TopAppBar` con `containerColor = surfaceContainer`, título en `titleLarge`, subtítulo "Crédito …" en `bodyMedium` + `onSurfaceVariant`; el título queda a 16dp (inset estándar de `TopAppBar` sin icono), alineado con las cards. El padding del Scaffold se aplica una sola vez.
- Decisión: elegí `surfaceContainer` y no `primary`: con `enableEdgeToEdge` en tema claro los íconos de la status bar son oscuros y sobre el verde primario quedarían ilegibles. Como el `surfaceContainer` por defecto de M3 es lavanda, lo definí en `Theme.kt` con un gris verdoso (claro `#EBEFEC`, oscuro `#1B211F`) y `onSurfaceVariant` a juego.
- Evidencia: `progress/evidence/pay_payment/header.png` (emulator-5554): el título queda justo bajo la status bar, sin hueco doble.
- Observación para T4 (sin tocar): las cards usan el `surfaceContainerHighest` por defecto de M3 (lavanda), que no casa con la marca verde.

## Notas
- Una compilación incremental dio "Unresolved reference" falsos (`EmptyState`, `formatAmount`) que desaparecieron con `compileDebugKotlin --rerun-tasks`; caché incremental de Kotlin, no código.
- No se commitea `.harness/phase` (lo modifica el coordinador).
