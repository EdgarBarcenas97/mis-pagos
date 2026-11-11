package dev.base.app.feature.payments.presentation

import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentsFormatTest {
    // Cubre: payments_list/R6
    @Test
    fun `monto con separador de miles y moneda`() {
        assertEquals("$1,250.10 MXN", formatAmount(BigDecimal("1250.10"), "MXN"))
    }

    // Cubre: payments_list/R6
    @Test
    fun `monto menor a mil`() {
        assertEquals("$980.55 MXN", formatAmount(BigDecimal("980.55"), "MXN"))
    }

    // Cubre: payments_list/R6
    @Test
    fun `monto siempre con dos decimales`() {
        assertEquals("$1,250.10 MXN", formatAmount(BigDecimal("1250.1"), "MXN"))
        assertEquals("$1,000.00 MXN", formatAmount(BigDecimal("1000"), "MXN"))
    }

    // Cubre: payments_list/R6
    @Test
    fun `monto no depende del Locale del dispositivo`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("$1,250.10 MXN", formatAmount(BigDecimal("1250.10"), "MXN"))
        } finally {
            Locale.setDefault(original)
        }
    }

    // Cubre: payments_list/R7
    @Test
    fun `pago no pagado muestra Vence el`() {
        assertEquals("Vence el 30 nov 2026", formatDueDate(LocalDate.of(2026, 11, 30), paid = false))
    }

    // Cubre: payments_list/R7
    @Test
    fun `pago pagado muestra Vencio el`() {
        assertEquals("Venció el 30 sep 2026", formatDueDate(LocalDate.of(2026, 9, 30), paid = true))
    }

    // Cubre: payments_list/R7
    @Test
    fun `fecha no depende del Locale del dispositivo`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("Vence el 30 nov 2026", formatDueDate(LocalDate.of(2026, 11, 30), paid = false))
        } finally {
            Locale.setDefault(original)
        }
    }
}
