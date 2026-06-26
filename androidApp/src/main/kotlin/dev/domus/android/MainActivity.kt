package dev.domus.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.domus.android.ui.LocalAnimatedVisibilityScope
import dev.domus.android.ui.LocalSharedTransitionScope
import dev.domus.android.data.ConnectionStore
import dev.domus.android.data.FavoritesStore
import dev.domus.android.data.HaSessionHolder
import dev.domus.android.data.OnboardingStore
import dev.domus.android.data.SettingsStore
import dev.domus.android.ui.screens.ClimateDetailScreen
import dev.domus.android.ui.screens.ConnectScreen
import dev.domus.android.ui.screens.DashboardScreen
import dev.domus.android.ui.screens.EntityPickerScreen
import dev.domus.android.ui.screens.LightDetailScreen
import dev.domus.android.ui.screens.MediaPlayerDetailScreen
import dev.domus.android.ui.screens.OAuthLoginScreen
import dev.domus.android.ui.screens.OnboardingScreen
import dev.domus.android.ui.screens.SettingsScreen
import dev.domus.android.ui.screens.SwitchDetailScreen
import dev.domus.android.ui.theme.DomusTheme
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.HaConnectionConfig
import dev.domus.shared.model.HaCredentials
import java.net.URLDecoder
import java.net.URLEncoder
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
    const val ONBOARDING = "onboarding"
    const val CONNECT = "connect"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val PICKER = "picker"
    const val CLIMATE_DETAIL = "climate_detail"
    const val LIGHT_DETAIL = "light_detail"
    const val SWITCH_DETAIL = "switch_detail"
    const val MEDIA_PLAYER_DETAIL = "media_player_detail"
    const val ENTITY_DETAIL_ARG = "entityId"
    const val OAUTH_LOGIN = "oauth_login"
    const val OAUTH_LOGIN_ARG = "baseUrl"
}

