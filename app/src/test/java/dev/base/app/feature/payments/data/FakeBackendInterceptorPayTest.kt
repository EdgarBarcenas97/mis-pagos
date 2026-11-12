package dev.base.app.feature.payments.data

import dev.base.app.core.network.Network
import java.net.SocketTimeoutException
import kotlin.random.Random
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Random de test: `nextDouble()` siempre devuelve [value] (las tasas 0.0/1.0 deciden igual sin importar
 * en qué orden se sorteen) y los bits salen de una secuencia fija con [seed].
 */
private class ScriptedPayRandom(private val value: Double, seed: Int = 7) : Random() {
    private val bits = Random(seed)

    override fun nextBits(bitCount: Int): Int = bits.nextBits(bitCount)

    override fun nextDouble(): Double = value
}

class FakeBackendInterceptorPayTest {
    private val sleeps = mutableListOf<Long>()
    private val server = FakePaymentsServer()

    private fun interceptor(
        random: Random = ScriptedPayRandom(0.5),
        payLatencyMs: LongRange = 0L..0L,
        payTimeoutRate: Double = 0.0,
        registeredOnTimeoutRate: Double = 0.0,
        payDeclineRate: Double = 0.0
    ) = FakeBackendInterceptor(
        random = random,
        failureRate = 0.0,
        sleeper = { sleeps += it },
        server = server,
        payLatencyMs = payLatencyMs,
        payTimeoutRate = payTimeoutRate,
        registeredOnTimeoutRate = registeredOnTimeoutRate,
        payDeclineRate = payDeclineRate
    )

    private fun pay(interceptor: FakeBackendInterceptor, id: String = "p1", key: String = "k1"): Pair<Int, String> {
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val request = Request.Builder()
            .url("https://api.mispagos.fake/loans/payments/$id/pay")
            .header("Idempotency-Key", key)
            .post("".toRequestBody(null))
            .build()
        return client.newCall(request).execute().use { it.code to it.body!!.string() }
    }

    private fun get(interceptor: FakeBackendInterceptor): String {
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val request = Request.Builder().url("https://api.mispagos.fake/loans/payments").build()
        return client.newCall(request).execute().use { it.body!!.string() }
    }

    private fun assertPaidBody(id: String, body: String) {
        val dto = Network.json.decodeFromString<PayResponseDto>(body)
        assertEquals(PayResponseDto(id, "PAGADO"), dto)
    }

    // Cubre: pay_payment/R11
    @Test
    fun `pay duerme una vez un valor dentro de 2 a 5 s por defecto`() {
        (1..20).forEach { seed ->
            sleeps.clear()
            val fresh = FakeBackendInterceptor(
                random = ScriptedPayRandom(0.99, seed),
                failureRate = 0.0,
                sleeper = { sleeps += it },
                server = FakePaymentsServer(),
                payLatencyMs = 2_000L..5_000L,
                payTimeoutRate = 0.0
            )
            pay(fresh)

            assertEquals("una sola espera: $sleeps", 1, sleeps.size)
            assertTrue("espera ${sleeps.single()} fuera de 2000..5000", sleeps.single() in 2_000L..5_000L)
        }
    }

    // Cubre: pay_payment/R11
    @Test
    fun `la latencia por defecto de pay es 2 a 5 s`() {
        val defaults = FakeBackendInterceptor(
            random = ScriptedPayRandom(0.99),
            failureRate = 0.0,
            sleeper = { sleeps += it },
            server = server
        )

        pay(defaults)

        assertTrue("espera $sleeps fuera de 2000..5000", sleeps.single() in 2_000L..5_000L)
    }

    // Cubre: pay_payment/R11
    @Test
    fun `la latencia de pay es inyectable`() {
        pay(interceptor(payLatencyMs = 123L..123L))

        assertEquals(listOf(123L), sleeps)
    }

    // Cubre: pay_payment/R11
    @Test
    fun `pay sin timeout ni rechazo responde 200 PAGADO y registra un cargo`() {
        val (code, body) = pay(interceptor())

        assertEquals(200, code)
        assertPaidBody("p1", body)
        assertTrue(server.isPaid("p1"))
        assertEquals(1, server.chargeCount("p1"))
    }

