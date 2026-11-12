# payments_list: diseño

Toca `docs/design.md` → pantalla **Mis pagos**: estados Cargando, Error, Vacío,
Contenido y *Fila de pago* (monto, fecha y chip; el botón Pagar llega en `pay_payment`).
No cambia el diseño aprobado.

## Archivos

| Archivo | Qué |
|---|---|
| `feature/payments/domain/Payment.kt` | `Payment`, `PaymentStatus`, `PaymentsRepository` |
| `feature/payments/data/PaymentsApi.kt` | Interfaz Retrofit y DTOs (`kotlinx.serialization`) |
| `feature/payments/data/BigDecimalSerializer.kt` | Lee el número JSON como texto → `BigDecimal` |
| `feature/payments/data/FakeBackendInterceptor.kt` | Backend simulado dentro de OkHttp: latencia + 30% de fallos + JSON del enunciado |
| `feature/payments/data/PaymentsMapper.kt` | DTO → dominio |
| `feature/payments/data/PaymentsRepositoryImpl.kt` | `safeApiCall` + mapeo + orden |
| `feature/payments/di/PaymentsModule.kt` | OkHttp con el interceptor, Retrofit, `PaymentsApi` y binds |
| `feature/payments/presentation/PaymentsFormat.kt` | `formatAmount`, `formatDueDate` |
| `feature/payments/presentation/PaymentsViewModel.kt` | `PaymentsUiState`, `PaymentsViewModel` |
| `feature/payments/presentation/PaymentsScreen.kt` | `PaymentsRoute`, `PaymentsScreen`, `PaymentsTags` |
| `MainActivity.kt` | `PaymentsDestination` como inicio (se elimina `feature/home`) |
| `res/values/strings.xml` | Textos de abajo |

Paquete base: `dev.base.app.feature.payments`.

## Firmas públicas (contrato)

```kotlin
// domain
data class Payment(
    val id: String,
    val amount: BigDecimal,
    val currency: String,
    val dueDate: LocalDate,
    val status: PaymentStatus,
)
sealed interface PaymentStatus {
    val isPayable: Boolean
    data object Pending : PaymentStatus   // PENDIENTE, pagable
    data object Overdue : PaymentStatus   // VENCIDO, pagable
    data object Paid : PaymentStatus      // PAGADO
    data object InProcess : PaymentStatus // EN_PROCESO
    data class Unknown(val raw: String) : PaymentStatus
    companion object { fun fromApi(raw: String): PaymentStatus }
}
data class Loan(val loanId: String, val payments: List<Payment>)
interface PaymentsRepository { suspend fun getPayments(): AppResult<Loan> }

// data
interface PaymentsApi {
    @GET("loans/payments") suspend fun getPayments(): LoanDto
}
@Serializable data class LoanDto(val loanId: String, val payments: List<PaymentDto>)
@Serializable data class PaymentDto(
    val id: String,
    @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal, // texto del primitivo, nunca Double
    val currency: String,
    val dueDate: String,   // ISO-8601 instant
    val status: String,    // texto libre: un estado nuevo no rompe el parseo
)
object BigDecimalSerializer : KSerializer<BigDecimal>
/** DTO → dominio. dueDate = Instant.parse(..).atZone(UTC).toLocalDate(). Fecha inválida → DateTimeParseException. */
fun LoanDto.toDomain(): Loan

/** Backend simulado. Si random.nextDouble() < failureRate responde HTTP 503; si no, 200 con [body]. Antes duerme latencyMs con [sleeper] (inyectable en tests). */
class FakeBackendInterceptor(
    private val random: Random = Random.Default,
    private val latencyMs: Long = 1_500,
    private val failureRate: Double = 0.30,
    private val body: String = SAMPLE_JSON,
    private val sleeper: (Long) -> Unit = Thread::sleep,
) : Interceptor {
    companion object { const val SAMPLE_JSON: String /* JSON exacto del enunciado */ }
}

class PaymentsRepositoryImpl @Inject constructor(
    private val api: PaymentsApi,
) : PaymentsRepository
// safeApiCall de core/network: IOException → Network, HTTP → Server/NotFound, SerializationException → Serialization.
// Error de mapeo (fecha inválida) → AppError.Serialization. Éxito → pagos ordenados por dueDate asc.

// presentation
fun formatAmount(amount: BigDecimal, currency: String): String  // "$1,250.10 MXN" (Locale es-MX, 2 decimales)
fun formatDueDate(date: LocalDate, paid: Boolean): String       // "Vence el 30 nov 2026" / "Venció el 30 sep 2026"
sealed interface PaymentsUiState {
    data object Loading : PaymentsUiState
    data object Empty : PaymentsUiState
    data class Content(val loanId: String, val payments: List<Payment>) : PaymentsUiState
    data class Error(val error: AppError) : PaymentsUiState
}
@HiltViewModel
class PaymentsViewModel @Inject constructor(
    private val repository: PaymentsRepository,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    val uiState: StateFlow<PaymentsUiState>   // carga en init
    fun retry()
}
@Composable fun PaymentsRoute(viewModel: PaymentsViewModel = hiltViewModel())
@Composable fun PaymentsScreen(state: PaymentsUiState, onRetry: () -> Unit, modifier: Modifier = Modifier)
object PaymentsTags {
    const val LIST = "payments_list"
    fun item(id: String) = "payment_item_$id"
    fun amount(id: String) = "payment_amount_$id"
    fun due(id: String) = "payment_due_$id"
    fun status(id: String) = "payment_status_$id"
}
```

