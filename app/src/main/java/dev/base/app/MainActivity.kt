package dev.base.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    Scaffold { padding ->
        NavHost(navController, startDestination = PaymentsDestination, modifier = Modifier.padding(padding)) {
            composable<PaymentsDestination> { PaymentsRoute() }
        }
    }
}
