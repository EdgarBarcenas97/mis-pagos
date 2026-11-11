# pay_payment: diseño

Toca `docs/design.md` → **Mis pagos**, *Estados de una fila al pagar* y el chip de estado.
**Cambio de diseño:** en Verificando, al agotar los reintentos automáticos aparece el botón
"Verificar de nuevo" (antes no tenía acción). Se actualiza `docs/design.md` y el mockup.

## Archivos

| Archivo | Qué |
|---|---|
| `feature/payments/domain/Payment.kt` | + `PayResult`, `PaymentGateway`, `IdempotencyKeyFactory` |
| `feature/payments/data/PaymentsApi.kt` | + `pay` (POST con `Idempotency-Key`) y `PayResponseDto` |
| `feature/payments/data/FakePaymentsServer.kt` | **nuevo**: estado del servidor simulado (pagos, llaves, cargos) |
| `feature/payments/data/FakeBackendInterceptor.kt` | Enruta GET lista / POST pay hacia `FakePaymentsServer` |
| `feature/payments/data/PaymentGatewayImpl.kt` | **nuevo**: `safeApiCall` → `PayResult` |
| `feature/payments/di/PaymentsModule.kt` | Provee `FakePaymentsServer` (singleton), gateway y key factory |
| `feature/payments/presentation/PayViewModel.kt` | **nuevo**: `PayUiState`, `PayState`, `PayViewModel` |
| `feature/payments/presentation/PaymentsScreen.kt` | Botón, diálogo, Procesando, Verificando, snackbar, chip |
| `core/designsystem/Theme.kt` | + color `success` / `successContainer` (claro y oscuro) |
| `res/values/strings.xml` | Textos de abajo |

**No se tocan** `PaymentsRepository`, `PaymentsViewModel` ni las firmas que usan los tests de
`payments_list` (siguen congelados y deben seguir en verde).

## Firmas públicas (contrato)

```kotlin
// domain
sealed interface PayResult {
    data object Paid : PayResult      // el servidor confirmó (o ya estaba pagado)
    data object Declined : PayResult  // rechazo definitivo: no hubo cargo
    data object Unknown : PayResult   // timeout / red / 5xx: no sabemos
}
interface PaymentGateway { suspend fun pay(paymentId: String, idempotencyKey: String): PayResult }
fun interface IdempotencyKeyFactory { fun newKey(): String }   // impl: UUID.randomUUID().toString()

// data
interface PaymentsApi {
    @GET("loans/payments") suspend fun getPayments(): LoanDto
    @POST("loans/payments/{id}/pay")
    suspend fun pay(@Path("id") id: String, @Header("Idempotency-Key") idempotencyKey: String): PayResponseDto
}
@Serializable data class PayResponseDto(val paymentId: String, val status: String)

class FakePaymentsServer(initialJson: String = FakeBackendInterceptor.SAMPLE_JSON) {
    /** Sin pagos registrados devuelve exactamente initialJson; después, el JSON con los estados actualizados. */
    @Synchronized fun paymentsJson(): String
    /** Registra el pago si no estaba pagado y la llave es nueva. Idempotente por llave y por pago. */
    @Synchronized fun registerPayment(paymentId: String, idempotencyKey: String)
    @Synchronized fun isPaid(paymentId: String): Boolean
    @Synchronized fun chargeCount(paymentId: String): Int   // cargos reales; nunca > 1
}

class FakeBackendInterceptor(
    private val random: Random = Random.Default,
    private val latencyMs: Long = 1_500,
    private val failureRate: Double = 0.30,
    private val body: String? = null,                       // null → server.paymentsJson()
    private val sleeper: (Long) -> Unit = Thread::sleep,
    private val server: FakePaymentsServer = FakePaymentsServer(),
    private val payLatencyMs: LongRange = 2_000L..5_000L,
    private val payTimeoutRate: Double = 0.30,
    private val registeredOnTimeoutRate: Double = 0.50,
    private val payDeclineRate: Double = 0.0,
) : Interceptor
// GET .../loans/payments → igual que en payments_list (latencia, 503 con failureRate).
// POST .../loans/payments/{id}/pay → duerme un valor de payLatencyMs; luego, con random:
//   decline → 422 sin registrar · timeout → (registra si toca) y lanza SocketTimeoutException
//   · si no → registra y 200 {"paymentId":id,"status":"PAGADO"}. Pago ya pagado → 200 PAGADO sin nuevo cargo.

class PaymentGatewayImpl @Inject constructor(private val api: PaymentsApi) : PaymentGateway
// safeApiCall: Success con status PAGADO → Paid · Server(code in 400..499) o NotFound → Declined · todo lo demás → Unknown.

// presentation
sealed interface PayState {
    data object Processing : PayState
    data class Verifying(val attemptsLeft: Int) : PayState   // 0 → mostrar "Verificar de nuevo"
    data object Paid : PayState
}
enum class PayMessage { Paid, Declined }
data class PayUiState(
    val payStates: Map<String, PayState> = emptyMap(),
    val confirming: Payment? = null,
    val message: PayMessage? = null,
)
@HiltViewModel
class PayViewModel @Inject constructor(
    private val gateway: PaymentGateway,
    private val keyFactory: IdempotencyKeyFactory,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    val uiState: StateFlow<PayUiState>
    fun onPayClick(payment: Payment)        // abre confirmación si es pagable y no hay pago en curso
    fun onCancel()
    fun onConfirm()                         // R3/R4
    fun onVerifyAgain(paymentId: String)    // R8
    fun onPaymentsLoaded(payments: List<Payment>)  // R10
    fun onMessageShown()
    companion object { const val VERIFY_INTERVAL_MS = 3_000L; const val VERIFY_ATTEMPTS = 3 }
}

@Composable fun PaymentsScreen(
    state: PaymentsUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    payState: PayUiState = PayUiState(),
    onPayClick: (Payment) -> Unit = {},
    onConfirm: () -> Unit = {},
    onCancel: () -> Unit = {},
    onVerifyAgain: (String) -> Unit = {},
)
object PaymentsTags { /* existentes */ 
    fun payButton(id: String) = "pay_button_$id"
    fun verifying(id: String) = "payment_verifying_$id"
    fun verifyAgain(id: String) = "payment_verify_again_$id"
    const val PAY_CONFIRM = "pay_confirm"; const val PAY_CANCEL = "pay_cancel"
}
```

