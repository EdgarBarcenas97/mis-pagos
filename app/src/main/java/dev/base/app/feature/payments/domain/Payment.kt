package dev.base.app.feature.payments.domain

import dev.base.app.core.common.AppResult
import java.math.BigDecimal
import java.time.LocalDate

data class Payment(
    val id: String,
    val amount: BigDecimal,
    val currency: String,
    val dueDate: LocalDate,
    val status: PaymentStatus
)

/** Estado de un pago. Un texto desconocido del backend se conserva en [Unknown] y nunca rompe la lista. */
sealed interface PaymentStatus {
    val isPayable: Boolean

    /** `PENDIENTE`, pagable. */
    data object Pending : PaymentStatus {
        override val isPayable: Boolean get() = true
    }

    /** `VENCIDO`, pagable. */
    data object Overdue : PaymentStatus {
        override val isPayable: Boolean get() = true
    }

    /** `PAGADO`. */
    data object Paid : PaymentStatus {
        override val isPayable: Boolean get() = false
    }

    /** `EN_PROCESO`. */
    data object InProcess : PaymentStatus {
        override val isPayable: Boolean get() = false
    }

    data class Unknown(val raw: String) : PaymentStatus {
        override val isPayable: Boolean get() = false
    }

    companion object {
        fun fromApi(raw: String): PaymentStatus = when (raw) {
            "PENDIENTE" -> Pending
            "VENCIDO" -> Overdue
            "PAGADO" -> Paid
            "EN_PROCESO" -> InProcess
            else -> Unknown(raw)
        }
    }
}

data class Loan(val loanId: String, val payments: List<Payment>)

interface PaymentsRepository {
    suspend fun getPayments(): AppResult<Loan>
}
