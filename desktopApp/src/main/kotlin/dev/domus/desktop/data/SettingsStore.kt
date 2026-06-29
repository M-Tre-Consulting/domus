package dev.domus.desktop.data

import java.util.prefs.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val PREFS: Preferences = Preferences.userRoot().node("dev/domus/settings")

/** Desktop settings — a subset of Android's (no haptic feedback, no keep-screen-on). */
class SettingsStore {
    private val _showDebugDiag = MutableStateFlow(PREFS.getBoolean("show_debug_diag", true))
    val showDebugDiag: StateFlow<Boolean> = _showDebugDiag.asStateFlow()

    private val _groupByRoom = MutableStateFlow(PREFS.getBoolean("group_by_room", true))
    val groupByRoom: StateFlow<Boolean> = _groupByRoom.asStateFlow()

    private val _refreshIntervalSeconds = MutableStateFlow(PREFS.getInt("refresh_interval_seconds", 10))
    val refreshIntervalSeconds: StateFlow<Int> = _refreshIntervalSeconds.asStateFlow()

    fun setShowDebugDiag(show: Boolean) {
        PREFS.putBoolean("show_debug_diag", show); PREFS.flush()
        _showDebugDiag.value = show
    }

    fun setGroupByRoom(enabled: Boolean) {
        PREFS.putBoolean("group_by_room", enabled); PREFS.flush()
        _groupByRoom.value = enabled
    }

    fun setRefreshIntervalSeconds(seconds: Int) {
        PREFS.putInt("refresh_interval_seconds", seconds); PREFS.flush()
        _refreshIntervalSeconds.value = seconds
    }
}
