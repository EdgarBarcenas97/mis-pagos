package dev.base.app.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.base.app.R
import dev.base.app.core.designsystem.AppTheme
import dev.base.app.core.designsystem.EmptyState
import dev.base.app.core.designsystem.ErrorState
import dev.base.app.core.designsystem.LoadingState
import dev.base.app.core.designsystem.Spacing
import dev.base.app.core.designsystem.messageRes
import dev.base.app.feature.home.domain.Tip

object HomeTags {
    const val LIST = "home_list"
    fun item(id: Int) = "home_item_$id"
}

/** Conecta el ViewModel con la pantalla. */
@Composable
fun HomeRoute(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state = state, onRetry = viewModel::retry)
}

/** Sin estado: recibe datos y lambdas. Es lo que se testea y previsualiza. */
@Composable
fun HomeScreen(state: HomeUiState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(Spacing.md)
        )
        when (state) {
            HomeUiState.Loading -> LoadingState()
            HomeUiState.Empty -> EmptyState(stringResource(R.string.state_empty))
            is HomeUiState.Error -> ErrorState(stringResource(state.error.messageRes()), onRetry)
            is HomeUiState.Content -> TipList(state.tips)
        }
    }
}

@Composable
private fun TipList(tips: List<Tip>) {
    LazyColumn(
        modifier = Modifier.testTag(HomeTags.LIST),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(tips, key = { it.id }) { tip ->
            Card(Modifier.testTag(HomeTags.item(tip.id))) {
                Text(tip.title, modifier = Modifier.padding(Spacing.md), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    AppTheme { HomeScreen(HomeUiState.Content(listOf(Tip(1, "Empieza por la spec"))), onRetry = {}) }
}
