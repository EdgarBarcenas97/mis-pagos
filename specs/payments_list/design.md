# payments_list: diseño

Toca `docs/design.md` → pantalla **Mis pagos**: estados Cargando, Error, Vacío,
Contenido y *Fila de pago* (monto, fecha y chip; el botón Pagar llega en `pay_payment`).
No cambia el diseño aprobado.

## Archivos

| Archivo | Qué |
|---|---|
| `feature/payments/domain/Payment.kt` | `Payment`, `PaymentStatus`, `PaymentsRepository` |
| `feature/payments/data/FakePaymentsApi.kt` | Backend simulado con el JSON del enunciado |
| `feature/payments/data/PaymentsMapper.kt` | JSON → dominio |
| `feature/payments/data/PaymentsRepositoryImpl.kt` | Llama al API, mapea, ordena, `AppResult` |
| `feature/payments/di/PaymentsModule.kt` | Binds de Hilt |
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
class PaymentsUnavailableException : Exception()
class FakePaymentsApi(
    private val random: Random = Random.Default,
    private val latencyMs: Long = 1_500,
    private val failureRate: Double = 0.30,
    private val json: String = SAMPLE_JSON,
) {
    /** Espera latencyMs (delay, testeable con tiempo virtual); si random < failureRate lanza PaymentsUnavailableException; si no, devuelve el JSON. */
    suspend fun getPayments(): String
    companion object { const val SAMPLE_JSON: String }  // JSON exacto del enunciado
}
/** Monto leído como texto del primitivo JSON → BigDecimal; dueDate = Instant.parse(..).atZone(UTC).toLocalDate(). Lanza SerializationException/IllegalArgumentException si el JSON es inválido. */
fun parseLoan(json: String): Loan
class PaymentsRepositoryImpl @Inject constructor(
    private val api: FakePaymentsApi,
) : PaymentsRepository
// PaymentsUnavailableException → AppError.Network; error de parseo → AppError.Serialization; éxito → pagos ordenados por dueDate asc.

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

`FakePaymentsApi` se provee en `PaymentsModule` como `@Singleton` con los valores
por defecto. Los tests lo crean con `Random(seed)` o con `failureRate` 0 o 1.

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

- **Retrofit + MockWebServer:** más realista, pero no cabe en 45 min. El repositorio
  aísla la fuente, así que el cambio después no toca la UI.
- **`amount: Double` en el DTO:** `1250.10` no es exacto en binario, y en dinero no se acepta.
- **Fecha en la zona del dispositivo:** en CDMX mostraría 29 nov y adelantaría el vencimiento.
