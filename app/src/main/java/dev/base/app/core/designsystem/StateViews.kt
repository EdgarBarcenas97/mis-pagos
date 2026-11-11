package dev.base.app.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import dev.base.app.R

/** Estados comunes de pantalla. Cualquier feature los reutiliza. */
object StateTags {
    const val LOADING = "state_loading"
    const val EMPTY = "state_empty"
    const val ERROR = "state_error"
    const val RETRY = "state_retry"
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.state_loading)
    CenteredColumn(modifier.testTag(StateTags.LOADING)) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = description })
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    CenteredColumn(modifier.testTag(StateTags.EMPTY)) {
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    CenteredColumn(modifier.testTag(StateTags.ERROR)) {
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Button(onClick = onRetry, modifier = Modifier.testTag(StateTags.RETRY)) {
            Text(stringResource(R.string.state_retry))
        }
    }
}

@Composable
private fun CenteredColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) { content() }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStatePreview() {
    AppTheme { ErrorState(message = "Sin conexión. Revisa tu red.", onRetry = {}) }
}
