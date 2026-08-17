package dev.domus.android

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.domus.android.ui.AppLockGate
import dev.domus.android.ui.LocalAnimatedVisibilityScope
import dev.domus.android.ui.LocalRefreshIntervalSeconds
import dev.domus.android.ui.LocalSharedTransitionScope
import dev.domus.android.data.ConnectionStore
import dev.domus.android.data.DomusShortcuts
import dev.domus.android.data.FavoritesStore
import dev.domus.android.data.HaSessionHolder
import dev.domus.android.data.OnboardingStore
import dev.domus.android.data.SettingsStore
import dev.domus.android.ui.screens.CameraDetailScreen
import dev.domus.android.ui.screens.ClimateDetailScreen
import dev.domus.android.ui.screens.ConnectScreen
import dev.domus.android.ui.screens.DashboardScreen
import dev.domus.android.ui.screens.EntityPickerScreen
import dev.domus.android.ui.screens.LightDetailScreen
import dev.domus.android.ui.screens.LockDetailScreen
import dev.domus.android.ui.screens.MediaPlayerDetailScreen
import dev.domus.android.ui.screens.OAuthLoginScreen
import dev.domus.android.ui.screens.OnboardingScreen
import dev.domus.android.ui.screens.SettingsScreen
import dev.domus.android.ui.screens.SplashScreen
import dev.domus.android.ui.screens.SwitchDetailScreen
import dev.domus.android.ui.theme.DomusTheme
import dev.domus.android.widget.DomusWidget
import dev.domus.shared.api.HaApiException
import dev.domus.shared.api.HaOAuthException
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.HaConnectionConfig
import dev.domus.shared.model.HaCredentials
import dev.domus.shared.model.friendlyName
import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsStore = SettingsStore(applicationContext)
        setContent {
            val themeMode by settingsStore.themeMode.collectAsState(initial = "system")
            val seedColorArgb by settingsStore.seedColorArgb.collectAsState(initial = 0)
            val uiDensity by settingsStore.uiDensity.collectAsState(initial = "comfortable")

            DomusTheme(themeMode = themeMode, seedColorArgb = seedColorArgb) {
                val densityMultiplier = when (uiDensity) {
                    "compact" -> 0.85f
                    "spacious" -> 1.15f
                    else -> 1.0f
                }
                val base = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(base.density * densityMultiplier, base.fontScale),
                ) {
                    AppLockGate(settingsStore = settingsStore, activity = this@MainActivity) {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            DomusNavHost()
                        }
                    }
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
    const val LOCK_DETAIL = "lock_detail"
    const val CAMERA_DETAIL = "camera_detail"
    const val ENTITY_DETAIL_ARG = "entityId"
    const val OAUTH_LOGIN = "oauth_login"
    const val OAUTH_LOGIN_ARG = "baseUrl"
}

