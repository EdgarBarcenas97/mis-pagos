package dev.base.app.feature.payments.presentation

import dev.base.app.core.network.Network
import dev.base.app.feature.payments.data.FakeBackendInterceptor
import dev.base.app.feature.payments.data.FakePaymentsServer
import dev.base.app.feature.payments.data.PaymentGatewayImpl
import dev.base.app.feature.payments.data.PaymentsApi
import dev.base.app.feature.payments.domain.IdempotencyKeyFactory
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Random guionizado por petición: [ScriptInterceptor] fija [value] antes de que cada POST pay
 * llegue a [FakeBackendInterceptor], así todos los sorteos de esa petición devuelven lo mismo
 * sin depender del orden en que el interceptor los haga.
 */
private class PerRequestRandom : Random() {
    private val bits = Random(11)

    @Volatile var value: Double = 0.9

    override fun nextBits(bitCount: Int): Int = synchronized(bits) { bits.nextBits(bitCount) }

    override fun nextDouble(): Double = value
}

private class ScriptInterceptor(private val random: PerRequestRandom, private val script: List<Double>) :
    Interceptor {
    val payKeys = CopyOnWriteArrayList<String>()

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        if (request.method == "POST") {
            payKeys += request.header("Idempotency-Key").orEmpty()
            random.value = script.getOrElse(payKeys.size - 1) { script.last() }
        }
        return chain.proceed(request)
    }
}

/** ViewModel → PaymentGatewayImpl → Retrofit → FakeBackendInterceptor → FakePaymentsServer. */
@OptIn(ExperimentalCoroutinesApi::class)
class PayEndToEndTest {
    private val dispatcher = StandardTestDispatcher()
    private val server = FakePaymentsServer()
    private val store = InMemoryPendingPaymentStore()
    private val random = PerRequestRandom()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** Con azar 0.1 una petición es timeout (0.1 < 0.5); con 0.9 es éxito. Nunca rechazo. */
    private fun viewModel(
        script: List<Double>,
        registeredOnTimeoutRate: Double,
        key: String = "llave-e2e"
    ): Pair<PayViewModel, ScriptInterceptor> {
        val scriptInterceptor = ScriptInterceptor(random, script)
        val client = OkHttpClient.Builder()
            .addInterceptor(scriptInterceptor)
            .addInterceptor(
                FakeBackendInterceptor(
                    random = random,
                    latencyMs = 0,
                    failureRate = 0.0,
                    sleeper = {},
                    server = server,
                    payLatencyMs = 0L..0L,
                    payTimeoutRate = 0.5,
                    registeredOnTimeoutRate = registeredOnTimeoutRate,
                    payDeclineRate = 0.0
                )
            )
            .build()
        val api = Network.retrofit("https://api.mispagos.fake/", client).create(PaymentsApi::class.java)
        val vm = PayViewModel(
            PaymentGatewayImpl(api),
            IdempotencyKeyFactory { key },
            store,
            PayTestDispatchers(dispatcher)
        )
        return vm to scriptInterceptor
    }

    /** OkHttp responde en otro hilo: avanza el reloj virtual y espera en tiempo real hasta el estado final. */
    private fun TestScope.awaitPaid(vm: PayViewModel, id: String) {
        val deadline = System.currentTimeMillis() + 10_000
        while (vm.uiState.value.payStates[id] != PayState.Paid) {
            check(System.currentTimeMillis() < deadline) { "no llegó a Paid: ${vm.uiState.value}" }
            Thread.sleep(10)
            advanceTimeBy(100)
            runCurrent()
        }
    }

    // Cubre: pay_payment/R12, pay_payment/R6, pay_payment/R7, pay_payment/R11
    @Test
    fun `timeout que si registra y luego exito termina en Pagado con exactamente un cargo`() = runTest(dispatcher) {
        val (vm, script) = viewModel(script = listOf(0.1, 0.9), registeredOnTimeoutRate = 1.0)

        vm.onPayClick(payP1)
        vm.onConfirm()
        awaitPaid(vm, "p1")

        assertEquals(1, server.chargeCount("p1"))
        assertTrue(server.isPaid("p1"))
        assertEquals("primer intento + un reintento", 2, script.payKeys.size)
        assertEquals("misma llave en el reintento", listOf("llave-e2e", "llave-e2e"), script.payKeys.toList())
        assertEquals(PayMessage.Paid, vm.uiState.value.message)
        assertTrue(store.all().isEmpty())
    }

    // Cubre: pay_payment/R12, pay_payment/R7, pay_payment/R11
    @Test
    fun `timeout que no registra y luego exito tambien deja exactamente un cargo`() = runTest(dispatcher) {
        val (vm, script) = viewModel(script = listOf(0.1, 0.9), registeredOnTimeoutRate = 0.0)

        vm.onPayClick(payP1)
        vm.onConfirm()
        awaitPaid(vm, "p1")

        assertEquals(1, server.chargeCount("p1"))
        assertEquals(2, script.payKeys.size)
    }

    // Cubre: pay_payment/R12, pay_payment/R7, pay_payment/R11
    @Test
    fun `varios timeouts que registran seguidos de exito dejan un solo cargo`() = runTest(dispatcher) {
        val (vm, script) = viewModel(script = listOf(0.1, 0.1, 0.1, 0.9), registeredOnTimeoutRate = 1.0)

        vm.onPayClick(payP2)
        vm.onConfirm()
        awaitPaid(vm, "p2")

        assertEquals(1, server.chargeCount("p2"))
        assertEquals(4, script.payKeys.size)
        assertTrue(script.payKeys.all { it == "llave-e2e" })
    }

    // Cubre: pay_payment/R15, pay_payment/R12, pay_payment/R14
    @Test
    fun `la app se cerro tras un timeout que cobro y al reabrir verifica con la llave guardada sin cobrar dos veces`() =
        runTest(dispatcher) {
            server.registerPayment("p1", "llave-vieja")
            store.save("p1", "llave-vieja")

            val (vm, script) = viewModel(script = listOf(0.9), registeredOnTimeoutRate = 1.0, key = "llave-nueva")
            awaitPaid(vm, "p1")

            assertEquals(1, server.chargeCount("p1"))
            assertTrue(script.payKeys.all { it == "llave-vieja" })
            assertTrue(store.all().isEmpty())
        }
}
