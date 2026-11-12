package dev.base.app.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Tokens: la única fuente de color, tipografía y espaciado. Las pantallas no
// usan valores sueltos; leen MaterialTheme y Spacing.

// Paleta de marca: tokens --l-* (claro) y --d-* (oscuro) de docs/design/mockup.html.
// Sin dynamic color: la marca manda en ambos temas.
private val LightColors = lightColorScheme(
    primary = Color(0xFF1F6F5C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFFFFF), // chip "Pendiente" (--l-pend-bg)
    onPrimaryContainer = Color(0xFF1F6F5C), // --l-pend
    secondary = Color(0xFF4C635B),
    tertiaryContainer = Color(0xFFF3E6C6), // "En proceso" (--l-proc-bg)
    onTertiaryContainer = Color(0xFF6B4A00), // --l-proc
    background = Color(0xFFF4F7F5),
    onBackground = Color(0xFF13201B),
    surface = Color(0xFFF4F7F5),
    onSurface = Color(0xFF13201B),
    surfaceVariant = Color(0xFFDCEBE4),
    onSurfaceVariant = Color(0xFF4E5F58),
    surfaceContainer = Color(0xFFE7EFEB), // header (--l-bar)
    surfaceContainerHighest = Color(0xFFDCEBE4), // Cards M3 (--l-card)
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFF9DCD8), // "Vencido" (--l-venc-bg)
    onErrorContainer = Color(0xFF8C1D18) // --l-venc
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD1B6),
    onPrimary = Color(0xFF00382B),
    primaryContainer = Color(0xFF0D1513), // --d-pend-bg
    onPrimaryContainer = Color(0xFFA8E3CF), // --d-pend
    secondary = Color(0xFFB2CCC2),
    tertiaryContainer = Color(0xFF3B3118), // --d-proc-bg
    onTertiaryContainer = Color(0xFFF2CF83), // --d-proc
    background = Color(0xFF0D1513),
    onBackground = Color(0xFFE3EEE9),
    surface = Color(0xFF0D1513),
    onSurface = Color(0xFFE3EEE9),
    surfaceVariant = Color(0xFF173029),
    onSurfaceVariant = Color(0xFF9DB3AA),
    surfaceContainer = Color(0xFF13201C), // --d-bar
    surfaceContainerHighest = Color(0xFF173029), // --d-card
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF4A1D1A), // --d-venc-bg
    onErrorContainer = Color(0xFFFFB4AB) // --d-venc
)

/** Colores propios fuera del `ColorScheme` de Material 3 (p. ej. el verde de "Pagado"). */
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color
)

// "Pagado" = relleno sólido de marca (--l-pag-bg/--l-pag, --d-pag-bg/--d-pag).
private val LightExtendedColors = ExtendedColors(
    success = Color(0xFF1F6F5C),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFF1F6F5C),
    onSuccessContainer = Color(0xFFFFFFFF)
)

private val DarkExtendedColors = ExtendedColors(
    success = Color(0xFF7FD1B6),
    onSuccess = Color(0xFF00382B),
    successContainer = Color(0xFF7FD1B6),
    onSuccessContainer = Color(0xFF00382B)
)

private val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/** Acceso a los colores propios del tema: `AppColors.current.success`. */
object AppColors {
    val current: ExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current
}

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

@Composable
fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalExtendedColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Typography(),
            content = content
        )
    }
}
