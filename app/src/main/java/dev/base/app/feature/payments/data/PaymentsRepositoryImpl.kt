package dev.base.app.feature.payments.data

import dev.base.app.core.common.AppResult
import dev.base.app.feature.payments.domain.Loan
import dev.base.app.feature.payments.domain.PaymentsRepository
import javax.inject.Inject

/**
 * `safeApiCall` de core/network: IOException → Network, HTTP → Server/NotFound,
 * SerializationException → Serialization. Error de mapeo (fecha inválida) → AppError.Serialization.
 * Éxito → pagos ordenados por dueDate ascendente.
 */
class PaymentsRepositoryImpl @Inject constructor(private val api: PaymentsApi) : PaymentsRepository {
    override suspend fun getPayments(): AppResult<Loan> = TODO()
}
