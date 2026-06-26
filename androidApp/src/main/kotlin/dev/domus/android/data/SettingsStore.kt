package dev.domus.android.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "domus_settings")

/** Global app settings managed by Jetpack DataStore. */
class SettingsStore(private val context: Context) {
    companion object {
        private val SHOW_DEBUG_DIAG_KEY = booleanPreferencesKey("show_debug_diag")
        private val USE_HAPTIC_FEEDBACK_KEY = booleanPreferencesKey("use_haptic_feedback")
    }

    val showDebugDiag: Flow<Boolean> = context.settingsDataStore.data.map { it[SHOW_DEBUG_DIAG_KEY] ?: true }
    val useHapticFeedback: Flow<Boolean> = context.settingsDataStore.data.map { it[USE_HAPTIC_FEEDBACK_KEY] ?: true }

    suspend fun setShowDebugDiag(show: Boolean) {
        context.settingsDataStore.edit { it[SHOW_DEBUG_DIAG_KEY] = show }
    }

    suspend fun setUseHapticFeedback(use: Boolean) {
        context.settingsDataStore.edit { it[USE_HAPTIC_FEEDBACK_KEY] = use }
    }
}
