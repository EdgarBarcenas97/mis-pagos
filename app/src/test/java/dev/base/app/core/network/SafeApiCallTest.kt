package dev.base.app.core.network

import dev.base.app.core.common.AppError
import dev.base.app.core.common.AppResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.http.GET

// Patrón de referencia para tests de red: MockWebServer + la API real de Retrofit.

@Serializable
private data class PingDto(val ok: Boolean)

private interface PingApi {
    @GET("ping")
    suspend fun ping(): PingDto
}

class SafeApiCallTest {
    private val server = MockWebServer()
    private lateinit var api: PingApi

    @Before
    fun setUp() {
        server.start()
        api = Network.retrofit(server.url("/").toString()).create(PingApi::class.java)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `respuesta 200 valida es Success`() = runTest {
        server.enqueue(MockResponse().setBody("""{"ok":true}"""))
        assertEquals(AppResult.Success(PingDto(true)), safeApiCall { api.ping() })
    }

    @Test
    fun `404 es NotFound`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        assertEquals(AppResult.Failure(AppError.NotFound), safeApiCall { api.ping() })
    }

    @Test
    fun `500 es Server con su codigo`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        assertEquals(AppResult.Failure(AppError.Server(500)), safeApiCall { api.ping() })
    }

    @Test
    fun `JSON invalido es Serialization`() = runTest {
        server.enqueue(MockResponse().setBody("""{"ok":"no-es-boolean"}"""))
        assertEquals(AppResult.Failure(AppError.Serialization), safeApiCall { api.ping() })
    }

    @Test
    fun `sin conexion es Network`() = runTest {
        server.shutdown()
        assertEquals(AppResult.Failure(AppError.Network), safeApiCall { api.ping() })
    }
}
