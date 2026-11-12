package dev.base.app.feature.payments.data

import dev.base.app.feature.payments.domain.PayResult
import dev.base.app.feature.payments.domain.PaymentGateway
import javax.inject.Inject

/**
 * `safeApiCall`: Success con status PAGADO → Paid · Server(code in 400..499) o NotFound → Declined ·
 * todo lo demás → Unknown.
 */
class PaymentGatewayImpl @Inject constructor(private val api: PaymentsApi) : PaymentGateway {
    override suspend fun pay(paymentId: String, idempotencyKey: String): PayResult = TODO()
}
