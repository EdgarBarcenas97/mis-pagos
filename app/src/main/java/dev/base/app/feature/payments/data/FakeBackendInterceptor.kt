package dev.base.app.feature.payments.data

import kotlin.random.Random
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Backend simulado. Si `random.nextDouble() < failureRate` responde HTTP 503; si no, 200 con [body].
 * Antes duerme `latencyMs` con [sleeper] (inyectable en tests).
 */
class FakeBackendInterceptor(
    private val random: Random = Random.Default,
    private val latencyMs: Long = 1_500,
    private val failureRate: Double = 0.30,
    private val body: String = SAMPLE_JSON,
    private val sleeper: (Long) -> Unit = Thread::sleep
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
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
                .body(body.toResponseBody(JSON))
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
