package dev.base.app.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Se inyecta para poder reemplazar los dispatchers en tests. */
interface DispatcherProvider {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

object DefaultDispatcherProvider : DispatcherProvider {
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
}