@Composable
private fun DomusNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Android 16 (API 36) requires a runtime grant for local-network access.
    // Request it immediately so the user sees the dialog on first launch rather
    // than having to hunt for "Nearby devices" in Settings after hitting an error.
    val localNetworkPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 36 &&
            context.checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK") !=
            PackageManager.PERMISSION_GRANTED
        ) {
            localNetworkPermLauncher.launch("android.permission.ACCESS_LOCAL_NETWORK")
        }
    }
    // Android 13+ requires a runtime grant to show the "keep connected in background" status
    // notification. Requested lazily (only once that setting is turned on) below.
    val notificationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}
    val connectionStore = remember { ConnectionStore(context.applicationContext) }
    val favoritesStore = remember { FavoritesStore(context.applicationContext) }
    val onboardingStore = remember { OnboardingStore(context.applicationContext) }
    val settingsStore = remember { SettingsStore(context.applicationContext) }
    val favoriteEntityIds by favoritesStore.favoriteEntityIds.collectAsState(initial = emptySet())
    val refreshIntervalSeconds by settingsStore.refreshIntervalSeconds.collectAsState(initial = 10)
    val keepConnectionAlive by settingsStore.keepConnectionAlive.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    fun persistRefreshed(baseUrl: String): suspend (HaCredentials.OAuthSession) -> Unit = { refreshed ->
        connectionStore.save(HaConnectionConfig(baseUrl, refreshed))
    }

    CompositionLocalProvider(LocalRefreshIntervalSeconds provides refreshIntervalSeconds) {
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
                // Reconnect using saved credentials.
                // HaApiException(401/403) or HaOAuthException (dead refresh_token) means
                // credentials are invalid → force re-login.
                // Any other exception (network timeout, server unreachable) is transient —
                // keep the session and let the Dashboard's WS reconnect loop recover.
                val reconnected = savedConfig?.let { config ->
                    val session = HaSession(config, persistRefreshed(config.baseUrl))
                    try {
                        session.restApi.checkConnection()
                        session
                    } catch (e: HaApiException) {
                        null // Explicit 401/403: credentials rejected, must re-login
                    } catch (e: HaOAuthException) {
                        null // refresh_token rejected by the server, must re-login
                    } catch (_: Exception) {
                        session // Network error: go to Dashboard, it will reconnect
                    }
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
            SplashScreen()
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
                LaunchedEffect(session) {
                    session.authExpired.collect { expired ->
                        if (expired) {
                            HaConnectionService.stop(context.applicationContext)
                            HaSessionHolder.disconnect()
                            navController.navigate(Routes.CONNECT) {
                                popUpTo(Routes.DASHBOARD) { inclusive = true }
                            }
                        }
                    }
                }
                LaunchedEffect(session, keepConnectionAlive) {
                    if (keepConnectionAlive) {
                        if (Build.VERSION.SDK_INT >= 33 &&
                            context.checkSelfPermission("android.permission.POST_NOTIFICATIONS") !=
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermLauncher.launch("android.permission.POST_NOTIFICATIONS")
                        }
                        HaConnectionService.start(context.applicationContext)
                    } else {
                        HaConnectionService.stop(context.applicationContext)
                    }
                }
                LaunchedEffect(session, favoriteEntityIds) {
                    session.repository.entities
                        .map { entities -> favoriteEntityIds.mapNotNull { entities[it] } }
                        .distinctUntilChangedBy { list -> list.map { it.entityId to it.friendlyName } }
                        .collect { favorites ->
                            try {
                                DomusShortcuts.update(context.applicationContext, favorites)
                            } catch (_: Exception) {
                                // Best-effort: e.g. ShortcutManager rate limit. Never worth crashing over.
                            }
                            try {
                                DomusWidget.updateAll(context.applicationContext)
                            } catch (_: Exception) {
                                // Best-effort: no widget instances placed, etc.
                            }
                        }
                }
                CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                    DashboardScreen(
                        session = session,
                        settingsStore = settingsStore,
                        favoriteEntityIds = favoriteEntityIds,
                        onEditEntities = { navController.navigate(Routes.PICKER) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onLogout = {
                            scope.launch { connectionStore.clear() }
                            HaConnectionService.stop(context.applicationContext)
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
                                "lock" -> navController.navigate("${Routes.LOCK_DETAIL}/$entityId")
                                "camera" -> navController.navigate("${Routes.CAMERA_DETAIL}/$entityId")
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
        composable(
            route = "${Routes.LOCK_DETAIL}/{${Routes.ENTITY_DETAIL_ARG}}",
            arguments = listOf(navArgument(Routes.ENTITY_DETAIL_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val entityId = backStackEntry.arguments?.getString(Routes.ENTITY_DETAIL_ARG)
            val session = HaSessionHolder.session
            if (session == null || entityId == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.CONNECT) { popUpTo(Routes.LOCK_DETAIL) { inclusive = true } }
                }
            } else {
                LockDetailScreen(session = session, entityId = entityId, onBack = { navController.popBackStack() })
            }
        }
        composable(
            route = "${Routes.CAMERA_DETAIL}/{${Routes.ENTITY_DETAIL_ARG}}",
            arguments = listOf(navArgument(Routes.ENTITY_DETAIL_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val entityId = backStackEntry.arguments?.getString(Routes.ENTITY_DETAIL_ARG)
            val session = HaSessionHolder.session
            if (session == null || entityId == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.CONNECT) { popUpTo(Routes.CAMERA_DETAIL) { inclusive = true } }
                }
            } else {
                CameraDetailScreen(session = session, entityId = entityId, onBack = { navController.popBackStack() })
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
                session = HaSessionHolder.session,
                favoriteEntityIds = favoriteEntityIds,
                onBack = { navController.popBackStack() },
            )
        }
    }
    } // CompositionLocalProvider(LocalSharedTransitionScope)
    } // SharedTransitionLayout
    } // CompositionLocalProvider(LocalRefreshIntervalSeconds)
}
