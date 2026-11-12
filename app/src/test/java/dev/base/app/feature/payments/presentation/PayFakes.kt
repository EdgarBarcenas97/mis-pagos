package dev.base.app.feature.payments.presentation

import dev.base.app.core.common.DispatcherProvider
import dev.base.app.feature.payments.domain.IdempotencyKeyFactory
import dev.base.app.feature.payments.domain.PayResult
import dev.base.app.feature.payments.domain.Payment
import dev.base.app.feature.payments.domain.PaymentGateway
import dev.base.app.feature.payments.domain.PaymentStatus
import dev.base.app.feature.payments.domain.PendingPaymentStore
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.TestDispatcher

internal class PayTestDispatchers(dispatcher: TestDispatcher) : DispatcherProvider {
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

/** Store en memoria. Anota "save:<id>:<llave>" y "remove:<id>" en [events] (compartido con el gateway). */
internal class InMemoryPendingPaymentStore(private val events: MutableList<String> = mutableListOf()) :
    PendingPaymentStore {
    private val keys = linkedMapOf<String, String>()

    override fun save(paymentId: String, idempotencyKey: String) {
        events += "save:$paymentId:$idempotencyKey"
        keys[paymentId] = idempotencyKey
    }

    override fun remove(paymentId: String) {
        events += "remove:$paymentId"
        keys.remove(paymentId)
    }

    override fun all(): Map<String, String> = keys.toMap()
}

/** Llaves deterministas "k1", "k2", … y cuántas se generaron. */
internal class SequentialKeyFactory : IdempotencyKeyFactory {
    var generated = 0
        private set

    override fun newKey(): String = "k${++generated}"
}

/**
 * Gateway falso: devuelve [results] en orden (el último se repite; sin resultados → Unknown),
 * anota "pay:<id>:<llave>" en [events], guarda cada llamada y lo que había en [store] en ese momento.
 * Si hay [gate], cada llamada espera a que se complete.
 */
internal class FakePaymentGateway(
    private val events: MutableList<String> = mutableListOf(),
    vararg results: PayResult,
    private val store: PendingPaymentStore? = null
) : PaymentGateway {
    private val queue = ArrayDeque(results.toList())
    val calls = mutableListOf<Pair<String, String>>()
    val storeAtCall = mutableListOf<Map<String, String>>()
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun pay(paymentId: String, idempotencyKey: String): PayResult {
        events += "pay:$paymentId:$idempotencyKey"
        calls += paymentId to idempotencyKey
        store?.let { storeAtCall += it.all() }
        gate?.await()
        return when {
            queue.size > 1 -> queue.removeFirst()
            queue.isEmpty() -> PayResult.Unknown
            else -> queue.first()
        }
    }
}

internal val payP1 = Payment("p1", BigDecimal("1250.10"), "MXN", LocalDate.of(2026, 11, 30), PaymentStatus.Pending)
internal val payP2 = Payment("p2", BigDecimal("1250.10"), "MXN", LocalDate.of(2026, 10, 31), PaymentStatus.Overdue)
internal val payP3 = Payment("p3", BigDecimal("1250.10"), "MXN", LocalDate.of(2026, 9, 30), PaymentStatus.Paid)
internal val payP4 = Payment("p4", BigDecimal("980.55"), "MXN", LocalDate.of(2026, 12, 31), PaymentStatus.InProcess)
internal val payP5 =
    Payment("p5", BigDecimal("10.00"), "MXN", LocalDate.of(2027, 1, 31), PaymentStatus.Unknown("CONGELADO"))
internal val payList = listOf(payP3, payP2, payP1, payP4)
