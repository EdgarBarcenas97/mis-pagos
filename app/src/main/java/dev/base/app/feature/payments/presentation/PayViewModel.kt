package dev.base.app.feature.payments.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.base.app.core.common.DispatcherProvider
import dev.base.app.feature.payments.domain.IdempotencyKeyFactory
import dev.base.app.feature.payments.domain.Payment
import dev.base.app.feature.payments.domain.PaymentGateway
import dev.base.app.feature.payments.domain.PendingPaymentStore
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Estado de un pago en curso. Sin entrada en el mapa = Listo. */
sealed interface PayState {
    data object Processing : PayState

    /** 0 → mostrar "Verificar de nuevo". */
    data class Verifying(val attemptsLeft: Int) : PayState

    data object Paid : PayState
}

enum class PayMessage { Paid, Declined }

data class PayUiState(
    val payStates: Map<String, PayState> = emptyMap(),
    val confirming: Payment? = null,
    val message: PayMessage? = null
)

@HiltViewModel
class PayViewModel @Inject constructor(
    private val gateway: PaymentGateway,
    private val keyFactory: IdempotencyKeyFactory,
    private val pendingStore: PendingPaymentStore,
    private val dispatchers: DispatcherProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow(PayUiState())

    // TODO(pay_payment T3) init: por cada entrada de pendingStore.all() → Verifying y verificación con esa llave (R15).
    val uiState: StateFlow<PayUiState> = _uiState.asStateFlow()

    /** Abre confirmación si es pagable y no hay pago en curso. */
    fun onPayClick(payment: Payment): Unit = TODO()

    fun onCancel(): Unit = TODO()

    /** R3/R4. */
    fun onConfirm(): Unit = TODO()

    /** R8. */
    fun onVerifyAgain(paymentId: String): Unit = TODO()

    /** R10. */
    fun onPaymentsLoaded(payments: List<Payment>): Unit = TODO()

    fun onMessageShown(): Unit = TODO()

    companion object {
        const val VERIFY_INTERVAL_MS = 3_000L
        const val VERIFY_ATTEMPTS = 3
    }
}
