package dev.base.app.feature.payments.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.base.app.core.common.AppError
import dev.base.app.core.common.DispatcherProvider
import dev.base.app.feature.payments.domain.Payment
import dev.base.app.feature.payments.domain.PaymentsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface PaymentsUiState {
    data object Loading : PaymentsUiState

    data object Empty : PaymentsUiState

    data class Content(val loanId: String, val payments: List<Payment>) : PaymentsUiState

    data class Error(val error: AppError) : PaymentsUiState
}

@HiltViewModel
class PaymentsViewModel @Inject constructor(
    private val repository: PaymentsRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<PaymentsUiState>(PaymentsUiState.Loading)

    /** Carga en init. */
    val uiState: StateFlow<PaymentsUiState> = _uiState.asStateFlow()

    fun retry(): Unit = TODO()
}
