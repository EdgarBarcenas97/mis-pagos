package dev.base.app.feature.home.presentation

import app.cash.turbine.test
import dev.base.app.core.common.AppError
import dev.base.app.core.common.AppResult
import dev.base.app.core.common.DispatcherProvider
import dev.base.app.feature.home.domain.Tip
import dev.base.app.feature.home.domain.TipRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

// Patrón de referencia para tests de ViewModel: fake del repositorio,
// dispatcher de test inyectado y Turbine para leer los estados en orden.

private class FakeTipRepository(var result: AppResult<List<Tip>>) : TipRepository {
    override suspend fun getTips() = result
}

private class TestDispatchers(dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val io = dispatcher
    override val default = dispatcher
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(result: AppResult<List<Tip>>) =
        HomeViewModel(FakeTipRepository(result), TestDispatchers(dispatcher))

    @Test
    fun `muestra Loading y luego Content cuando hay tips`() = runTest(dispatcher) {
        val tips = listOf(Tip(1, "a"))
        viewModel(AppResult.Success(tips)).uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            assertEquals(HomeUiState.Content(tips), awaitItem())
        }
    }

    @Test
    fun `muestra Empty cuando la lista viene vacía`() = runTest(dispatcher) {
        viewModel(AppResult.Success(emptyList())).uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            assertEquals(HomeUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `muestra Error y retry vuelve a cargar`() = runTest(dispatcher) {
        val repository = FakeTipRepository(AppResult.Failure(AppError.Network))
        val vm = HomeViewModel(repository, TestDispatchers(dispatcher))
        vm.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            assertEquals(HomeUiState.Error(AppError.Network), awaitItem())

            repository.result = AppResult.Success(listOf(Tip(1, "a")))
            vm.retry()
            assertEquals(HomeUiState.Loading, awaitItem())
            assertEquals(HomeUiState.Content(listOf(Tip(1, "a"))), awaitItem())
        }
    }
}
