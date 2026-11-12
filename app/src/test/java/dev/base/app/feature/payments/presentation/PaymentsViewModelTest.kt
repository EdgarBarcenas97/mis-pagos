package dev.base.app.feature.payments.presentation

import dev.base.app.core.common.AppError
import dev.base.app.core.common.AppResult
import dev.base.app.core.common.DispatcherProvider
import dev.base.app.feature.payments.domain.Loan
import dev.base.app.feature.payments.domain.Payment
import dev.base.app.feature.payments.domain.PaymentStatus
import dev.base.app.feature.payments.domain.PaymentsRepository
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private class TestDispatcherProvider(dispatcher: TestDispatcher) : DispatcherProvider {
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

/** Fake: devuelve los resultados en orden y cuenta llamadas. Si hay [gate], espera a que se complete. */
private class FakePaymentsRepository(vararg results: AppResult<Loan>) : PaymentsRepository {
    private val queue = ArrayDeque(results.toList())
    var calls = 0
        private set
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun getPayments(): AppResult<Loan> {
        calls++
        gate?.await()
        return if (queue.size > 1) queue.removeFirst() else queue.first()
    }
}

private val samplePayments = listOf(
    Payment("p3", BigDecimal("1250.10"), "MXN", LocalDate.of(2026, 9, 30), PaymentStatus.Paid),
    Payment("p2", BigDecimal("1250.10"), "MXN", LocalDate.of(2026, 10, 31), PaymentStatus.Overdue),
    Payment("p1", BigDecimal("1250.10"), "MXN", LocalDate.of(2026, 11, 30), PaymentStatus.Pending),
    Payment("p4", BigDecimal("980.55"), "MXN", LocalDate.of(2026, 12, 31), PaymentStatus.InProcess)
)

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(repository: PaymentsRepository) =
        PaymentsViewModel(repository, TestDispatcherProvider(dispatcher))

    // Cubre: payments_list/R1
    @Test
    fun `al abrir muestra Loading mientras pide los pagos`() = runTest(dispatcher) {
        val repository = FakePaymentsRepository(AppResult.Success(Loan("CR-88213", samplePayments)))
        repository.gate = CompletableDeferred()

        val vm = viewModel(repository)
        assertEquals(PaymentsUiState.Loading, vm.uiState.value)

        advanceUntilIdle()
        assertEquals(1, repository.calls)
        assertEquals(PaymentsUiState.Loading, vm.uiState.value)
    }

    // Cubre: payments_list/R5
    @Test
    fun `pagos recibidos se muestran como Content con el credito`() = runTest(dispatcher) {
        val repository = FakePaymentsRepository(AppResult.Success(Loan("CR-88213", samplePayments)))

        val vm = viewModel(repository)
        advanceUntilIdle()

        assertEquals(PaymentsUiState.Content("CR-88213", samplePayments), vm.uiState.value)
        assertEquals(1, repository.calls)
    }

    // Cubre: payments_list/R2
    @Test
    fun `fallo de red muestra Error con el AppError`() = runTest(dispatcher) {
        val vm = viewModel(FakePaymentsRepository(AppResult.Failure(AppError.Network)))
        advanceUntilIdle()

        assertEquals(PaymentsUiState.Error(AppError.Network), vm.uiState.value)
    }

    // Cubre: payments_list/R2, payments_list/R10
    @Test
    fun `fallo del servidor 503 muestra Error`() = runTest(dispatcher) {
        val vm = viewModel(FakePaymentsRepository(AppResult.Failure(AppError.Server(503))))
        advanceUntilIdle()

        assertEquals(PaymentsUiState.Error(AppError.Server(503)), vm.uiState.value)
    }

    // Cubre: payments_list/R4
    @Test
    fun `respuesta sin pagos muestra Empty`() = runTest(dispatcher) {
        val vm = viewModel(FakePaymentsRepository(AppResult.Success(Loan("CR-88213", emptyList()))))
        advanceUntilIdle()

        assertEquals(PaymentsUiState.Empty, vm.uiState.value)
    }

    // Cubre: payments_list/R3
    @Test
    fun `retry vuelve a Loading y pide los pagos de nuevo`() = runTest(dispatcher) {
        val repository = FakePaymentsRepository(
            AppResult.Failure(AppError.Network),
            AppResult.Success(Loan("CR-88213", samplePayments))
        )
        val vm = viewModel(repository)
        advanceUntilIdle()
        assertEquals(PaymentsUiState.Error(AppError.Network), vm.uiState.value)

        val gate = CompletableDeferred<Unit>()
        repository.gate = gate
        vm.retry()
        advanceUntilIdle()
        assertEquals(PaymentsUiState.Loading, vm.uiState.value)
        assertEquals(2, repository.calls)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(PaymentsUiState.Content("CR-88213", samplePayments), vm.uiState.value)
    }

    // Cubre: payments_list/R3
    @Test
    fun `retry que vuelve a fallar termina en Error`() = runTest(dispatcher) {
        val repository = FakePaymentsRepository(
            AppResult.Failure(AppError.Network),
            AppResult.Failure(AppError.Server(503))
        )
        val vm = viewModel(repository)
        advanceUntilIdle()

        vm.retry()
        advanceUntilIdle()

        assertEquals(PaymentsUiState.Error(AppError.Server(503)), vm.uiState.value)
        assertEquals(2, repository.calls)
    }
}
