package dev.base.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.base.app.core.designsystem.AppTheme
import dev.base.app.feature.payments.presentation.PaymentsRoute
import kotlinx.serialization.Serializable

/** Rutas tipadas de navegación. Cada feature agrega la suya aquí. */
@Serializable
data object PaymentsDestination

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Antes de super.onCreate: el splash se retira solo al dibujar el primer frame.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme { AppNavHost() }
        }
    }
}

@Composable
private fun AppNavHost() {
    val navController = rememberNavController()
    // Sin Scaffold externo: cada pantalla maneja sus insets con su propio Scaffold.
    NavHost(navController, startDestination = PaymentsDestination) {
        composable<PaymentsDestination> { PaymentsRoute() }
    }
}
