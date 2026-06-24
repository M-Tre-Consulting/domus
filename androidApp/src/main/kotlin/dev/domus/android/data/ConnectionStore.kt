package dev.domus.android.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dev.domus.shared.model.HaConnectionConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PREFS_FILE_NAME = "domus_connection_secure"
private const val BASE_URL_KEY = "base_url"
private const val ACCESS_TOKEN_KEY = "access_token"

/**
 * Persists the active Home Assistant connection in an Android Keystore-backed
 * [EncryptedSharedPreferences] file, so the long-lived access token isn't stored as
 * plaintext on disk.
 */
class ConnectionStore(private val context: Context) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    suspend fun read(): HaConnectionConfig? = withContext(Dispatchers.IO) {
        val baseUrl = prefs.getString(BASE_URL_KEY, null)
        val accessToken = prefs.getString(ACCESS_TOKEN_KEY, null)
        if (baseUrl != null && accessToken != null) HaConnectionConfig(baseUrl, accessToken) else null
    }

    suspend fun save(config: HaConnectionConfig) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString(BASE_URL_KEY, config.baseUrl)
            .putString(ACCESS_TOKEN_KEY, config.accessToken)
            .apply()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }
}
