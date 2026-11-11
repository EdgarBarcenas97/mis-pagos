package dev.base.app.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.base.app.core.common.DefaultDispatcherProvider
import dev.base.app.core.common.DispatcherProvider
import dev.base.app.core.database.AppDatabase
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

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "app.db").build()
}
