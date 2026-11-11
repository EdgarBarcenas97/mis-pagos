package dev.base.app.core.network

import dev.base.app.core.common.AppError
import dev.base.app.core.common.AppResult
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private const val HTTP_NOT_FOUND = 404

object Network {
    val json: Json = Json { ignoreUnknownKeys = true }

    fun retrofit(baseUrl: String, client: OkHttpClient = OkHttpClient()): Retrofit = Retrofit
        .Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}

/** Ejecuta una llamada de red y convierte cualquier excepción conocida en [AppError]. */
suspend fun <T> safeApiCall(block: suspend () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    AppResult.Failure(if (e.code() == HTTP_NOT_FOUND) AppError.NotFound else AppError.Server(e.code()))
} catch (e: IOException) {
    AppResult.Failure(AppError.Network)
} catch (e: SerializationException) {
    AppResult.Failure(AppError.Serialization)
}
