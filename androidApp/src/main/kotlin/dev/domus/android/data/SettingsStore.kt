package dev.domus.android.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
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
        private val KEEP_CONNECTION_ALIVE_KEY = booleanPreferencesKey("keep_connection_alive")
        private val QUICK_TILE_ENTITY_ID_KEY = stringPreferencesKey("quick_tile_entity_id")
        private val APP_LOCK_ENABLED_KEY = booleanPreferencesKey("app_lock_enabled")
        private val REFRESH_INTERVAL_KEY = intPreferencesKey("refresh_interval_seconds")
        // Appearance
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")        // "system"|"light"|"dark"
        private val SEED_COLOR_KEY = intPreferencesKey("seed_color_argb")      // 0 = use wallpaper dynamic color
        // Dashboard
        private val DASHBOARD_LAYOUT_KEY = stringPreferencesKey("dashboard_layout") // "grid2"|"list"|"grid4"
        // Geofence
        private val GEOFENCE_ENABLED_KEY = booleanPreferencesKey("geofence_enabled")
        private val GEOFENCE_LATITUDE_KEY = doublePreferencesKey("geofence_latitude")
        private val GEOFENCE_LONGITUDE_KEY = doublePreferencesKey("geofence_longitude")
        private val GEOFENCE_RADIUS_METERS_KEY = intPreferencesKey("geofence_radius_meters")
        private val GEOFENCE_ENTITY_ID_KEY = stringPreferencesKey("geofence_entity_id")
    }

    val showDebugDiag: Flow<Boolean> = context.settingsDataStore.data.map { it[SHOW_DEBUG_DIAG_KEY] ?: true }
    val useHapticFeedback: Flow<Boolean> = context.settingsDataStore.data.map { it[USE_HAPTIC_FEEDBACK_KEY] ?: true }
    val groupByRoom: Flow<Boolean> = context.settingsDataStore.data.map { it[GROUP_BY_ROOM_KEY] ?: true }
    val keepScreenOn: Flow<Boolean> = context.settingsDataStore.data.map { it[KEEP_SCREEN_ON_KEY] ?: false }
    val keepConnectionAlive: Flow<Boolean> =
        context.settingsDataStore.data.map { it[KEEP_CONNECTION_ALIVE_KEY] ?: false }
    val quickTileEntityId: Flow<String?> =
        context.settingsDataStore.data.map { it[QUICK_TILE_ENTITY_ID_KEY] }
    val appLockEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[APP_LOCK_ENABLED_KEY] ?: false }
    val refreshIntervalSeconds: Flow<Int> = context.settingsDataStore.data.map { it[REFRESH_INTERVAL_KEY] ?: 10 }
    val themeMode: Flow<String> = context.settingsDataStore.data.map { it[THEME_MODE_KEY] ?: "system" }
    val seedColorArgb: Flow<Int> = context.settingsDataStore.data.map { it[SEED_COLOR_KEY] ?: 0 }
    val dashboardLayout: Flow<String> = context.settingsDataStore.data.map { it[DASHBOARD_LAYOUT_KEY] ?: "grid2" }
    val geofenceEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[GEOFENCE_ENABLED_KEY] ?: false }
    val geofenceLatitude: Flow<Double?> = context.settingsDataStore.data.map { it[GEOFENCE_LATITUDE_KEY] }
    val geofenceLongitude: Flow<Double?> = context.settingsDataStore.data.map { it[GEOFENCE_LONGITUDE_KEY] }
    val geofenceRadiusMeters: Flow<Int> = context.settingsDataStore.data.map { it[GEOFENCE_RADIUS_METERS_KEY] ?: 150 }
    val geofenceEntityId: Flow<String?> = context.settingsDataStore.data.map { it[GEOFENCE_ENTITY_ID_KEY] }

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

    suspend fun setKeepConnectionAlive(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEEP_CONNECTION_ALIVE_KEY] = enabled }
    }

    suspend fun setQuickTileEntityId(entityId: String?) {
        context.settingsDataStore.edit {
            if (entityId == null) it.remove(QUICK_TILE_ENTITY_ID_KEY) else it[QUICK_TILE_ENTITY_ID_KEY] = entityId
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[APP_LOCK_ENABLED_KEY] = enabled }
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

    suspend fun setDashboardLayout(layout: String) {
        context.settingsDataStore.edit { it[DASHBOARD_LAYOUT_KEY] = layout }
    }

    suspend fun setGeofenceEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[GEOFENCE_ENABLED_KEY] = enabled }
    }

    suspend fun setGeofenceLocation(latitude: Double, longitude: Double) {
        context.settingsDataStore.edit {
            it[GEOFENCE_LATITUDE_KEY] = latitude
            it[GEOFENCE_LONGITUDE_KEY] = longitude
        }
    }

    suspend fun setGeofenceRadiusMeters(radius: Int) {
        context.settingsDataStore.edit { it[GEOFENCE_RADIUS_METERS_KEY] = radius }
    }

    suspend fun setGeofenceEntityId(entityId: String?) {
        context.settingsDataStore.edit {
            if (entityId == null) it.remove(GEOFENCE_ENTITY_ID_KEY) else it[GEOFENCE_ENTITY_ID_KEY] = entityId
        }
    }
}
