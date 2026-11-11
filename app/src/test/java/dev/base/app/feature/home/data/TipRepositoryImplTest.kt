package dev.base.app.feature.home.data

import dev.base.app.core.common.AppError
import dev.base.app.core.common.AppResult
import dev.base.app.feature.home.domain.Tip
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

// Patrón de referencia para repositorios: MockK solo en la frontera (el DAO de Room).

class TipRepositoryImplTest {
    private val dao = mockk<TipDao>(relaxUnitFun = true)
    private val repository = TipRepositoryImpl(dao)

    @Test
    fun `si la base esta vacia guarda los tips de ejemplo y los devuelve`() = runTest {
        val seeded = TipRepositoryImpl.DEFAULT_TIPS.map { it.toEntity() }
        coEvery { dao.getAll() } returnsMany listOf(emptyList(), seeded)

        val result = repository.getTips()

        coVerify(exactly = 1) { dao.insertAll(seeded) }
        assertEquals(AppResult.Success(TipRepositoryImpl.DEFAULT_TIPS), result)
    }

    @Test
    fun `si ya hay datos no vuelve a guardar`() = runTest {
        coEvery { dao.getAll() } returns listOf(TipEntity(7, "guardado"))

        val result = repository.getTips()

        coVerify(exactly = 0) { dao.insertAll(any()) }
        assertEquals(AppResult.Success(listOf(Tip(7, "guardado"))), result)
    }

    @Test
    fun `un error de la base se convierte en AppError`() = runTest {
        coEvery { dao.getAll() } throws IllegalStateException("db cerrada")

        assertEquals(AppResult.Failure(AppError.Unknown), repository.getTips())
    }
}
