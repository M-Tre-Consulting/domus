package dev.domus.android.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "domus_settings")

/** Global app settings managed by Jetpack DataStore. */
class SettingsStore(private val context: Context) {
    companion object {
        private val SHOW_DEBUG_DIAG_KEY = booleanPreferencesKey("show_debug_diag")
        private val USE_HAPTIC_FEEDBACK_KEY = booleanPreferencesKey("use_haptic_feedback")
        private val GROUP_BY_ROOM_KEY = booleanPreferencesKey("group_by_room")
        private val KEEP_SCREEN_ON_KEY = booleanPreferencesKey("keep_screen_on")
        private val REFRESH_INTERVAL_KEY = intPreferencesKey("refresh_interval_seconds")
        // Appearance
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")        // "system"|"light"|"dark"
        private val SEED_COLOR_KEY = intPreferencesKey("seed_color_argb")      // 0 = use wallpaper dynamic color
        private val UI_DENSITY_KEY = stringPreferencesKey("ui_density")        // "compact"|"comfortable"|"spacious"
        // Dashboard
        private val DASHBOARD_LAYOUT_KEY = stringPreferencesKey("dashboard_layout") // "grid2"|"list"|"grid4"
    }

    val showDebugDiag: Flow<Boolean> = context.settingsDataStore.data.map { it[SHOW_DEBUG_DIAG_KEY] ?: true }
    val useHapticFeedback: Flow<Boolean> = context.settingsDataStore.data.map { it[USE_HAPTIC_FEEDBACK_KEY] ?: true }
    val groupByRoom: Flow<Boolean> = context.settingsDataStore.data.map { it[GROUP_BY_ROOM_KEY] ?: true }
    val keepScreenOn: Flow<Boolean> = context.settingsDataStore.data.map { it[KEEP_SCREEN_ON_KEY] ?: false }
    val refreshIntervalSeconds: Flow<Int> = context.settingsDataStore.data.map { it[REFRESH_INTERVAL_KEY] ?: 10 }
    val themeMode: Flow<String> = context.settingsDataStore.data.map { it[THEME_MODE_KEY] ?: "system" }
    val seedColorArgb: Flow<Int> = context.settingsDataStore.data.map { it[SEED_COLOR_KEY] ?: 0 }
    val uiDensity: Flow<String> = context.settingsDataStore.data.map { it[UI_DENSITY_KEY] ?: "comfortable" }
    val dashboardLayout: Flow<String> = context.settingsDataStore.data.map { it[DASHBOARD_LAYOUT_KEY] ?: "grid2" }

    suspend fun setShowDebugDiag(show: Boolean) {
        context.settingsDataStore.edit { it[SHOW_DEBUG_DIAG_KEY] = show }
    }

    suspend fun setUseHapticFeedback(use: Boolean) {
        context.settingsDataStore.edit { it[USE_HAPTIC_FEEDBACK_KEY] = use }
    }

    suspend fun setGroupByRoom(enabled: Boolean) {
        context.settingsDataStore.edit { it[GROUP_BY_ROOM_KEY] = enabled }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEEP_SCREEN_ON_KEY] = enabled }
    }

    suspend fun setRefreshIntervalSeconds(seconds: Int) {
        context.settingsDataStore.edit { it[REFRESH_INTERVAL_KEY] = seconds }
    }

    suspend fun setThemeMode(mode: String) {
        context.settingsDataStore.edit { it[THEME_MODE_KEY] = mode }
    }

    suspend fun setSeedColorArgb(argb: Int) {
        context.settingsDataStore.edit { it[SEED_COLOR_KEY] = argb }
    }

    suspend fun setUiDensity(density: String) {
        context.settingsDataStore.edit { it[UI_DENSITY_KEY] = density }
    }

    suspend fun setDashboardLayout(layout: String) {
        context.settingsDataStore.edit { it[DASHBOARD_LAYOUT_KEY] = layout }
    }
}
