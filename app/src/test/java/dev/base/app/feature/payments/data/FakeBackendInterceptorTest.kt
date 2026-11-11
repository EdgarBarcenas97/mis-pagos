package dev.base.app.feature.payments.data

import kotlin.random.Random
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Test

/** Random de test: siempre devuelve [value] en nextDouble(). */
private class FixedRandom(private val value: Double) : Random() {
    override fun nextBits(bitCount: Int): Int = 0

    override fun nextDouble(): Double = value
}

class FakeBackendInterceptorTest {
    private val sleeps = mutableListOf<Long>()

    private fun call(interceptor: FakeBackendInterceptor): Pair<Int, String> {
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val request = Request.Builder().url("https://api.mispagos.fake/loans/payments").build()
        return client.newCall(request).execute().use { it.code to it.body!!.string() }
    }

    // Cubre: payments_list/R10
    @Test
    fun `failureRate 1 responde 503`() {
        val (code, _) = call(FakeBackendInterceptor(failureRate = 1.0, sleeper = { sleeps += it }))

        assertEquals(503, code)
    }

    // Cubre: payments_list/R10
    @Test
    fun `failureRate 0 responde 200 con el JSON del enunciado`() {
        val (code, body) = call(FakeBackendInterceptor(failureRate = 0.0, sleeper = { sleeps += it }))

        assertEquals(200, code)
        assertEquals(FakeBackendInterceptor.SAMPLE_JSON, body)
    }

    // Cubre: payments_list/R10
    @Test
    fun `body inyectado se devuelve tal cual`() {
        val (code, body) = call(FakeBackendInterceptor(failureRate = 0.0, body = "{}", sleeper = { sleeps += it }))

        assertEquals(200, code)
        assertEquals("{}", body)
    }

    // Cubre: payments_list/R10
    @Test
    fun `espera 1500 ms por defecto antes de responder con exito`() {
        call(FakeBackendInterceptor(failureRate = 0.0, sleeper = { sleeps += it }))

        assertEquals(listOf(1_500L), sleeps)
    }

    // Cubre: payments_list/R10
    @Test
    fun `tambien espera antes de responder con error`() {
        call(FakeBackendInterceptor(failureRate = 1.0, sleeper = { sleeps += it }))

        assertEquals(listOf(1_500L), sleeps)
    }

    // Cubre: payments_list/R10
    @Test
    fun `latencia inyectada se pasa al sleeper`() {
        call(FakeBackendInterceptor(latencyMs = 42, failureRate = 0.0, sleeper = { sleeps += it }))

        assertEquals(listOf(42L), sleeps)
    }

    // Cubre: payments_list/R10
    @Test
    fun `con la tasa por defecto del 30 por ciento un azar menor a 0_30 falla`() {
        val (code, _) = call(FakeBackendInterceptor(random = FixedRandom(0.29), sleeper = { sleeps += it }))

        assertEquals(503, code)
    }

    // Cubre: payments_list/R10
    @Test
    fun `con la tasa por defecto del 30 por ciento un azar de 0_30 o mas responde 200`() {
        val (atLimit, _) = call(FakeBackendInterceptor(random = FixedRandom(0.30), sleeper = { sleeps += it }))
        val (high, _) = call(FakeBackendInterceptor(random = FixedRandom(0.99), sleeper = { sleeps += it }))

        assertEquals(200, atLimit)
        assertEquals(200, high)
    }
}
