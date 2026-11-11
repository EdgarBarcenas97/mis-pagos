package dev.base.app.feature.payments.data

import dev.base.app.core.network.Network
import dev.base.app.feature.payments.domain.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentsMapperTest {
    private fun decodeSample(): LoanDto =
        Network.json.decodeFromString(LoanDto.serializer(), FakeBackendInterceptor.SAMPLE_JSON)

    private fun dto(id: String, dueDate: String = "2026-11-30T00:00:00Z", status: String = "PENDIENTE") =
        PaymentDto(id = id, amount = BigDecimal("1250.10"), currency = "MXN", dueDate = dueDate, status = status)

    // Cubre: payments_list/R6
    @Test
    fun `monto se lee como BigDecimal exacto conservando la escala`() {
        val loan = decodeSample().toDomain()
        val p1 = loan.payments.first { it.id == "p1" }
        val p4 = loan.payments.first { it.id == "p4" }

        // BigDecimal.equals compara también la escala: 1250.10 != 1250.1
        assertEquals(BigDecimal("1250.10"), p1.amount)
        assertEquals(2, p1.amount.scale())
        assertEquals(BigDecimal("980.55"), p4.amount)
        assertEquals("MXN", p1.currency)
    }

    // Cubre: payments_list/R6
    @Test
    fun `monto con mas digitos de los que caben en un Double no pierde precision`() {
        val json = """{"loanId":"CR-1","payments":[
            {"id":"x","amount":12345678901234567.89,"currency":"MXN","dueDate":"2026-11-30T00:00:00Z","status":"PENDIENTE"}
        ]}"""
        val loan = Network.json.decodeFromString(LoanDto.serializer(), json)

        assertEquals(BigDecimal("12345678901234567.89"), loan.payments.single().amount)
    }

    // Cubre: payments_list/R7
    @Test
    fun `fecha limite es la fecha de calendario UTC aunque el dispositivo este en CDMX`() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Mexico_City"))
            val loan = decodeSample().toDomain()

            assertEquals(LocalDate.of(2026, 11, 30), loan.payments.first { it.id == "p1" }.dueDate)
            assertEquals(LocalDate.of(2026, 10, 31), loan.payments.first { it.id == "p2" }.dueDate)
            assertEquals(LocalDate.of(2026, 9, 30), loan.payments.first { it.id == "p3" }.dueDate)
            assertEquals(LocalDate.of(2026, 12, 31), loan.payments.first { it.id == "p4" }.dueDate)
        } finally {
            TimeZone.setDefault(original)
        }
    }

    // Cubre: payments_list/R7
    @Test
    fun `fecha limite al final del dia UTC no se corre al dia siguiente en zona positiva`() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
            val loan = LoanDto("CR-1", listOf(dto("x", dueDate = "2026-11-30T23:30:00Z"))).toDomain()

            assertEquals(LocalDate.of(2026, 11, 30), loan.payments.single().dueDate)
        } finally {
            TimeZone.setDefault(original)
        }
    }

    // Cubre: payments_list/R7
    @Test(expected = DateTimeParseException::class)
    fun `fecha invalida lanza DateTimeParseException`() {
        LoanDto("CR-1", listOf(dto("x", dueDate = "30/11/2026"))).toDomain()
    }

    // Cubre: payments_list/R8
    @Test
    fun `los cuatro estados conocidos se mapean desde el texto del API`() {
        assertEquals(PaymentStatus.Pending, PaymentStatus.fromApi("PENDIENTE"))
        assertEquals(PaymentStatus.Overdue, PaymentStatus.fromApi("VENCIDO"))
        assertEquals(PaymentStatus.Paid, PaymentStatus.fromApi("PAGADO"))
        assertEquals(PaymentStatus.InProcess, PaymentStatus.fromApi("EN_PROCESO"))
    }

    // Cubre: payments_list/R8
    @Test
    fun `solo Pendiente y Vencido son pagables`() {
        assertTrue(PaymentStatus.Pending.isPayable)
        assertTrue(PaymentStatus.Overdue.isPayable)
        assertFalse(PaymentStatus.Paid.isPayable)
        assertFalse(PaymentStatus.InProcess.isPayable)
    }

    // Cubre: payments_list/R8
    @Test
    fun `el JSON del enunciado mapea cada pago a su estado`() {
        val byId = decodeSample().toDomain().payments.associateBy { it.id }

        assertEquals(PaymentStatus.Pending, byId.getValue("p1").status)
        assertEquals(PaymentStatus.Overdue, byId.getValue("p2").status)
        assertEquals(PaymentStatus.Paid, byId.getValue("p3").status)
        assertEquals(PaymentStatus.InProcess, byId.getValue("p4").status)
        assertEquals("CR-88213", decodeSample().toDomain().loanId)
    }

    // Cubre: payments_list/R9
    @Test
    fun `estado desconocido es Unknown con el texto original y no es pagable`() {
        val status = PaymentStatus.fromApi("REEMBOLSADO")

        assertEquals(PaymentStatus.Unknown("REEMBOLSADO"), status)
        assertFalse(status.isPayable)
    }

    // Cubre: payments_list/R9
    @Test
    fun `cadena vacia es Unknown`() {
        assertEquals(PaymentStatus.Unknown(""), PaymentStatus.fromApi(""))
    }

    // Cubre: payments_list/R9
    @Test
    fun `estado desconocido no descarta el resto de la lista`() {
        val loan = LoanDto(
            "CR-88213",
            listOf(dto("a", status = "PENDIENTE"), dto("b", status = "CONGELADO"), dto("c", status = "PAGADO"))
        ).toDomain()

        assertEquals(listOf("a", "b", "c"), loan.payments.map { it.id }.sorted())
        assertEquals(PaymentStatus.Unknown("CONGELADO"), loan.payments.first { it.id == "b" }.status)
    }

    // Cubre: payments_list/R9
    @Test
    fun `un estado desconocido en el JSON no rompe el parseo`() {
        val json = FakeBackendInterceptor.SAMPLE_JSON.replace("EN_PROCESO", "ESTADO_NUEVO")
        val loan = Network.json.decodeFromString(LoanDto.serializer(), json).toDomain()

        assertEquals(4, loan.payments.size)
        assertEquals(PaymentStatus.Unknown("ESTADO_NUEVO"), loan.payments.first { it.id == "p4" }.status)
    }
}
