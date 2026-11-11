package dev.base.app.feature.payments.data

import java.math.BigDecimal
import kotlinx.serialization.Serializable
import retrofit2.http.GET

interface PaymentsApi {
    @GET("loans/payments")
    suspend fun getPayments(): LoanDto
}

@Serializable
data class LoanDto(val loanId: String, val payments: List<PaymentDto>)

@Serializable
data class PaymentDto(
    val id: String,
    /** Texto del primitivo JSON, nunca `Double`. */
    @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal,
    val currency: String,
    /** ISO-8601 instant. */
    val dueDate: String,
    /** Texto libre: un estado nuevo no rompe el parseo. */
    val status: String
)
