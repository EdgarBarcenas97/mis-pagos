package dev.base.app.core.common

/** Resultado de una operación que puede fallar con un [AppError] conocido. */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>

    data class Failure(val error: AppError) : AppResult<Nothing>
}

/** Errores que la app sabe explicar al usuario. La UI nunca ve excepciones. */
sealed interface AppError {
    data object Network : AppError

    data class Server(val code: Int) : AppError

    data object NotFound : AppError

    data object Serialization : AppError

    data object Unknown : AppError
}
