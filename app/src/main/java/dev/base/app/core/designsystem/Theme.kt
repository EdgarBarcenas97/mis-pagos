package dev.base.app.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Tokens: la única fuente de color, tipografía y espaciado. Las pantallas no
// usan valores sueltos; leen MaterialTheme y Spacing.

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F6F5C),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF4C635B),
    background = Color(0xFFF7FAF8),
    surface = Color(0xFFF7FAF8),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AD5BE),
    onPrimary = Color(0xFF00382D),
    secondary = Color(0xFFB2CCC2),
    background = Color(0xFF0F1513),
    surface = Color(0xFF0F1513),
    error = Color(0xFFFFB4AB)
)

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

@Composable
fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
