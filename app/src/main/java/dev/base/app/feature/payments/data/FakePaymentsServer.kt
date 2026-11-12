package dev.base.app.feature.payments.data

/** Estado del servidor simulado: pagos, llaves de idempotencia y cargos. */
class FakePaymentsServer(private val initialJson: String = FakeBackendInterceptor.SAMPLE_JSON) {
    private val initiallyPaid: Set<String> = OBJECT_REGEX.findAll(initialJson)
        .mapNotNull { match ->
            val obj = match.value
            val id = ID_REGEX.find(obj)?.groupValues?.get(1)
            val status = STATUS_REGEX.find(obj)?.groupValues?.get(1)
            if (id != null && status == PAID) id else null
        }
        .toSet()
    private val usedKeys = mutableSetOf<String>()
    private val charges = mutableMapOf<String, Int>()

    /** Sin pagos registrados devuelve exactamente initialJson; después, el JSON con los estados actualizados. */
    @Synchronized
    fun paymentsJson(): String {
        if (charges.isEmpty()) return initialJson
        return OBJECT_REGEX.replace(initialJson) { match ->
            val obj = match.value
            val id = ID_REGEX.find(obj)?.groupValues?.get(1)
            if (id != null && charges.containsKey(id)) {
                STATUS_REGEX.replace(obj) { "\"status\": \"$PAID\"" }
            } else {
                obj
            }
        }
    }

    /** Registra el pago si no estaba pagado y la llave es nueva. Idempotente por llave y por pago. */
    @Synchronized
    fun registerPayment(paymentId: String, idempotencyKey: String) {
        if (!usedKeys.add(idempotencyKey)) return
        if (isPaid(paymentId)) return
        charges[paymentId] = 1
    }

    @Synchronized
    fun isPaid(paymentId: String): Boolean = paymentId in initiallyPaid || charges.containsKey(paymentId)

    /** Cargos reales; nunca > 1. */
    @Synchronized
    fun chargeCount(paymentId: String): Int = charges[paymentId] ?: 0

    private companion object {
        const val PAID = "PAGADO"
        val OBJECT_REGEX = Regex("""\{[^{}]*\}""")
        val ID_REGEX = Regex(""""id"\s*:\s*"([^"]*)"""")
        val STATUS_REGEX = Regex(""""status"\s*:\s*"([^"]*)"""")
    }
}
