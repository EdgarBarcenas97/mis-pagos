package dev.base.app.feature.payments.data

import dev.base.app.core.network.Network
import dev.base.app.feature.payments.domain.PayResult
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PaymentGatewayImplTest {
    private val server = MockWebServer()
    private lateinit var gateway: PaymentGatewayImpl

    @Before
    fun setUp() {
        server.start()
        val client = OkHttpClient.Builder().readTimeout(300, TimeUnit.MILLISECONDS).build()
        val api = Network.retrofit(server.url("/").toString(), client).create(PaymentsApi::class.java)
        gateway = PaymentGatewayImpl(api)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun paidBody(id: String = "p1") = MockResponse().setBody("""{"paymentId":"$id","status":"PAGADO"}""")

    // Cubre: pay_payment/R3, pay_payment/R5
    @Test
    fun `200 PAGADO es Paid`() = runTest {
        server.enqueue(paidBody())

        assertEquals(PayResult.Paid, gateway.pay("p1", "k1"))
    }

    // Cubre: pay_payment/R3, pay_payment/R7
    @Test
    fun `la peticion es POST a loans payments id pay con el header Idempotency-Key`() = runTest {
        server.enqueue(paidBody("p2"))

        gateway.pay("p2", "llave-123")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/loans/payments/p2/pay", request.path)
        assertEquals("llave-123", request.getHeader("Idempotency-Key"))
    }

    // Cubre: pay_payment/R9
    @Test
    fun `422 es Declined`() = runTest {
        server.enqueue(MockResponse().setResponseCode(422))

        assertEquals(PayResult.Declined, gateway.pay("p1", "k1"))
    }

    // Cubre: pay_payment/R9
    @Test
    fun `400 y 409 son Declined`() = runTest {
        server.enqueue(MockResponse().setResponseCode(400))
        server.enqueue(MockResponse().setResponseCode(409))

        assertEquals(PayResult.Declined, gateway.pay("p1", "k1"))
        assertEquals(PayResult.Declined, gateway.pay("p1", "k1"))
    }

    // Cubre: pay_payment/R9
    @Test
    fun `404 es Declined`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))

        assertEquals(PayResult.Declined, gateway.pay("p1", "k1"))
    }

    // Cubre: pay_payment/R6
    @Test
    fun `503 y 500 son Unknown`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setResponseCode(500))

        assertEquals(PayResult.Unknown, gateway.pay("p1", "k1"))
        assertEquals(PayResult.Unknown, gateway.pay("p1", "k1"))
    }

    // Cubre: pay_payment/R6
    @Test
    fun `desconexion es Unknown`() = runTest {
        // Varias por si OkHttp reintenta la conexión: la cola vacía bloquearía el test.
        repeat(3) { server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)) }

        assertEquals(PayResult.Unknown, gateway.pay("p1", "k1"))
    }

    // Cubre: pay_payment/R6
    @Test
    fun `timeout de lectura es Unknown`() = runTest {
        repeat(3) { server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)) }

        assertEquals(PayResult.Unknown, gateway.pay("p1", "k1"))
    }

    // Cubre: pay_payment/R6
    @Test
    fun `200 con un estado distinto de PAGADO es Unknown`() = runTest {
        server.enqueue(MockResponse().setBody("""{"paymentId":"p1","status":"EN_PROCESO"}"""))

        assertEquals(PayResult.Unknown, gateway.pay("p1", "k1"))
    }

    // Cubre: pay_payment/R6
    @Test
    fun `200 con JSON invalido es Unknown`() = runTest {
        server.enqueue(MockResponse().setBody("no-es-json"))

        assertEquals(PayResult.Unknown, gateway.pay("p1", "k1"))
    }
}
