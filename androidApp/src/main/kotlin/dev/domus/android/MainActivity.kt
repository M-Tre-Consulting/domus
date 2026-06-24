package dev.domus.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.domus.android.data.HaSessionHolder
import dev.domus.android.ui.screens.ConnectScreen
import dev.domus.android.ui.screens.DashboardScreen
import dev.domus.android.ui.theme.DomusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DomusTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DomusNavHost()
                }
            }
        }
    }
}

object Routes {
    const val CONNECT = "connect"
    const val DASHBOARD = "dashboard"
}

@Composable
private fun DomusNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.CONNECT) {
        composable(Routes.CONNECT) {
            ConnectScreen(
                onConnected = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.CONNECT) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.DASHBOARD) {
            val session = HaSessionHolder.session
            if (session == null) {
                // Process death or direct navigation without connecting first.
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.CONNECT) {
                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                    }
                }
            } else {
                DashboardScreen(session)
            }
        }
    }
}