`PaymentsRoute` usa los dos ViewModels, llama a `onPaymentsLoaded` cuando cambia el
contenido y muestra `message` en un `SnackbarHost` (luego `onMessageShown`). Un pago con
`PayState.Paid` se pinta como Pagado aunque la lista aún diga Pendiente.

## Textos (`strings.xml`)

| Clave | Texto |
|---|---|
| `pay_button` | Pagar |
| `pay_processing` | Procesando… |
| `pay_confirm_title` | Confirmar pago |
| `pay_confirm_body` | Vas a pagar %1$s de la cuota que vence el %2$s. |
| `pay_confirm_action` | Pagar %1$s (monto sin "MXN": `$1,250.10`) |
| `pay_cancel` | Cancelar |
| `pay_verifying_title` | Verificando tu pago… |
| `pay_verifying_body` | No vuelvas a pagar; te avisaremos en cuanto se confirme. |
| `pay_verify_again` | Verificar de nuevo |
| `pay_paid` | Pago registrado. |
| `pay_declined` | No se pudo procesar el pago y no se hizo ningún cargo. |

La fecha del diálogo es `30 nov 2026`: el mismo formateador de `PaymentsFormat.kt`, sin "Vence el".

## Chip (R13)
Relleno tonal sin acción: un `Surface`/`Box` con fondo `*Container` y texto `on*Container`, en
lugar de `AssistChip`. Colores: Pendiente → primaryContainer · Vencido → errorContainer ·
Pagado → `success`Container (verde nuevo en `Theme.kt`) · En proceso → tertiaryContainer ·
En revisión → surfaceVariant.

## Alternativas descartadas
- **Conciliar con un GET del estado del pago** en vez de reintentar con la misma llave: con
  "sigue pendiente" no se sabe si el cobro aún está en vuelo. El reintento idempotente da
  una respuesta definitiva y es el patrón estándar (Stripe, Adyen).
- **Extender `PaymentsRepository`/`PaymentsViewModel`:** rompe los tests congelados de
  `payments_list`. Un `PaymentGateway` y un `PayViewModel` aparte mantienen las features separadas.
- **Llave por intento** (nueva en cada reintento): es justo lo que causa cobros duplicados.
