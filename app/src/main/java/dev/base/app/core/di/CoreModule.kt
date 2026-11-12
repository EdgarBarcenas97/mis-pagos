package dev.base.app.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.base.app.core.common.DefaultDispatcherProvider
import dev.base.app.core.common.DispatcherProvider
import dev.base.app.core.network.Network
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

/** Dependencias compartidas por toda la app. Cada feature declara las suyas en feature/<x>/di/. */
@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    @Provides
    @Singleton
    fun dispatchers(): DispatcherProvider = DefaultDispatcherProvider

    @Provides
    @Singleton
    fun json(): Json = Network.json

    @Provides
    @Singleton
    fun okHttpClient(): OkHttpClient = OkHttpClient()
}
