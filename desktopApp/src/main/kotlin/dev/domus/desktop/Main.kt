package dev.domus.desktop

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.domus.desktop.data.ConnectionStore
import dev.domus.desktop.data.FavoritesStore
import dev.domus.desktop.data.HaSessionHolder
import dev.domus.desktop.data.SettingsStore
import dev.domus.desktop.ui.screens.ClimateDetailScreen
import dev.domus.desktop.ui.screens.ConnectScreen
import dev.domus.desktop.ui.screens.DashboardScreen
import dev.domus.desktop.ui.screens.EntityPickerScreen
import dev.domus.desktop.ui.screens.LightDetailScreen
import dev.domus.desktop.ui.screens.LockDetailScreen
import dev.domus.desktop.ui.screens.MediaPlayerDetailScreen
import dev.domus.desktop.ui.screens.SettingsScreen
import dev.domus.desktop.ui.screens.SwitchDetailScreen
import dev.domus.shared.api.HaApiException
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.HaConnectionConfig

// --- Navigation model ---

private sealed interface Screen {
    data object Splash : Screen
    data object Connect : Screen
    data object Dashboard : Screen
    data object Picker : Screen
    data object Settings : Screen
    data class ClimateDetail(val entityId: String) : Screen
    data class LightDetail(val entityId: String) : Screen
    data class SwitchDetail(val entityId: String) : Screen
    data class MediaPlayerDetail(val entityId: String) : Screen
    data class LockDetail(val entityId: String) : Screen
}

fun main() = application {
    val windowState = rememberWindowState(size = DpSize(1100.dp, 750.dp))
    Window(onCloseRequest = ::exitApplication, title = "Domus", state = windowState) {
        DomusDesktopTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                App()
            }
        }
    }
}

@Composable
private fun App() {
    val connectionStore = remember { ConnectionStore() }
    val favoritesStore = remember { FavoritesStore() }
    val settingsStore = remember { SettingsStore() }
    val favoriteEntityIds by favoritesStore.favoriteEntityIds.collectAsState()

    var screenStack by remember { mutableStateOf(listOf<Screen>(Screen.Splash)) }
    val currentScreen = screenStack.last()

    fun push(screen: Screen) { screenStack = screenStack + screen }
    fun pop() { if (screenStack.size > 1) screenStack = screenStack.dropLast(1) }
    fun replaceAll(screen: Screen) { screenStack = listOf(screen) }

    Crossfade(targetState = currentScreen, label = "nav") { screen ->
        when (screen) {
            Screen.Splash -> SplashScreen(connectionStore = connectionStore, onNavigate = ::replaceAll)

            Screen.Connect -> ConnectScreen(
                onConnected = { config ->
                    connectionStore.save(config)
                    val session = HaSession(config)
                    HaSessionHolder.connect(session)
                    replaceAll(Screen.Dashboard)
                },
            )

            Screen.Dashboard -> {
                val session = HaSessionHolder.session
                if (session == null) {
                    replaceAll(Screen.Connect)
                } else {
                    DashboardScreen(
                        session = session,
                        settingsStore = settingsStore,
                        favoriteEntityIds = favoriteEntityIds,
                        onEditEntities = { push(Screen.Picker) },
                        onOpenSettings = { push(Screen.Settings) },
                        onLogout = {
                            connectionStore.clear()
                            HaSessionHolder.disconnect()
                            replaceAll(Screen.Connect)
                        },
                        onOpenDetail = { entityId ->
                            val domain = session.repository.entities.value[entityId]?.domain
                            when (domain) {
                                "climate", "water_heater" -> push(Screen.ClimateDetail(entityId))
                                "light" -> push(Screen.LightDetail(entityId))
                                "switch" -> push(Screen.SwitchDetail(entityId))
                                "media_player" -> push(Screen.MediaPlayerDetail(entityId))
                                "lock" -> push(Screen.LockDetail(entityId))
                                else -> {}
                            }
                        },
                    )
                }
            }

            Screen.Picker -> {
                val session = HaSessionHolder.session
                if (session == null) { replaceAll(Screen.Connect) } else {
                    EntityPickerScreen(
                        session = session,
                        initialSelection = favoriteEntityIds,
                        onSave = { selection -> favoritesStore.setFavorites(selection); pop() },
                        onBack = ::pop,
                    )
                }
            }

            Screen.Settings -> SettingsScreen(settingsStore = settingsStore, onBack = ::pop)

            is Screen.ClimateDetail -> {
                val session = HaSessionHolder.session
                if (session == null) { replaceAll(Screen.Connect) } else {
                    ClimateDetailScreen(session = session, entityId = screen.entityId, onBack = ::pop)
                }
            }

            is Screen.LightDetail -> {
                val session = HaSessionHolder.session
                if (session == null) { replaceAll(Screen.Connect) } else {
                    LightDetailScreen(session = session, entityId = screen.entityId, onBack = ::pop)
                }
            }

            is Screen.SwitchDetail -> {
                val session = HaSessionHolder.session
                if (session == null) { replaceAll(Screen.Connect) } else {
                    SwitchDetailScreen(session = session, entityId = screen.entityId, onBack = ::pop)
                }
            }

            is Screen.MediaPlayerDetail -> {
                val session = HaSessionHolder.session
                if (session == null) { replaceAll(Screen.Connect) } else {
                    MediaPlayerDetailScreen(session = session, entityId = screen.entityId, onBack = ::pop)
                }
            }

            is Screen.LockDetail -> {
                val session = HaSessionHolder.session
                if (session == null) { replaceAll(Screen.Connect) } else {
                    LockDetailScreen(session = session, entityId = screen.entityId, onBack = ::pop)
                }
            }
        }
    }
}

@Composable
private fun SplashScreen(connectionStore: ConnectionStore, onNavigate: (Screen) -> Unit) {
    LaunchedEffect(Unit) {
        val savedConfig = connectionStore.read()
        val reconnected: HaSession? = savedConfig?.let { config ->
            val session = HaSession(config)
            try {
                session.restApi.checkConnection()
                session
            } catch (_: HaApiException) {
                null // 401/403: credentials invalid, must re-connect
            } catch (_: Exception) {
                session // Network error: optimistically go to Dashboard
            }
        }

        if (reconnected != null) {
            HaSessionHolder.connect(reconnected)
            onNavigate(Screen.Dashboard)
        } else {
            onNavigate(Screen.Connect)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
