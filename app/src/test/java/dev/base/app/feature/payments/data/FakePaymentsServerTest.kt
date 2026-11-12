package dev.base.app.feature.payments.data

import dev.base.app.core.network.Network
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakePaymentsServerTest {
    private val server = FakePaymentsServer()

    private fun statuses(): Map<String, String> =
        Network.json.decodeFromString<LoanDto>(server.paymentsJson()).payments.associate { it.id to it.status }

    // Cubre: pay_payment/R11
    @Test
    fun `sin pagos registrados paymentsJson es exactamente SAMPLE_JSON`() {
        assertEquals(FakeBackendInterceptor.SAMPLE_JSON, server.paymentsJson())
    }

    // Cubre: pay_payment/R11
    @Test
    fun `sin pagos registrados devuelve exactamente el JSON inicial inyectado`() {
        val json = """{"loanId":"X","payments":[]}"""

        assertEquals(json, FakePaymentsServer(json).paymentsJson())
    }

    // Cubre: pay_payment/R11
    @Test
    fun `al inicio nada esta cobrado y solo esta pagado lo que el JSON dice PAGADO`() {
        assertFalse(server.isPaid("p1"))
        assertFalse(server.isPaid("p2"))
        assertTrue(server.isPaid("p3"))
        assertEquals(0, server.chargeCount("p1"))
        assertEquals(0, server.chargeCount("p3"))
    }

    // Cubre: pay_payment/R11, pay_payment/R10
    @Test
    fun `registrar un pago lo marca pagado con un cargo y paymentsJson lo refleja como PAGADO`() {
        server.registerPayment("p1", "k1")

        assertTrue(server.isPaid("p1"))
        assertEquals(1, server.chargeCount("p1"))
        assertEquals(
            mapOf("p1" to "PAGADO", "p2" to "VENCIDO", "p3" to "PAGADO", "p4" to "EN_PROCESO"),
            statuses()
        )
    }

    // Cubre: pay_payment/R11
    @Test
    fun `la misma llave dos veces no genera un segundo cargo`() {
        server.registerPayment("p1", "k1")
        server.registerPayment("p1", "k1")

        assertEquals(1, server.chargeCount("p1"))
    }

    // Cubre: pay_payment/R11
    @Test
    fun `un pago ya pagado con otra llave no genera un segundo cargo`() {
        server.registerPayment("p2", "k1")
        server.registerPayment("p2", "k2")
        server.registerPayment("p2", "k3")

        assertEquals(1, server.chargeCount("p2"))
        assertTrue(server.isPaid("p2"))
    }

    // Cubre: pay_payment/R11
    @Test
    fun `un pago que ya venia PAGADO en el JSON nunca genera cargo`() {
        server.registerPayment("p3", "k1")

        assertEquals(0, server.chargeCount("p3"))
        assertTrue(server.isPaid("p3"))
    }

    // Cubre: pay_payment/R11
    @Test
    fun `pagar uno no afecta a los demas`() {
        server.registerPayment("p1", "k1")

        assertFalse(server.isPaid("p2"))
        assertEquals(0, server.chargeCount("p2"))
    }

    // Cubre: pay_payment/R11
    @Test
    fun `registros concurrentes con distintas llaves dejan un solo cargo`() {
        val threads = (1..16).map { i -> Thread { server.registerPayment("p1", "k$i") } }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(1, server.chargeCount("p1"))
    }
}
