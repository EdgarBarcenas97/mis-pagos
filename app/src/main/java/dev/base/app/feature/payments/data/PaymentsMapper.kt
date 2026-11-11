package dev.base.app.feature.payments.data

import dev.base.app.feature.payments.domain.Loan
import dev.base.app.feature.payments.domain.Payment
import dev.base.app.feature.payments.domain.PaymentStatus
import java.time.Instant
import java.time.ZoneOffset

/**
 * DTO → dominio. `dueDate = Instant.parse(..).atZone(UTC).toLocalDate()`.
 * Fecha inválida → `DateTimeParseException`.
 */
fun LoanDto.toDomain(): Loan = Loan(loanId = loanId, payments = payments.map { it.toDomain() })

private fun PaymentDto.toDomain(): Payment = Payment(
    id = id,
    amount = amount,
    currency = currency,
    dueDate = Instant.parse(dueDate).atZone(ZoneOffset.UTC).toLocalDate(),
    status = PaymentStatus.fromApi(status)
)
