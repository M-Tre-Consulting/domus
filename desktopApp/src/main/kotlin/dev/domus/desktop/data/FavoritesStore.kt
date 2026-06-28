package dev.domus.desktop.data

import java.util.prefs.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val PREFS: Preferences = Preferences.userRoot().node("dev/domus/favorites")

/** Persists which entity IDs the user has chosen to show on the dashboard. */
class FavoritesStore {
    private val _favoriteEntityIds = MutableStateFlow(load())
    val favoriteEntityIds: StateFlow<Set<String>> = _favoriteEntityIds.asStateFlow()

    private fun load(): Set<String> {
        val raw = PREFS.get("favorite_entities", "") ?: return emptySet()
        return raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    fun setFavorites(entityIds: Set<String>) {
        PREFS.put("favorite_entities", entityIds.joinToString(","))
        PREFS.flush()
        _favoriteEntityIds.value = entityIds
    }
}
