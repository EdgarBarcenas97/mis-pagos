package dev.base.app.feature.payments.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.base.app.core.network.Network
import dev.base.app.feature.payments.data.FakeBackendInterceptor
import dev.base.app.feature.payments.data.PaymentsApi
import dev.base.app.feature.payments.data.PaymentsRepositoryImpl
import dev.base.app.feature.payments.domain.PaymentsRepository
import javax.inject.Singleton
import okhttp3.OkHttpClient

private const val BASE_URL = "https://api.mispagos.fake/"

/** Dependencias de la feature payments. Nada sale a la red: [FakeBackendInterceptor] responde antes. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PaymentsModule {
    @Binds
    abstract fun paymentsRepository(impl: PaymentsRepositoryImpl): PaymentsRepository

    companion object {
        @Provides
        @Singleton
        fun paymentsApi(client: OkHttpClient): PaymentsApi {
            val fakeClient = client.newBuilder().addInterceptor(FakeBackendInterceptor()).build()
            return Network.retrofit(BASE_URL, fakeClient).create(PaymentsApi::class.java)
        }
    }
}
