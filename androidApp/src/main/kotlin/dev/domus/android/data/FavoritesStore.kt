package dev.domus.android.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "domus_prefs")
private val FAVORITE_ENTITIES_KEY = stringSetPreferencesKey("favorite_entities")

/** Persists which entity IDs the user has chosen to show on the dashboard. */
class FavoritesStore(private val context: Context) {
    val favoriteEntityIds: Flow<Set<String>> =
        context.dataStore.data.map { prefs -> prefs[FAVORITE_ENTITIES_KEY] ?: emptySet() }

    suspend fun setFavorites(entityIds: Set<String>) {
        context.dataStore.edit { prefs -> prefs[FAVORITE_ENTITIES_KEY] = entityIds }
    }
}