    // Cubre: pay_payment/R11
    @Test
    fun `pay toma el id del path`() {
        val (code, body) = pay(interceptor(), id = "p2")

        assertEquals(200, code)
        assertPaidBody("p2", body)
        assertTrue(server.isPaid("p2"))
        assertFalse(server.isPaid("p1"))
    }

    // Cubre: pay_payment/R11, pay_payment/R9
    @Test
    fun `payDeclineRate 1 responde 422 sin registrar`() {
        val (code, _) = pay(interceptor(payDeclineRate = 1.0))

        assertEquals(422, code)
        assertFalse(server.isPaid("p1"))
        assertEquals(0, server.chargeCount("p1"))
    }

    // Cubre: pay_payment/R11
    @Test
    fun `timeout que registra lanza SocketTimeoutException y deja el cargo hecho`() {
        val interceptor = interceptor(payTimeoutRate = 1.0, registeredOnTimeoutRate = 1.0)

        assertThrows(SocketTimeoutException::class.java) { pay(interceptor) }

        assertTrue(server.isPaid("p1"))
        assertEquals(1, server.chargeCount("p1"))
    }

    // Cubre: pay_payment/R11
    @Test
    fun `timeout que no registra lanza SocketTimeoutException sin cargo`() {
        val interceptor = interceptor(payTimeoutRate = 1.0, registeredOnTimeoutRate = 0.0)

        assertThrows(SocketTimeoutException::class.java) { pay(interceptor) }

        assertFalse(server.isPaid("p1"))
        assertEquals(0, server.chargeCount("p1"))
    }

    // Cubre: pay_payment/R11
    @Test
    fun `timeout tambien espera la latencia antes de fallar`() {
        val interceptor = interceptor(payLatencyMs = 77L..77L, payTimeoutRate = 1.0)

        assertThrows(SocketTimeoutException::class.java) { pay(interceptor) }

        assertEquals(listOf(77L), sleeps)
    }

    // Cubre: pay_payment/R11
    @Test
    fun `con las tasas por defecto un azar alto responde 200`() {
        val defaults = FakeBackendInterceptor(
            random = ScriptedPayRandom(0.99),
            failureRate = 0.0,
            sleeper = {},
            server = server
        )

        assertEquals(200, pay(defaults).first)
    }

    // Cubre: pay_payment/R11
    @Test
    fun `con las tasas por defecto un azar bajo es timeout que registra`() {
        val defaults = FakeBackendInterceptor(
            random = ScriptedPayRandom(0.01),
            failureRate = 0.0,
            sleeper = {},
            server = server
        )

        assertThrows(SocketTimeoutException::class.java) { pay(defaults) }
        assertEquals(1, server.chargeCount("p1"))
    }

    // Cubre: pay_payment/R11, pay_payment/R7
    @Test
    fun `reintento con la misma llave tras un timeout que registro responde 200 sin nuevo cargo`() {
        assertThrows(SocketTimeoutException::class.java) {
            pay(interceptor(payTimeoutRate = 1.0, registeredOnTimeoutRate = 1.0), key = "k1")
        }

        val (code, body) = pay(interceptor(), key = "k1")

        assertEquals(200, code)
        assertPaidBody("p1", body)
        assertEquals(1, server.chargeCount("p1"))
    }

    // Cubre: pay_payment/R11
    @Test
    fun `pago ya pagado con otra llave responde 200 PAGADO sin nuevo cargo`() {
        server.registerPayment("p1", "k0")

        val (code, body) = pay(interceptor(), key = "k1")

        assertEquals(200, code)
        assertPaidBody("p1", body)
        assertEquals(1, server.chargeCount("p1"))
    }

    // Cubre: pay_payment/R10, pay_payment/R11
    @Test
    fun `GET sin body inyectado refleja los pagos registrados por pay`() {
        val interceptor = interceptor()
        pay(interceptor)

        val loan = Network.json.decodeFromString<LoanDto>(get(interceptor))

        assertEquals("PAGADO", loan.payments.first { it.id == "p1" }.status)
        assertEquals("VENCIDO", loan.payments.first { it.id == "p2" }.status)
    }
}
