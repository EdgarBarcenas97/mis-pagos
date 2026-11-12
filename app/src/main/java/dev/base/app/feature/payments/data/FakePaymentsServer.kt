package dev.base.app.feature.payments.data

/** Estado del servidor simulado: pagos, llaves de idempotencia y cargos. */
class FakePaymentsServer(private val initialJson: String = FakeBackendInterceptor.SAMPLE_JSON) {
    /** Sin pagos registrados devuelve exactamente initialJson; después, el JSON con los estados actualizados. */
    @Synchronized
    fun paymentsJson(): String = initialJson // TODO(pay_payment T1): reflejar pagos registrados.

    /** Registra el pago si no estaba pagado y la llave es nueva. Idempotente por llave y por pago. */
    @Synchronized
    fun registerPayment(paymentId: String, idempotencyKey: String): Unit = TODO()

    @Synchronized
    fun isPaid(paymentId: String): Boolean = TODO()

    /** Cargos reales; nunca > 1. */
    @Synchronized
    fun chargeCount(paymentId: String): Int = TODO()
}
