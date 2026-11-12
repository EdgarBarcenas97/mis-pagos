package dev.base.app.feature.payments.data

import dev.base.app.core.common.AppError
import dev.base.app.core.common.AppResult
import dev.base.app.core.network.safeApiCall
import dev.base.app.feature.payments.domain.PayResult
import dev.base.app.feature.payments.domain.PaymentGateway
import javax.inject.Inject

/**
 * `safeApiCall`: Success con status PAGADO → Paid · Server(code in 400..499) o NotFound → Declined ·
 * todo lo demás → Unknown.
 */
class PaymentGatewayImpl @Inject constructor(private val api: PaymentsApi) : PaymentGateway {
    override suspend fun pay(paymentId: String, idempotencyKey: String): PayResult =
        when (val result = safeApiCall { api.pay(paymentId, idempotencyKey) }) {
            is AppResult.Success -> if (result.data.status == PAID) PayResult.Paid else PayResult.Unknown
            is AppResult.Failure -> when (val error = result.error) {
                is AppError.Server -> if (error.code in CLIENT_ERRORS) PayResult.Declined else PayResult.Unknown
                AppError.NotFound -> PayResult.Declined
                else -> PayResult.Unknown
            }
        }

    private companion object {
        const val PAID = "PAGADO"
        val CLIENT_ERRORS = 400..499
    }
}
