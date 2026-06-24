package dev.domus.android.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dev.domus.shared.model.HaConnectionConfig
import dev.domus.shared.model.HaCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PREFS_FILE_NAME = "domus_connection_secure"
private const val BASE_URL_KEY = "base_url"
private const val AUTH_TYPE_KEY = "auth_type"
private const val ACCESS_TOKEN_KEY = "access_token"
private const val REFRESH_TOKEN_KEY = "refresh_token"
private const val EXPIRES_AT_KEY = "expires_at"
private const val AUTH_TYPE_TOKEN = "token"
private const val AUTH_TYPE_OAUTH = "oauth"

/**
 * Persists the active Home Assistant connection in an Android Keystore-backed
 * [EncryptedSharedPreferences] file, so neither the long-lived token nor the OAuth
 * access/refresh token pair is stored as plaintext on disk.
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
        val baseUrl = prefs.getString(BASE_URL_KEY, null) ?: return@withContext null
        val accessToken = prefs.getString(ACCESS_TOKEN_KEY, null) ?: return@withContext null
        when (prefs.getString(AUTH_TYPE_KEY, AUTH_TYPE_TOKEN)) {
            AUTH_TYPE_OAUTH -> {
                val refreshToken = prefs.getString(REFRESH_TOKEN_KEY, null) ?: return@withContext null
                val expiresAt = prefs.getLong(EXPIRES_AT_KEY, 0L)
                HaConnectionConfig.withOAuthSession(baseUrl, accessToken, refreshToken, expiresAt)
            }
            else -> HaConnectionConfig.withToken(baseUrl, accessToken)
        }
    }

    suspend fun save(config: HaConnectionConfig) = withContext(Dispatchers.IO) {
        val editor = prefs.edit().putString(BASE_URL_KEY, config.baseUrl)
        when (val credentials = config.credentials) {
            is HaCredentials.LongLivedToken -> editor
                .putString(AUTH_TYPE_KEY, AUTH_TYPE_TOKEN)
                .putString(ACCESS_TOKEN_KEY, credentials.token)
                .remove(REFRESH_TOKEN_KEY)
                .remove(EXPIRES_AT_KEY)

            is HaCredentials.OAuthSession -> editor
                .putString(AUTH_TYPE_KEY, AUTH_TYPE_OAUTH)
                .putString(ACCESS_TOKEN_KEY, credentials.accessToken)
                .putString(REFRESH_TOKEN_KEY, credentials.refreshToken)
                .putLong(EXPIRES_AT_KEY, credentials.expiresAtEpochMillis)
        }
        editor.apply()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }
}
