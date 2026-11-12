package dev.base.app.feature.payments.data

import dev.base.app.core.common.AppError
import dev.base.app.core.common.AppResult
import dev.base.app.core.network.safeApiCall
import dev.base.app.feature.payments.domain.Loan
import dev.base.app.feature.payments.domain.PaymentsRepository
import java.time.format.DateTimeParseException
import javax.inject.Inject

/**
 * `safeApiCall` de core/network: IOException → Network, HTTP → Server/NotFound,
 * SerializationException → Serialization. Error de mapeo (fecha inválida) → AppError.Serialization.
 * Éxito → pagos ordenados por dueDate ascendente.
 */
class PaymentsRepositoryImpl @Inject constructor(private val api: PaymentsApi) : PaymentsRepository {
    override suspend fun getPayments(): AppResult<Loan> = when (val result = safeApiCall { api.getPayments() }) {
        is AppResult.Failure -> result
        is AppResult.Success -> try {
            val loan = result.data.toDomain()
            AppResult.Success(loan.copy(payments = loan.payments.sortedBy { it.dueDate }))
        } catch (e: DateTimeParseException) {
            AppResult.Failure(AppError.Serialization)
        }
    }
}
