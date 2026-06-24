package dev.domus.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.domus.android.data.ConnectionStore
import dev.domus.android.data.FavoritesStore
import dev.domus.android.data.HaSessionHolder
import dev.domus.android.ui.screens.ConnectScreen
import dev.domus.android.ui.screens.DashboardScreen
import dev.domus.android.ui.screens.EntityPickerScreen
import dev.domus.android.ui.theme.DomusTheme
import dev.domus.shared.data.HaSession
import kotlinx.coroutines.launch

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
    const val SPLASH = "splash"
    const val CONNECT = "connect"
    const val DASHBOARD = "dashboard"
    const val PICKER = "picker"
}

@Composable
private fun DomusNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val connectionStore = remember { ConnectionStore(context.applicationContext) }
    val favoritesStore = remember { FavoritesStore(context.applicationContext) }
    val favoriteEntityIds by favoritesStore.favoriteEntityIds.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            LaunchedEffect(Unit) {
                val savedConfig = connectionStore.read()
                val reconnected = savedConfig?.let { config ->
                    runCatching {
                        val session = HaSession(config)
                        if (session.restApi.checkConnection()) session else null
                    }.getOrNull()
                }

                if (reconnected != null) {
                    HaSessionHolder.session = reconnected
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                } else {
                    navController.navigate(Routes.CONNECT) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        composable(Routes.CONNECT) {
            ConnectScreen(
                onConnected = { config ->
                    scope.launch { connectionStore.save(config) }
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
                DashboardScreen(
                    session = session,
                    favoriteEntityIds = favoriteEntityIds,
                    onEditEntities = { navController.navigate(Routes.PICKER) },
                    onLogout = {
                        scope.launch { connectionStore.clear() }
                        HaSessionHolder.session = null
                        navController.navigate(Routes.CONNECT) {
                            popUpTo(Routes.DASHBOARD) { inclusive = true }
                        }
                    },
                )
            }
        }
        composable(Routes.PICKER) {
            val session = HaSessionHolder.session
            if (session == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.CONNECT) {
                        popUpTo(Routes.PICKER) { inclusive = true }
                    }
                }
            } else {
                EntityPickerScreen(
                    session = session,
                    initialSelection = favoriteEntityIds,
                    onSave = { selection ->
                        scope.launch {
                            favoritesStore.setFavorites(selection)
                            navController.popBackStack()
                        }
                    },
                )
            }
        }
    }
}
