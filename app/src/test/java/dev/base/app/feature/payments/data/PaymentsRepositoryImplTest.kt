package dev.base.app.feature.payments.data

import dev.base.app.core.common.AppError
import dev.base.app.core.common.AppResult
import dev.base.app.core.network.Network
import dev.base.app.feature.payments.domain.Loan
import dev.base.app.feature.payments.domain.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PaymentsRepositoryImplTest {
    private val server = MockWebServer()
    private lateinit var repository: PaymentsRepositoryImpl

    @Before
    fun setUp() {
        server.start()
        val api = Network.retrofit(server.url("/").toString()).create(PaymentsApi::class.java)
        repository = PaymentsRepositoryImpl(api)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun AppResult<Loan>.loan(): Loan {
        assertTrue("se esperaba Success y fue $this", this is AppResult.Success)
        return (this as AppResult.Success).data
    }

    // Cubre: payments_list/R5
    @Test
    fun `200 devuelve el credito con los pagos ordenados por fecha limite ascendente`() = runTest {
        server.enqueue(MockResponse().setBody(FakeBackendInterceptor.SAMPLE_JSON))

        val loan = repository.getPayments().loan()

        assertEquals("CR-88213", loan.loanId)
        assertEquals(listOf("p3", "p2", "p1", "p4"), loan.payments.map { it.id })
        assertEquals("/loans/payments", server.takeRequest().path)
    }

    // Cubre: payments_list/R5, payments_list/R6, payments_list/R7, payments_list/R8
    @Test
    fun `200 mapea monto, fecha y estado de cada pago`() = runTest {
        server.enqueue(MockResponse().setBody(FakeBackendInterceptor.SAMPLE_JSON))

        val first = repository.getPayments().loan().payments.first()

        assertEquals("p3", first.id)
        assertEquals(BigDecimal("1250.10"), first.amount)
        assertEquals(LocalDate.of(2026, 9, 30), first.dueDate)
        assertEquals(PaymentStatus.Paid, first.status)
    }

    // Cubre: payments_list/R4
    @Test
    fun `200 sin pagos devuelve Success con lista vacia`() = runTest {
        server.enqueue(MockResponse().setBody("""{"loanId":"CR-88213","payments":[]}"""))

        assertEquals(AppResult.Success(Loan("CR-88213", emptyList())), repository.getPayments())
    }

    // Cubre: payments_list/R9
    @Test
    fun `estado desconocido no descarta el resto de la lista`() = runTest {
        server.enqueue(MockResponse().setBody(FakeBackendInterceptor.SAMPLE_JSON.replace("VENCIDO", "CONGELADO")))

        val payments = repository.getPayments().loan().payments

        assertEquals(listOf("p3", "p2", "p1", "p4"), payments.map { it.id })
        assertEquals(PaymentStatus.Unknown("CONGELADO"), payments.first { it.id == "p2" }.status)
    }

    // Cubre: payments_list/R2, payments_list/R10
    @Test
    fun `503 es Server 503`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))

        assertEquals(AppResult.Failure(AppError.Server(503)), repository.getPayments())
    }

    // Cubre: payments_list/R2
    @Test
    fun `404 es NotFound`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))

        assertEquals(AppResult.Failure(AppError.NotFound), repository.getPayments())
    }

    // Cubre: payments_list/R2
    @Test
    fun `desconexion es Network`() = runTest {
        // Varias por si OkHttp reintenta la conexión: la cola vacía bloquearía el test.
        repeat(3) { server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)) }

        assertEquals(AppResult.Failure(AppError.Network), repository.getPayments())
    }

    // Cubre: payments_list/R2
    @Test
    fun `JSON invalido es Serialization`() = runTest {
        server.enqueue(MockResponse().setBody("""{"loanId":"CR-88213","payments":"no-es-lista"}"""))

        assertEquals(AppResult.Failure(AppError.Serialization), repository.getPayments())
    }

    // Cubre: payments_list/R2, payments_list/R7
    @Test
    fun `fecha invalida es Serialization`() = runTest {
        val body = FakeBackendInterceptor.SAMPLE_JSON.replace("2026-11-30T00:00:00Z", "ayer")
        server.enqueue(MockResponse().setBody(body))

        assertEquals(AppResult.Failure(AppError.Serialization), repository.getPayments())
    }
}
