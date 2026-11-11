package dev.base.app.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.base.app.core.common.AppError
import dev.base.app.core.common.AppResult
import dev.base.app.core.common.DispatcherProvider
import dev.base.app.feature.home.domain.Tip
import dev.base.app.feature.home.domain.TipRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Content(val tips: List<Tip>) : HomeUiState

    data object Empty : HomeUiState

    data class Error(val error: AppError) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TipRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            val result = withContext(dispatchers.io) { repository.getTips() }
            _uiState.value = when (result) {
                is AppResult.Success -> if (result.data.isEmpty()) {
                    HomeUiState.Empty
                } else {
                    HomeUiState.Content(
                        result.data
                    )
                }
                is AppResult.Failure -> HomeUiState.Error(result.error)
            }
        }
    }
}