@Composable
private fun DomusNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val connectionStore = remember { ConnectionStore(context.applicationContext) }
    val favoritesStore = remember { FavoritesStore(context.applicationContext) }
    val onboardingStore = remember { OnboardingStore(context.applicationContext) }
    val settingsStore = remember { SettingsStore(context.applicationContext) }
    val favoriteEntityIds by favoritesStore.favoriteEntityIds.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()

    fun persistRefreshed(baseUrl: String): suspend (HaCredentials.OAuthSession) -> Unit = { refreshed ->
        connectionStore.save(HaConnectionConfig(baseUrl, refreshed))
    }

    SharedTransitionLayout {
    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = { slideInHorizontally(initialOffsetX = { it / 4 }) + fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 4 }) + fadeOut() },
    ) {
        composable(Routes.SPLASH) {
            LaunchedEffect(Unit) {
                val savedConfig = connectionStore.read()
                val reconnected = savedConfig?.let { config ->
                    runCatching {
                        val session = HaSession(config, persistRefreshed(config.baseUrl))
                        if (session.restApi.checkConnection()) session else null
                    }.getOrNull()
                }

                if (reconnected != null) {
                    HaSessionHolder.connect(reconnected)
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                } else if (!onboardingStore.hasSeenOnboarding()) {
                    navController.navigate(Routes.ONBOARDING) {
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
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    scope.launch { onboardingStore.markSeen() }
                    navController.navigate(Routes.CONNECT) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.CONNECT) {
            ConnectScreen(
                onConnected = { config ->
                    scope.launch { connectionStore.save(config) }
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.CONNECT) { inclusive = true }
                    }
                },
                onLoginWithHomeAssistant = { baseUrl ->
                    val encoded = URLEncoder.encode(baseUrl, "UTF-8")
                    navController.navigate("${Routes.OAUTH_LOGIN}/$encoded")
                },
            )
        }
        composable(
            route = "${Routes.OAUTH_LOGIN}/{${Routes.OAUTH_LOGIN_ARG}}",
            arguments = listOf(navArgument(Routes.OAUTH_LOGIN_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val encodedBaseUrl = backStackEntry.arguments?.getString(Routes.OAUTH_LOGIN_ARG)
            if (encodedBaseUrl == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                val baseUrl = URLDecoder.decode(encodedBaseUrl, "UTF-8")
                OAuthLoginScreen(
                    baseUrl = baseUrl,
                    onConnected = { config ->
                        scope.launch { connectionStore.save(config) }
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.CONNECT) { inclusive = true }
                        }
                    },
                    onCredentialsRefreshed = persistRefreshed(baseUrl),
                    onBack = { navController.popBackStack() },
                )
            }
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
                CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                    DashboardScreen(
                        session = session,
                        settingsStore = settingsStore,
                        favoriteEntityIds = favoriteEntityIds,
                        onEditEntities = { navController.navigate(Routes.PICKER) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onLogout = {
                            scope.launch { connectionStore.clear() }
                            HaSessionHolder.disconnect()
                            navController.navigate(Routes.CONNECT) {
                                popUpTo(Routes.DASHBOARD) { inclusive = true }
                            }
                        },
                        onOpenDetail = { entityId ->
                            val domain = session.repository.entities.value[entityId]?.domain
                            when (domain) {
                                "climate", "water_heater" -> navController.navigate("${Routes.CLIMATE_DETAIL}/$entityId")
                                "light" -> navController.navigate("${Routes.LIGHT_DETAIL}/$entityId")
                                "switch" -> navController.navigate("${Routes.SWITCH_DETAIL}/$entityId")
                                "media_player" -> navController.navigate("${Routes.MEDIA_PLAYER_DETAIL}/$entityId")
                                else -> {}
                            }
                        },
                    )
                }
            }
        }
        composable(
            route = "${Routes.CLIMATE_DETAIL}/{${Routes.ENTITY_DETAIL_ARG}}",
            arguments = listOf(navArgument(Routes.ENTITY_DETAIL_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val entityId = backStackEntry.arguments?.getString(Routes.ENTITY_DETAIL_ARG)
            val session = HaSessionHolder.session
            if (session == null || entityId == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.CONNECT) { popUpTo(Routes.CLIMATE_DETAIL) { inclusive = true } }
                }
            } else {
                ClimateDetailScreen(session = session, entityId = entityId, onBack = { navController.popBackStack() })
            }
        }
        composable(
            route = "${Routes.LIGHT_DETAIL}/{${Routes.ENTITY_DETAIL_ARG}}",
            arguments = listOf(navArgument(Routes.ENTITY_DETAIL_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val entityId = backStackEntry.arguments?.getString(Routes.ENTITY_DETAIL_ARG)
            val session = HaSessionHolder.session
            if (session == null || entityId == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.CONNECT) { popUpTo(Routes.LIGHT_DETAIL) { inclusive = true } }
                }
            } else {
                CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                    LightDetailScreen(session = session, entityId = entityId, onBack = { navController.popBackStack() })
                }
            }
        }
        composable(
            route = "${Routes.SWITCH_DETAIL}/{${Routes.ENTITY_DETAIL_ARG}}",
            arguments = listOf(navArgument(Routes.ENTITY_DETAIL_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val entityId = backStackEntry.arguments?.getString(Routes.ENTITY_DETAIL_ARG)
            val session = HaSessionHolder.session
            if (session == null || entityId == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.CONNECT) { popUpTo(Routes.SWITCH_DETAIL) { inclusive = true } }
                }
            } else {
                CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                    SwitchDetailScreen(session = session, entityId = entityId, onBack = { navController.popBackStack() })
                }
            }
        }
        composable(
            route = "${Routes.MEDIA_PLAYER_DETAIL}/{${Routes.ENTITY_DETAIL_ARG}}",
            arguments = listOf(navArgument(Routes.ENTITY_DETAIL_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val entityId = backStackEntry.arguments?.getString(Routes.ENTITY_DETAIL_ARG)
            val session = HaSessionHolder.session
            if (session == null || entityId == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.CONNECT) { popUpTo(Routes.MEDIA_PLAYER_DETAIL) { inclusive = true } }
                }
            } else {
                MediaPlayerDetailScreen(session = session, entityId = entityId, onBack = { navController.popBackStack() })
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
        composable(Routes.SETTINGS) {
            SettingsScreen(
                settingsStore = settingsStore,
                onBack = { navController.popBackStack() },
            )
        }
    }
    } // CompositionLocalProvider(LocalSharedTransitionScope)
    } // SharedTransitionLayout
}
