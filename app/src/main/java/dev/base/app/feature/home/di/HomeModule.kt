package dev.base.app.feature.home.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.base.app.core.database.AppDatabase
import dev.base.app.feature.home.data.TipDao
import dev.base.app.feature.home.data.TipRepositoryImpl
import dev.base.app.feature.home.domain.TipRepository

/** Dependencias de la feature home: qué implementación usa cada interfaz del dominio. */
@Module
@InstallIn(SingletonComponent::class)
abstract class HomeModule {
    @Binds
    abstract fun tipRepository(impl: TipRepositoryImpl): TipRepository

    companion object {
        @Provides
        fun tipDao(database: AppDatabase): TipDao = database.tipDao()
    }
}
