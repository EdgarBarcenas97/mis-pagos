package dev.base.app.feature.payments.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.base.app.R
import dev.base.app.core.designsystem.AppTheme
import dev.base.app.core.designsystem.EmptyState
import dev.base.app.core.designsystem.ErrorState
import dev.base.app.core.designsystem.LoadingState
import dev.base.app.core.designsystem.Spacing
import dev.base.app.feature.payments.domain.Payment
import dev.base.app.feature.payments.domain.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDate

object PaymentsTags {
    const val LIST = "payments_list"
    fun item(id: String) = "payment_item_$id"
    fun amount(id: String) = "payment_amount_$id"
    fun due(id: String) = "payment_due_$id"
    fun status(id: String) = "payment_status_$id"
    fun payButton(id: String) = "pay_button_$id"
    fun verifying(id: String) = "payment_verifying_$id"
    fun verifyAgain(id: String) = "payment_verify_again_$id"
    const val PAY_CONFIRM = "pay_confirm"
    const val PAY_CANCEL = "pay_cancel"
}

/** Conecta el ViewModel con la pantalla. */
@Composable
fun PaymentsRoute(viewModel: PaymentsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PaymentsScreen(state = state, onRetry = viewModel::retry)
}

/** Sin estado: recibe datos y lambdas. Es lo que se testea y previsualiza. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
    state: PaymentsUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    payState: PayUiState = PayUiState(),
    onPayClick: (Payment) -> Unit = {},
    onConfirm: () -> Unit = {},
    onCancel: () -> Unit = {},
    onVerifyAgain: (String) -> Unit = {}
) {
    // TODO(pay_payment T4): botón Pagar, diálogo, Procesando, Verificando y chip tonal con payState y las lambdas.
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = {
                    Column {
                        Text(
                            stringResource(R.string.payments_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (state is PaymentsUiState.Content) {
                            Text(
                                stringResource(R.string.payments_loan, state.loanId),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (state) {
            PaymentsUiState.Loading -> LoadingState(contentModifier)
            PaymentsUiState.Empty -> EmptyState(stringResource(R.string.payments_empty), contentModifier)
            is PaymentsUiState.Error -> ErrorState(stringResource(R.string.payments_error), onRetry, contentModifier)
            is PaymentsUiState.Content -> PaymentsList(state.payments, contentModifier)
        }
    }
}

@Composable
private fun PaymentsList(payments: List<Payment>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(PaymentsTags.LIST),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(payments, key = { it.id }) { payment -> PaymentRow(payment) }
    }
}

@Composable
private fun PaymentRow(payment: Payment) {
    Card(modifier = Modifier.fillMaxWidth().testTag(PaymentsTags.item(payment.id))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    formatAmount(payment.amount, payment.currency),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag(PaymentsTags.amount(payment.id))
                )
                Text(
                    formatDueDate(payment.dueDate, paid = payment.status == PaymentStatus.Paid),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag(PaymentsTags.due(payment.id))
                )
            }
            StatusChip(payment)
        }
    }
}

@Composable
private fun StatusChip(payment: Payment) {
    val (label, color) = statusAppearance(payment.status)
    AssistChip(
        onClick = {},
        label = { Text(stringResource(label)) },
        colors = AssistChipDefaults.assistChipColors(labelColor = color),
        modifier = Modifier.testTag(PaymentsTags.status(payment.id))
    )
}

@Composable
private fun statusAppearance(status: PaymentStatus): Pair<Int, Color> {
    val colors = MaterialTheme.colorScheme
    return when (status) {
        PaymentStatus.Pending -> R.string.status_pending to colors.primary
        PaymentStatus.Overdue -> R.string.status_overdue to colors.error
        // El tema no tiene token "success"; secondary (verde grisáceo) es el verde disponible distinto de primary.
        PaymentStatus.Paid -> R.string.status_paid to colors.secondary
        PaymentStatus.InProcess -> R.string.status_in_process to colors.tertiary
        is PaymentStatus.Unknown -> R.string.status_unknown to colors.onSurfaceVariant
    }
}

@Preview(showBackground = true)
@Composable
private fun PaymentsContentPreview() {
    AppTheme {
        PaymentsScreen(
            state = PaymentsUiState.Content(
                "CR-88213",
                listOf(
                    Payment("p2", BigDecimal("1250.10"), "MXN", LocalDate.of(2026, 10, 31), PaymentStatus.Overdue),
                    Payment("p1", BigDecimal("1250.10"), "MXN", LocalDate.of(2026, 11, 30), PaymentStatus.Pending)
                )
            ),
            onRetry = {}
        )
    }
}
