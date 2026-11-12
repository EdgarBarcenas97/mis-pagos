package dev.base.app.feature.payments.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.base.app.core.network.Network
import dev.base.app.feature.payments.data.FakeBackendInterceptor
import dev.base.app.feature.payments.data.FakePaymentsServer
import dev.base.app.feature.payments.data.PaymentGatewayImpl
import dev.base.app.feature.payments.data.PaymentsApi
import dev.base.app.feature.payments.data.PaymentsRepositoryImpl
import dev.base.app.feature.payments.data.PrefsPendingPaymentStore
import dev.base.app.feature.payments.domain.IdempotencyKeyFactory
import dev.base.app.feature.payments.domain.PaymentGateway
import dev.base.app.feature.payments.domain.PaymentsRepository
import dev.base.app.feature.payments.domain.PendingPaymentStore
import java.util.UUID
import javax.inject.Singleton
import okhttp3.OkHttpClient

private const val BASE_URL = "https://api.mispagos.fake/"

/**
 * Dependencias de la feature payments. Nada sale a la red: [FakeBackendInterceptor] responde antes,
 * con un único [FakePaymentsServer] compartido (lista y pagos ven el mismo estado).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PaymentsModule {
    @Binds
    abstract fun paymentsRepository(impl: PaymentsRepositoryImpl): PaymentsRepository

    @Binds
    abstract fun paymentGateway(impl: PaymentGatewayImpl): PaymentGateway

    @Binds
    @Singleton
    abstract fun pendingPaymentStore(impl: PrefsPendingPaymentStore): PendingPaymentStore

    companion object {
        @Provides
        @Singleton
        fun fakePaymentsServer(): FakePaymentsServer = FakePaymentsServer()

        @Provides
        @Singleton
        fun paymentsApi(client: OkHttpClient, server: FakePaymentsServer): PaymentsApi {
            val fakeClient = client.newBuilder().addInterceptor(FakeBackendInterceptor(server = server)).build()
            return Network.retrofit(BASE_URL, fakeClient).create(PaymentsApi::class.java)
        }

        @Provides
        fun idempotencyKeyFactory(): IdempotencyKeyFactory = IdempotencyKeyFactory { UUID.randomUUID().toString() }
    }
}
