package dev.base.app.feature.payments.presentation

import java.math.BigDecimal
import java.time.LocalDate

/** `"$1,250.10 MXN"` (Locale es-MX, 2 decimales). */
fun formatAmount(amount: BigDecimal, currency: String): String = TODO()

/** `"Vence el 30 nov 2026"` / `"Venció el 30 sep 2026"` si [paid]. */
fun formatDueDate(date: LocalDate, paid: Boolean): String = TODO()
