package dev.base.app.feature.payments.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.base.app.feature.payments.domain.PendingPaymentStore
import javax.inject.Inject

/** Llaves pendientes en `SharedPreferences` (archivo "pending_payments"), escritas con `commit()` síncrono. */
class PrefsPendingPaymentStore @Inject constructor(@ApplicationContext context: Context) : PendingPaymentStore {
    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun save(paymentId: String, idempotencyKey: String): Unit = TODO()

    override fun remove(paymentId: String): Unit = TODO()

    override fun all(): Map<String, String> = TODO()

    private companion object {
        const val FILE_NAME = "pending_payments"
    }
}
