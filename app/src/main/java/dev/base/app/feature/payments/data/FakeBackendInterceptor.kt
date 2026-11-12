package dev.base.app.feature.payments.data

import kotlin.random.Random
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Backend simulado.
 *
 * GET `.../loans/payments`: duerme `latencyMs` con [sleeper]; si `random.nextDouble() < failureRate`
 * responde HTTP 503; si no, 200 con [body] (o `server.paymentsJson()` si [body] es null).
 *
 * POST `.../loans/payments/{id}/pay`: duerme un valor de [payLatencyMs]; luego, con [random]:
 * decline → 422 sin registrar · timeout → (registra si toca) y lanza `SocketTimeoutException`
 * · si no → registra y 200 `{"paymentId":id,"status":"PAGADO"}`. Pago ya pagado → 200 PAGADO sin nuevo cargo.
 */
class FakeBackendInterceptor(
    private val random: Random = Random.Default,
    private val latencyMs: Long = 1_500,
    private val failureRate: Double = 0.30,
    private val body: String? = null,
    private val sleeper: (Long) -> Unit = Thread::sleep,
    private val server: FakePaymentsServer = FakePaymentsServer(),
    private val payLatencyMs: LongRange = 2_000L..5_000L,
    private val payTimeoutRate: Double = 0.30,
    private val registeredOnTimeoutRate: Double = 0.50,
    private val payDeclineRate: Double = 0.0
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val segments = request.url.pathSegments
        val isPay = request.method == "POST" && segments.lastOrNull() == "pay"
        return if (isPay) pay(chain, segments[segments.size - 2]) else payments(chain)
    }

    private fun pay(chain: Interceptor.Chain, paymentId: String): Response = TODO()

    private fun payments(chain: Interceptor.Chain): Response {
        sleeper(latencyMs)
        val fails = random.nextDouble() < failureRate
        val builder = Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
        return if (fails) {
            builder.code(HTTP_UNAVAILABLE).message("Service Unavailable")
                .body("".toResponseBody(JSON))
                .build()
        } else {
            builder.code(HTTP_OK).message("OK")
                .body((body ?: server.paymentsJson()).toResponseBody(JSON))
                .build()
        }
    }

    companion object {
        private const val HTTP_OK = 200
        private const val HTTP_UNAVAILABLE = 503
        private val JSON = "application/json".toMediaType()

        /** JSON exacto del enunciado. */
        const val SAMPLE_JSON: String = """{
  "loanId": "CR-88213",
  "payments": [
    { "id": "p1", "amount": 1250.10, "currency": "MXN", "dueDate": "2026-11-30T00:00:00Z", "status": "PENDIENTE" },
    { "id": "p2", "amount": 1250.10, "currency": "MXN", "dueDate": "2026-10-31T00:00:00Z", "status": "VENCIDO" },
    { "id": "p3", "amount": 1250.10, "currency": "MXN", "dueDate": "2026-09-30T00:00:00Z", "status": "PAGADO" },
    { "id": "p4", "amount": 980.55, "currency": "MXN", "dueDate": "2026-12-31T00:00:00Z", "status": "EN_PROCESO" }
  ]
}"""
    }
}