`PaymentsModule` construye un `OkHttpClient` derivado del de `CoreModule`
(`newBuilder().addInterceptor(FakeBackendInterceptor())`) y un Retrofit con
`Network.retrofit("https://api.mispagos.fake/", client)`. Nada sale a la red: el
interceptor responde antes. Los tests del repositorio usan **MockWebServer** con
Retrofit real (sin el interceptor), y los del interceptor usan un `OkHttpClient`
con `failureRate` 0 o 1 y un `sleeper` que registra la espera.

## UI

- `TopAppBar`: título `Mis pagos`, subtítulo `Crédito <loanId>`.
- Loading, Empty y Error reutilizan `LoadingState`, `EmptyState` y `ErrorState` de
  `core/designsystem/StateViews.kt` (tags `state_loading`, `state_empty`,
  `state_error`, `state_retry`).
- Contenido: `LazyColumn` (`payments_list`) de `Card` (`payment_item_<id>`) con el monto,
  la fecha, y un chip de estado con color semántico (Vencido = error, Pagado = verde,
  En proceso = tertiary, Pendiente = primary, En revisión = neutro).

### Textos (`strings.xml`)

| Clave | Texto |
|---|---|
| `payments_title` | Mis pagos |
| `payments_loan` | Crédito %1$s |
| `payments_error` | No pudimos cargar tus pagos. Revisa tu conexión e inténtalo de nuevo. |
| `payments_empty` | Aún no tienes pagos programados para este crédito. |
| `status_pending` / `status_overdue` / `status_paid` / `status_in_process` / `status_unknown` | Pendiente / Vencido / Pagado / En proceso / En revisión |

Los textos de fecha del formateador ("Vence el" / "Venció el") viven en
`PaymentsFormat.kt` para poder testearlos en JVM.

## Alternativas descartadas

- **Fake en código (sin Retrofit):** más rápido, pero no prueba el parseo HTTP ni los
  códigos de error. El humano eligió Retrofit + MockWebServer (D1 de la revisión).
- **`amount: Double` en el DTO:** `1250.10` no es exacto en binario, y en dinero no se acepta.
- **Fecha en la zona del dispositivo:** en CDMX mostraría 29 nov y adelantaría el vencimiento.
