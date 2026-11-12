package dev.base.app.feature.payments.presentation

import dev.base.app.feature.payments.domain.PayResult
import dev.base.app.feature.payments.domain.Payment
import dev.base.app.feature.payments.domain.PaymentStatus
import dev.base.app.feature.payments.presentation.PayViewModel.Companion.VERIFY_ATTEMPTS
import dev.base.app.feature.payments.presentation.PayViewModel.Companion.VERIFY_INTERVAL_MS
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PayViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val events = mutableListOf<String>()
    private val store = InMemoryPendingPaymentStore(events)
    private val keys = SequentialKeyFactory()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun gateway(vararg results: PayResult) = FakePaymentGateway(events, *results, store = store)

    private fun viewModel(gateway: FakePaymentGateway) =
        PayViewModel(gateway, keys, store, PayTestDispatchers(dispatcher))

    private fun PayViewModel.stateOf(id: String): PayState? = uiState.value.payStates[id]

    private fun PayViewModel.pay(payment: Payment) {
        onPayClick(payment)
        onConfirm()
    }

    /** Avanza el reloj virtual [ms] y ejecuta lo que toque justo en ese instante. */
    private fun TestScope.advance(ms: Long) {
        advanceTimeBy(ms)
        runCurrent()
    }

    private fun assertVerifying(state: PayState?) =
        assertTrue("se esperaba Verifying y fue $state", state is PayState.Verifying)

    // ---------- R1 ----------

    // Cubre: pay_payment/R1, pay_payment/R2
    @Test
    fun `onPayClick en Pendiente o Vencido abre la confirmacion de ese pago`() = runTest(dispatcher) {
        val vm = viewModel(gateway(PayResult.Paid))

        vm.onPayClick(payP1)
        assertEquals(payP1, vm.uiState.value.confirming)

        vm.onCancel()
        vm.onPayClick(payP2)
        assertEquals(payP2, vm.uiState.value.confirming)
    }

    // Cubre: pay_payment/R1
    @Test
    fun `onPayClick en Pagado, En proceso o estado desconocido no abre la confirmacion`() = runTest(dispatcher) {
        val gateway = gateway(PayResult.Paid)
        val vm = viewModel(gateway)

        listOf(payP3, payP4, payP5).forEach { payment ->
            vm.onPayClick(payment)
            assertNull("no debe confirmar ${payment.id}", vm.uiState.value.confirming)
        }
        vm.onConfirm()
        runCurrent()

        assertTrue(gateway.calls.isEmpty())
        assertEquals(0, keys.generated)
    }

    // Cubre: pay_payment/R1, pay_payment/R5
    @Test
    fun `un pago que ya quedo Pagado no vuelve a abrir la confirmacion aunque la lista diga Pendiente`() =
        runTest(dispatcher) {
            val gateway = gateway(PayResult.Paid)
            val vm = viewModel(gateway)
            vm.pay(payP1)
            runCurrent()

            vm.onPayClick(payP1)
            vm.onConfirm()
            runCurrent()

            assertNull(vm.uiState.value.confirming)
            assertEquals(1, gateway.calls.size)
        }

    // ---------- R2 ----------

    // Cubre: pay_payment/R2
    @Test
    fun `onCancel cierra la confirmacion sin llamar al servidor ni generar ni guardar llave`() = runTest(dispatcher) {
        val gateway = gateway(PayResult.Paid)
        val vm = viewModel(gateway)

        vm.onPayClick(payP1)
        vm.onCancel()
        runCurrent()

        assertNull(vm.uiState.value.confirming)
        assertTrue(gateway.calls.isEmpty())
        assertEquals(0, keys.generated)
        assertTrue(store.all().isEmpty())
        assertTrue(vm.uiState.value.payStates.isEmpty())
    }

    // Cubre: pay_payment/R2
    @Test
    fun `onConfirm sin confirmacion abierta no llama al servidor`() = runTest(dispatcher) {
        val gateway = gateway(PayResult.Paid)
        val vm = viewModel(gateway)

        vm.onConfirm()
        vm.onPayClick(payP1)
        vm.onCancel()
        vm.onConfirm()
        runCurrent()

        assertTrue(gateway.calls.isEmpty())
    }

    // ---------- R3 ----------

    // Cubre: pay_payment/R3
    @Test
    fun `onConfirm genera una llave nueva, llama a pay una vez con ella y muestra Procesando`() = runTest(dispatcher) {
        val gateway = gateway(PayResult.Paid).apply { gate = CompletableDeferred() }
        val vm = viewModel(gateway)

        vm.pay(payP1)
        runCurrent()

        assertEquals(listOf("p1" to "k1"), gateway.calls)
        assertEquals(1, keys.generated)
        assertEquals(PayState.Processing, vm.stateOf("p1"))
        assertNull("el diálogo se cierra al confirmar", vm.uiState.value.confirming)
    }

    // Cubre: pay_payment/R3
    @Test
    fun `cada pago confirmado usa su propia llave nueva`() = runTest(dispatcher) {
        val gateway = gateway(PayResult.Paid)
        val vm = viewModel(gateway)

        vm.pay(payP1)
        runCurrent()
        vm.pay(payP2)
        runCurrent()

        assertEquals(listOf("p1", "p2"), gateway.calls.map { it.first })
        assertNotEquals(gateway.calls[0].second, gateway.calls[1].second)
    }

    // ---------- R4 ----------

    // Cubre: pay_payment/R4
    @Test
    fun `doble confirm y doble tap mientras Procesa no llaman otra vez`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val gateway = gateway(PayResult.Paid).apply { this.gate = gate }
        val vm = viewModel(gateway)

        vm.onPayClick(payP1)
        vm.onConfirm()
        vm.onConfirm()
        runCurrent()
        vm.onPayClick(payP1)
        vm.onConfirm()
        runCurrent()

        assertEquals(1, gateway.calls.size)
        assertEquals(1, keys.generated)
        assertNull(vm.uiState.value.confirming)

        gate.complete(Unit)
        runCurrent()
        assertEquals(1, gateway.calls.size)
    }

    // Cubre: pay_payment/R4, pay_payment/R1
    @Test
    fun `pagar un pago en Verificando no abre confirmacion ni llama otra vez`() = runTest(dispatcher) {
        val gateway = gateway(PayResult.Unknown)
        val vm = viewModel(gateway)
        vm.pay(payP1)
        runCurrent()
        assertVerifying(vm.stateOf("p1"))

        vm.onPayClick(payP1)
        assertNull(vm.uiState.value.confirming)
        vm.onConfirm()
        runCurrent()

        assertEquals(1, gateway.calls.size)
        assertEquals(1, keys.generated)
    }

    // ---------- R5 ----------

    // Cubre: pay_payment/R5
    @Test
    fun `pago confirmado queda Pagado con el aviso Pago registrado`() = runTest(dispatcher) {
        val vm = viewModel(gateway(PayResult.Paid))

        vm.pay(payP1)
        runCurrent()

        assertEquals(PayState.Paid, vm.stateOf("p1"))
        assertEquals(PayMessage.Paid, vm.uiState.value.message)
    }

    // Cubre: pay_payment/R5
    @Test
    fun `onMessageShown limpia el aviso y conserva el estado Pagado`() = runTest(dispatcher) {
        val vm = viewModel(gateway(PayResult.Paid))
        vm.pay(payP1)
        runCurrent()

        vm.onMessageShown()

        assertNull(vm.uiState.value.message)
        assertEquals(PayState.Paid, vm.stateOf("p1"))
    }

    // ---------- R6 ----------

    // Cubre: pay_payment/R6
    @Test
    fun `resultado desconocido deja el pago en Verificando sin aviso`() = runTest(dispatcher) {
        val vm = viewModel(gateway(PayResult.Unknown))

        vm.pay(payP1)
        runCurrent()

        val state = vm.stateOf("p1")
        assertVerifying(state)
        assertTrue("aún quedan reintentos", (state as PayState.Verifying).attemptsLeft > 0)
        assertNull(vm.uiState.value.message)
    }

    // ---------- R7 ----------

    // Cubre: pay_payment/R7
    @Test
    fun `en Verificando reintenta cada 3 s con la misma llave hasta 3 veces`() = runTest(dispatcher) {
        val gateway = gateway(PayResult.Unknown)
        val vm = viewModel(gateway)
        vm.pay(payP1)
        runCurrent()
        assertEquals(1, gateway.calls.size)

        advance(VERIFY_INTERVAL_MS - 1)
        assertEquals("no reintenta antes de 3 s", 1, gateway.calls.size)
        advance(1)
        assertEquals(2, gateway.calls.size)
        advance(VERIFY_INTERVAL_MS)
        assertEquals(3, gateway.calls.size)
        advance(VERIFY_INTERVAL_MS)
        assertEquals(4, gateway.calls.size)
        advance(10 * VERIFY_INTERVAL_MS)
        assertEquals("solo 3 reintentos", 1 + VERIFY_ATTEMPTS, gateway.calls.size)

        assertEquals(List(1 + VERIFY_ATTEMPTS) { "p1" to "k1" }, gateway.calls)
        assertEquals(1, keys.generated)
    }

    // Cubre: pay_payment/R7, pay_payment/R5
    @Test
    fun `si un reintento confirma queda Pagado y deja de reintentar`() = runTest(dispatcher) {
        val gateway = gateway(PayResult.Unknown, PayResult.Unknown, PayResult.Paid)
        val vm = viewModel(gateway)
        vm.pay(payP1)
        runCurrent()

        advance(2 * VERIFY_INTERVAL_MS)

        assertEquals(3, gateway.calls.size)
        assertEquals(PayState.Paid, vm.stateOf("p1"))
        assertEquals(PayMessage.Paid, vm.uiState.value.message)

        advance(10 * VERIFY_INTERVAL_MS)
        assertEquals(3, gateway.calls.size)
    }

    // ---------- R8 ----------

    // Cubre: pay_payment/R8
    @Test
    fun `agotados los reintentos sigue en Verificando con attemptsLeft 0`() = runTest(dispatcher) {
        val gateway = gateway(PayResult.Unknown)
        val vm = viewModel(gateway)
        vm.pay(payP1)
        runCurrent()

        advance(VERIFY_ATTEMPTS * VERIFY_INTERVAL_MS)

        assertEquals(PayState.Verifying(attemptsLeft = 0), vm.stateOf("p1"))
        assertEquals(mapOf("p1" to "k1"), store.all())
        assertNull(vm.uiState.value.message)
    }

    // Cubre: pay_payment/R8, pay_payment/R5
    @Test
    fun `onVerifyAgain reintenta con la misma llave y si confirma queda Pagado`() = runTest(dispatcher) {
        val unknowns = Array<PayResult>(1 + VERIFY_ATTEMPTS) { PayResult.Unknown }
        val gateway = gateway(*unknowns, PayResult.Paid)
        val vm = viewModel(gateway)
        vm.pay(payP1)
        runCurrent()
        advance(VERIFY_ATTEMPTS * VERIFY_INTERVAL_MS)
        assertEquals(PayState.Verifying(0), vm.stateOf("p1"))

        vm.onVerifyAgain("p1")
        runCurrent()

        assertEquals(2 + VERIFY_ATTEMPTS, gateway.calls.size)
        assertEquals("p1" to "k1", gateway.calls.last())
        assertEquals(1, keys.generated)
        assertEquals(PayState.Paid, vm.stateOf("p1"))
    }

    // ---------- R9 ----------

    // Cubre: pay_payment/R9
    @Test
    fun `rechazo avisa, descarta la llave y vuelve a Listo`() = runTest(dispatcher) {
        val gateway = gateway(PayResult.Declined, PayResult.Paid)
        val vm = viewModel(gateway)
        vm.pay(payP1)
        runCurrent()

        assertNull("vuelve a Listo", vm.stateOf("p1"))
        assertEquals(PayMessage.Declined, vm.uiState.value.message)
        assertTrue(store.all().isEmpty())

        vm.onMessageShown()
        vm.onPayClick(payP1)
        assertEquals("se puede volver a pagar", payP1, vm.uiState.value.confirming)
        vm.onConfirm()
        runCurrent()

        assertEquals(2, gateway.calls.size)
        assertNotEquals("la llave rechazada no se reutiliza", gateway.calls[0].second, gateway.calls[1].second)
    }

    // Cubre: pay_payment/R9, pay_payment/R7
    @Test
    fun `rechazo en un reintento tambien descarta la llave y deja de reintentar`() = runTest(dispatcher) {
        val gateway = gateway(PayResult.Unknown, PayResult.Declined)
        val vm = viewModel(gateway)
        vm.pay(payP1)
        runCurrent()

        advance(VERIFY_INTERVAL_MS)

        assertNull(vm.stateOf("p1"))
        assertEquals(PayMessage.Declined, vm.uiState.value.message)
        assertTrue(store.all().isEmpty())
        advance(10 * VERIFY_INTERVAL_MS)
        assertEquals(2, gateway.calls.size)
    }

    // ---------- R10 ----------

    // Cubre: pay_payment/R10, pay_payment/R5
    @Test
    fun `recarga que trae PAGADO un pago en Verificando lo da por pagado`() = runTest(dispatcher) {
        val gateway = gateway(PayResult.Unknown)
        val vm = viewModel(gateway)
        vm.pay(payP1)
        runCurrent()
        assertVerifying(vm.stateOf("p1"))

        vm.onPaymentsLoaded(listOf(payP3, payP2, payP1.copy(status = PaymentStatus.Paid), payP4))
        runCurrent()

        assertEquals(PayState.Paid, vm.stateOf("p1"))
        assertEquals(PayMessage.Paid, vm.uiState.value.message)
        assertTrue("resultado definitivo: se borra la llave", store.all().isEmpty())
        advance(10 * VERIFY_INTERVAL_MS)
        assertEquals("ya no reintenta", 1, gateway.calls.size)
    }

    // Cubre: pay_payment/R10
    @Test
    fun `recarga con el pago aun Pendiente no borra Verificando`() = runTest(dispatcher) {
        val vm = viewModel(gateway(PayResult.Unknown))
        vm.pay(payP1)
        runCurrent()
        val before = vm.stateOf("p1")

        vm.onPaymentsLoaded(payList)
        runCurrent()

        assertVerifying(vm.stateOf("p1"))
        assertEquals(before, vm.stateOf("p1"))
        assertEquals(mapOf("p1" to "k1"), store.all())
    }

    // Cubre: pay_payment/R10
    @Test
    fun `recarga con el pago aun Pendiente no borra Procesando`() = runTest(dispatcher) {
        val gateway = gateway(PayResult.Paid).apply { gate = CompletableDeferred() }
        val vm = viewModel(gateway)
        vm.pay(payP1)
        runCurrent()

        vm.onPaymentsLoaded(payList)
        runCurrent()

        assertEquals(PayState.Processing, vm.stateOf("p1"))
    }

    // Cubre: pay_payment/R10
    @Test
    fun `recarga no borra un pago ya Pagado aunque la lista aun diga Pendiente`() = runTest(dispatcher) {
        val vm = viewModel(gateway(PayResult.Paid))
        vm.pay(payP1)
        runCurrent()

        vm.onPaymentsLoaded(payList)
        runCurrent()

        assertEquals(PayState.Paid, vm.stateOf("p1"))
    }

    // ---------- R14 ----------

    // Cubre: pay_payment/R14
    @Test
    fun `guarda la llave en el store antes de llamar a pay`() = runTest(dispatcher) {
        val gateway = gateway(PayResult.Unknown)
        val vm = viewModel(gateway)

        vm.pay(payP1)
        runCurrent()

        val save = events.indexOf("save:p1:k1")
        val pay = events.indexOf("pay:p1:k1")
        assertTrue("falta save en $events", save >= 0)
        assertTrue("falta pay en $events", pay >= 0)
        assertTrue("save debe ir antes de pay: $events", save < pay)
        assertEquals(mapOf("p1" to "k1"), gateway.storeAtCall.first())
    }

    // Cubre: pay_payment/R14, pay_payment/R6
    @Test
    fun `resultado desconocido no borra la llave guardada`() = runTest(dispatcher) {
        val vm = viewModel(gateway(PayResult.Unknown))

        vm.pay(payP1)
        runCurrent()
        advance(VERIFY_INTERVAL_MS)

        assertEquals(mapOf("p1" to "k1"), store.all())
        assertTrue(events.none { it.startsWith("remove:") })
    }

    // Cubre: pay_payment/R14, pay_payment/R5
    @Test
    fun `Pagado borra la llave despues de llamar a pay`() = runTest(dispatcher) {
        val vm = viewModel(gateway(PayResult.Paid))

        vm.pay(payP1)
        runCurrent()

        assertTrue(store.all().isEmpty())
        assertTrue("remove después de pay: $events", events.indexOf("remove:p1") > events.indexOf("pay:p1:k1"))
    }

    // Cubre: pay_payment/R14, pay_payment/R9
    @Test
    fun `Rechazado borra la llave despues de llamar a pay`() = runTest(dispatcher) {
        val vm = viewModel(gateway(PayResult.Declined))

        vm.pay(payP1)
        runCurrent()

        assertTrue(store.all().isEmpty())
        assertTrue("remove después de pay: $events", events.indexOf("remove:p1") > events.indexOf("pay:p1:k1"))
    }

    // ---------- R15 ----------

    // Cubre: pay_payment/R15, pay_payment/R7
    @Test
    fun `llave guardada al crear el VM pone el pago en Verificando y reintenta con esa llave`() = runTest(dispatcher) {
        store.save("p2", "llave-previa")
        val gateway = gateway(PayResult.Unknown)

        val vm = viewModel(gateway)
        runCurrent()
        assertVerifying(vm.stateOf("p2"))

        advance(VERIFY_ATTEMPTS * VERIFY_INTERVAL_MS)

        assertTrue("debe reintentar", gateway.calls.isNotEmpty())
        assertTrue(
            "siempre con la llave guardada: ${gateway.calls}",
            gateway.calls.all {
                it ==
                    "p2" to "llave-previa"
            }
        )
        assertEquals("no genera llaves nuevas", 0, keys.generated)
        assertVerifying(vm.stateOf("p2"))
        assertEquals(mapOf("p2" to "llave-previa"), store.all())
    }

    // Cubre: pay_payment/R15, pay_payment/R5, pay_payment/R14
    @Test
    fun `llave guardada que el servidor confirma queda Pagado y se borra`() = runTest(dispatcher) {
        store.save("p1", "llave-previa")
        val gateway = gateway(PayResult.Paid)

        val vm = viewModel(gateway)
        runCurrent()
        advance(VERIFY_INTERVAL_MS)

        assertEquals(listOf("p1" to "llave-previa"), gateway.calls)
        assertEquals(PayState.Paid, vm.stateOf("p1"))
        assertEquals(PayMessage.Paid, vm.uiState.value.message)
        assertTrue(store.all().isEmpty())
    }

    // Cubre: pay_payment/R15, pay_payment/R4
    @Test
    fun `pago con llave guardada no abre confirmacion`() = runTest(dispatcher) {
        store.save("p1", "llave-previa")
        val gateway = gateway(PayResult.Unknown)
        val vm = viewModel(gateway)
        runCurrent()

        vm.onPayClick(payP1)

        assertNull(vm.uiState.value.confirming)
        assertEquals(0, keys.generated)
    }
}
