package dev.base.app.feature.payments.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.base.app.core.common.DispatcherProvider
import dev.base.app.feature.payments.domain.IdempotencyKeyFactory
import dev.base.app.feature.payments.domain.Payment
import dev.base.app.feature.payments.domain.PayResult
import dev.base.app.feature.payments.domain.PaymentGateway
import dev.base.app.feature.payments.domain.PaymentStatus
import dev.base.app.feature.payments.domain.PendingPaymentStore
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val uiState: StateFlow<PayUiState> = _uiState.asStateFlow()
    private val jobs = mutableMapOf<String, Job>()

    init {
        pendingStore.all().forEach { (paymentId, key) ->
            setState(paymentId, PayState.Verifying(VERIFY_ATTEMPTS))
            jobs[paymentId] = viewModelScope.launch { verify(paymentId, key, immediate = true) }
        }
    }

    /** Abre confirmación si es pagable y no hay pago en curso. */
    fun onPayClick(payment: Payment) {
        if (!payment.status.isPayable) return
        if (_uiState.value.payStates[payment.id] != null) return
        _uiState.update { it.copy(confirming = payment) }
    }

    fun onCancel() {
        _uiState.update { it.copy(confirming = null) }
    }

    /** R3/R4. */
    fun onConfirm() {
        val payment = _uiState.value.confirming ?: return
        _uiState.update { it.copy(confirming = null) }
        if (_uiState.value.payStates[payment.id] != null) return
        val key = keyFactory.newKey()
        pendingStore.save(payment.id, key)
        setState(payment.id, PayState.Processing)
        jobs[payment.id] = viewModelScope.launch {
            val result = call(payment.id, key)
            if (!handleDefinitive(payment.id, result)) {
                setState(payment.id, PayState.Verifying(VERIFY_ATTEMPTS))
                verify(payment.id, key, immediate = false)
            }
        }
    }

    /** R8. */
    fun onVerifyAgain(paymentId: String) {
        val state = _uiState.value.payStates[paymentId]
        if (state !is PayState.Verifying || state.attemptsLeft > 0) return
        val key = pendingStore.all()[paymentId] ?: return
        setState(paymentId, PayState.Verifying(VERIFY_ATTEMPTS))
        jobs[paymentId] = viewModelScope.launch { verify(paymentId, key, immediate = true) }
    }

    /** R10. */
    fun onPaymentsLoaded(payments: List<Payment>) {
        payments.filter { it.status == PaymentStatus.Paid }.forEach { payment ->
            val state = _uiState.value.payStates[payment.id]
            if (state is PayState.Processing || state is PayState.Verifying) {
                jobs.remove(payment.id)?.cancel()
                pendingStore.remove(payment.id)
                setState(payment.id, PayState.Paid)
                _uiState.update { it.copy(message = PayMessage.Paid) }
            }
        }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    private suspend fun verify(paymentId: String, key: String, immediate: Boolean) {
        var attemptsLeft = VERIFY_ATTEMPTS
        if (immediate) {
            if (handleDefinitive(paymentId, call(paymentId, key))) return
        }
        while (attemptsLeft > 0) {
            delay(VERIFY_INTERVAL_MS)
            attemptsLeft--
            if (handleDefinitive(paymentId, call(paymentId, key))) return
            setState(paymentId, PayState.Verifying(attemptsLeft))
        }
    }

    private suspend fun call(paymentId: String, key: String): PayResult =
        withContext(dispatchers.io) { gateway.pay(paymentId, key) }

    /** true si el resultado es definitivo (Paid/Declined). */
    private fun handleDefinitive(paymentId: String, result: PayResult): Boolean = when (result) {
        PayResult.Paid -> {
            pendingStore.remove(paymentId)
            setState(paymentId, PayState.Paid)
            _uiState.update { it.copy(message = PayMessage.Paid) }
            true
        }
        PayResult.Declined -> {
            pendingStore.remove(paymentId)
            setState(paymentId, null)
            _uiState.update { it.copy(message = PayMessage.Declined) }
            true
        }
        PayResult.Unknown -> false
    }

    private fun setState(paymentId: String, state: PayState?) {
        _uiState.update {
            it.copy(payStates = if (state == null) it.payStates - paymentId else it.payStates + (paymentId to state))
        }
    }

    companion object {
        const val VERIFY_INTERVAL_MS = 3_000L
        const val VERIFY_ATTEMPTS = 3
    }
}
