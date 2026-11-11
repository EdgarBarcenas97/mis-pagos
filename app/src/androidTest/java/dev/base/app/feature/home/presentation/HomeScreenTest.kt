package dev.base.app.feature.home.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.base.app.core.common.AppError
import dev.base.app.core.designsystem.AppTheme
import dev.base.app.core.designsystem.StateTags
import dev.base.app.feature.home.domain.Tip
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Patrón de referencia para tests de UI: se prueba la Screen sin estado,
// buscando por testTag (contrato definido en design.md).

class HomeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun contenido_muestra_cada_tip() {
        compose.setContent {
            AppTheme { HomeScreen(HomeUiState.Content(listOf(Tip(1, "Hola"))), onRetry = {}) }
        }
        compose.onNodeWithTag(HomeTags.item(1)).assertIsDisplayed()
        compose.onNodeWithText("Hola").assertIsDisplayed()
    }

    @Test
    fun error_muestra_reintentar_y_lo_dispara() {
        var retried = false
        compose.setContent {
            AppTheme { HomeScreen(HomeUiState.Error(AppError.Network), onRetry = { retried = true }) }
        }
        compose.onNodeWithTag(StateTags.ERROR).assertIsDisplayed()
        compose.onNodeWithTag(StateTags.RETRY).performClick()
        assertTrue(retried)
    }
}
