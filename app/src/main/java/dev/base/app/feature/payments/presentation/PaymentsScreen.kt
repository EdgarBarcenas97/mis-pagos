package dev.base.app.feature.payments.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

object PaymentsTags {
    const val LIST = "payments_list"
    fun item(id: String) = "payment_item_$id"
    fun amount(id: String) = "payment_amount_$id"
    fun due(id: String) = "payment_due_$id"
    fun status(id: String) = "payment_status_$id"
}

/** Conecta el ViewModel con la pantalla. */
@Composable
fun PaymentsRoute(viewModel: PaymentsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PaymentsScreen(state = state, onRetry = viewModel::retry)
}

/** Sin estado: recibe datos y lambdas. Es lo que se testea y previsualiza. */
@Composable
fun PaymentsScreen(state: PaymentsUiState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    TODO()
}
