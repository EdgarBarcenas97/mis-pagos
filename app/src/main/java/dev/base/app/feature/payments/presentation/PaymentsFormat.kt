package dev.base.app.feature.payments.presentation

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.util.Locale

private val MX = Locale.forLanguageTag("es-MX")

/**
 * Abreviaturas propias: CLDR para es-MX da "sept." (y con punto) según la versión del JDK/Android,
 * y el diseño fija "sep" sin punto.
 */
private val MONTHS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/** `"$1,250.10 MXN"` (Locale es-MX, 2 decimales). */
fun formatAmount(amount: BigDecimal, currency: String): String {
    val format = DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(MX))
    return "$" + format.format(amount.setScale(2, RoundingMode.HALF_EVEN)) + " " + currency
}

/** `"Vence el 30 nov 2026"` / `"Venció el 30 sep 2026"` si [paid]. */
fun formatDueDate(date: LocalDate, paid: Boolean): String {
    val prefix = if (paid) "Venció el" else "Vence el"
    return "$prefix ${date.dayOfMonth} ${MONTHS[date.monthValue - 1]} ${date.year}"
}
