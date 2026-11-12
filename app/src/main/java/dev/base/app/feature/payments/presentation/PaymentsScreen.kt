package dev.base.app.feature.payments.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.base.app.R
import dev.base.app.core.designsystem.AppColors
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
fun PaymentsRoute(viewModel: PaymentsViewModel = hiltViewModel(), payViewModel: PayViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val payState by payViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val content = state as? PaymentsUiState.Content
    LaunchedEffect(content) {
        content?.let { payViewModel.onPaymentsLoaded(it.payments) }
    }
    val message = payState.message
    val paidText = stringResource(R.string.pay_paid)
    val declinedText = stringResource(R.string.pay_declined)
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(if (message == PayMessage.Paid) paidText else declinedText)
            payViewModel.onMessageShown()
        }
    }
    Box {
        PaymentsScreen(
            state = state,
            onRetry = viewModel::retry,
            payState = payState,
            onPayClick = payViewModel::onPayClick,
            onConfirm = payViewModel::onConfirm,
            onCancel = payViewModel::onCancel,
            onVerifyAgain = payViewModel::onVerifyAgain
        )
        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
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
    payState.confirming?.let { ConfirmDialog(it, onConfirm, onCancel) }
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
            is PaymentsUiState.Content -> Column(contentModifier) {
                // offline_last_known: banner sobre la lista; con staleSince != null, Pagar debe ir desactivado.
                state.staleSince?.let { LastUpdatedBanner(it) }
                PaymentsList(state.payments, payState.payStates, onPayClick, onVerifyAgain)
            }
        }
    }
}

@Composable
private fun PaymentsList(
    payments: List<Payment>,
    payStates: Map<String, PayState>,
    onPayClick: (Payment) -> Unit,
    onVerifyAgain: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(PaymentsTags.LIST),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(payments, key = { it.id }) { payment ->
            PaymentRow(payment, payStates[payment.id], onPayClick, onVerifyAgain)
        }
    }
}

@Composable
private fun PaymentRow(
    payment: Payment,
    payState: PayState?,
    onPayClick: (Payment) -> Unit,
    onVerifyAgain: (String) -> Unit
) {
    val status = if (payState == PayState.Paid) PaymentStatus.Paid else payment.status
    Card(modifier = Modifier.fillMaxWidth().testTag(PaymentsTags.item(payment.id))) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        formatDueDate(payment.dueDate, paid = status == PaymentStatus.Paid),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag(PaymentsTags.due(payment.id))
                    )
                }
                StatusChip(payment.id, status)
            }
            when (payState) {
                is PayState.Verifying -> VerifyingBox(payment.id, payState, onVerifyAgain)
                PayState.Processing -> Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth().testTag(PaymentsTags.payButton(payment.id))
                ) { Text(stringResource(R.string.pay_processing)) }
                PayState.Paid -> Unit
                null -> if (status.isPayable) {
                    Button(
                        onClick = { onPayClick(payment) },
                        modifier = Modifier.fillMaxWidth().testTag(PaymentsTags.payButton(payment.id))
                    ) { Text(stringResource(R.string.pay_button)) }
                }
            }
        }
    }
}

@Composable
private fun VerifyingBox(paymentId: String, state: PayState.Verifying, onVerifyAgain: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(PaymentsTags.verifying(paymentId)),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Text(stringResource(R.string.pay_verifying_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.pay_verifying_body), style = MaterialTheme.typography.bodyMedium)
        if (state.attemptsLeft == 0) {
            OutlinedButton(
                onClick = { onVerifyAgain(paymentId) },
                modifier = Modifier.testTag(PaymentsTags.verifyAgain(paymentId))
            ) { Text(stringResource(R.string.pay_verify_again)) }
        }
    }
}

@Composable
private fun ConfirmDialog(payment: Payment, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val amount = formatAmount(payment.amount, payment.currency)
    val date = formatDueDate(payment.dueDate, paid = false).removePrefix("Vence el ")
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.pay_confirm_title)) },
        text = { Text(stringResource(R.string.pay_confirm_body, amount, date)) },
        confirmButton = {
            Button(onClick = onConfirm, modifier = Modifier.testTag(PaymentsTags.PAY_CONFIRM)) {
                Text(stringResource(R.string.pay_confirm_action, amount.removeSuffix(" ${payment.currency}")))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.testTag(PaymentsTags.PAY_CANCEL)) {
                Text(stringResource(R.string.pay_cancel))
            }
        }
    )
}

/** Chip tonal sin acción (R13). */
@Composable
private fun StatusChip(paymentId: String, status: PaymentStatus) {
    val (label, colors) = statusAppearance(status)
    Surface(
        color = colors.first,
        contentColor = colors.second,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.testTag(PaymentsTags.status(paymentId))
    ) {
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
        )
    }
}

@Composable
private fun statusAppearance(status: PaymentStatus): Pair<Int, Pair<Color, Color>> {
    val colors = MaterialTheme.colorScheme
    val extended = AppColors.current
    return when (status) {
        PaymentStatus.Pending -> R.string.status_pending to (colors.primaryContainer to colors.onPrimaryContainer)
        PaymentStatus.Overdue -> R.string.status_overdue to (colors.errorContainer to colors.onErrorContainer)
        PaymentStatus.Paid -> R.string.status_paid to (extended.success to extended.onSuccess)
        PaymentStatus.InProcess ->
            R.string.status_in_process to (colors.tertiaryContainer to colors.onTertiaryContainer)
        is PaymentStatus.Unknown -> R.string.status_unknown to (colors.surfaceVariant to colors.onSurfaceVariant)
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
