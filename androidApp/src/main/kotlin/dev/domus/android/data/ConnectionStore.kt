package dev.domus.android.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.domus.shared.model.HaConnectionConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.connectionDataStore by preferencesDataStore(name = "domus_connection")
private val BASE_URL_KEY = stringPreferencesKey("base_url")
private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")

/**
 * Persists the active Home Assistant connection so the app doesn't ask to log in on every
 * launch. The token is stored as plain DataStore preferences — fine for a local scaffold,
 * but should move to `androidx.security` EncryptedSharedPreferences before any real use.
 */
class ConnectionStore(private val context: Context) {
    val connectionConfig: Flow<HaConnectionConfig?> =
        context.connectionDataStore.data.map { prefs ->
            val baseUrl = prefs[BASE_URL_KEY]
            val accessToken = prefs[ACCESS_TOKEN_KEY]
            if (baseUrl != null && accessToken != null) {
                HaConnectionConfig(baseUrl, accessToken)
            } else {
                null
            }
        }

    suspend fun save(config: HaConnectionConfig) {
        context.connectionDataStore.edit { prefs ->
            prefs[BASE_URL_KEY] = config.baseUrl
            prefs[ACCESS_TOKEN_KEY] = config.accessToken
        }
    }

    suspend fun clear() {
        context.connectionDataStore.edit { it.clear() }
    }
}
