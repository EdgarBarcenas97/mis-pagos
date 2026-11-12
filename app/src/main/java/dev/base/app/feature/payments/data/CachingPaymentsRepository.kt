package dev.base.app.feature.payments.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.base.app.core.common.AppResult
import dev.base.app.core.network.Network
import dev.base.app.feature.payments.domain.Loan
import dev.base.app.feature.payments.domain.Payment
import dev.base.app.feature.payments.domain.PaymentStatus
import dev.base.app.feature.payments.domain.PaymentsRepository
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

/** Marca de antigüedad del último dato bueno, expuesta sin cambiar [PaymentsRepository.getPayments]. */
interface PaymentsCacheInfo {
    /** Instante del último `getPayments` exitoso (o null si nunca hubo). */
    val lastSuccessAt: StateFlow<Instant?>

    /** true si el último resultado devuelto vino de caché porque la llamada falló. */
    val isStale: StateFlow<Boolean>
}

/**
 * Decorador: guarda el último [Loan] bueno + su [Instant] (memoria + `SharedPreferences` como JSON
 * con [Network.json]). Si la llamada falla y hay caché, devuelve el dato guardado y marca [isStale].
 */
@Singleton
class CachingPaymentsRepository internal constructor(
    private val delegate: PaymentsRepository,
    private val storage: Storage,
    private val clock: Clock
) : PaymentsRepository,
    PaymentsCacheInfo {

    @Inject
    constructor(
        delegate: PaymentsRepositoryImpl,
        @ApplicationContext context: Context
    ) : this(delegate, PrefsStorage(context), Clock.systemUTC())

    /** Persistencia del JSON del caché (sustituible en tests). */
    interface Storage {
        fun read(): String?

        fun write(value: String)
    }

    private var cached: CachedLoan? = storage.read()?.let { raw ->
        runCatching { Network.json.decodeFromString(CachedLoan.serializer(), raw) }.getOrNull()
    }

    private val _lastSuccessAt = MutableStateFlow(cached?.let { Instant.ofEpochMilli(it.savedAtEpochMs) })
    override val lastSuccessAt: StateFlow<Instant?> = _lastSuccessAt.asStateFlow()

    private val _isStale = MutableStateFlow(false)
    override val isStale: StateFlow<Boolean> = _isStale.asStateFlow()

    override suspend fun getPayments(): AppResult<Loan> = when (val result = delegate.getPayments()) {
        is AppResult.Success -> {
            val now = clock.instant()
            val entry = CachedLoan.from(result.data, now)
            cached = entry
            runCatching { storage.write(Network.json.encodeToString(CachedLoan.serializer(), entry)) }
            _lastSuccessAt.value = now
            _isStale.value = false
            result
        }
        is AppResult.Failure -> {
            val entry = cached
            if (entry == null) {
                _isStale.value = false
                result
            } else {
                _isStale.value = true
                AppResult.Success(entry.toDomain())
            }
        }
    }

    private class PrefsStorage(context: Context) : Storage {
        private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

        override fun read(): String? = prefs.getString(KEY, null)

        override fun write(value: String) {
            prefs.edit().putString(KEY, value).apply()
        }
    }

    private companion object {
        const val FILE_NAME = "payments_cache"
        const val KEY = "last_loan"
    }
}

@Serializable
internal data class CachedLoan(val loanId: String, val savedAtEpochMs: Long, val payments: List<CachedPayment>) {
    fun toDomain(): Loan = Loan(loanId, payments.map { it.toDomain() })

    companion object {
        fun from(loan: Loan, at: Instant): CachedLoan = CachedLoan(
            loanId = loan.loanId,
            savedAtEpochMs = at.toEpochMilli(),
            payments = loan.payments.map {
                CachedPayment(it.id, it.amount.toPlainString(), it.currency, it.dueDate.toString(), it.status.toRaw())
            }
        )
    }
}

@Serializable
internal data class CachedPayment(
    val id: String,
    val amount: String,
    val currency: String,
    val dueDate: String,
    val status: String
) {
    fun toDomain(): Payment =
        Payment(id, BigDecimal(amount), currency, LocalDate.parse(dueDate), PaymentStatus.fromApi(status))
}

private fun PaymentStatus.toRaw(): String = when (this) {
    PaymentStatus.Pending -> "PENDIENTE"
    PaymentStatus.Overdue -> "VENCIDO"
    PaymentStatus.Paid -> "PAGADO"
    PaymentStatus.InProcess -> "EN_PROCESO"
    is PaymentStatus.Unknown -> raw
}
