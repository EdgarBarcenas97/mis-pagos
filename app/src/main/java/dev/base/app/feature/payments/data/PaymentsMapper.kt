package dev.base.app.feature.payments.data

import dev.base.app.feature.payments.domain.Loan

/**
 * DTO → dominio. `dueDate = Instant.parse(..).atZone(UTC).toLocalDate()`.
 * Fecha inválida → `DateTimeParseException`.
 */
fun LoanDto.toDomain(): Loan = TODO()
