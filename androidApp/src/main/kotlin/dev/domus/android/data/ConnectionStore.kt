package dev.domus.android.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.domus.shared.model.HaConnectionConfig
import dev.domus.shared.model.HaCredentials
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val DATASTORE_NAME = "domus_connection"
private const val KEY_ALIAS = "domus_connection_key"
private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val AES_GCM_NOPADDING = "AES/GCM/NoPadding"

private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

private val BASE_URL_KEY = stringPreferencesKey("base_url")
private val AUTH_TYPE_KEY = stringPreferencesKey("auth_type")
private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
private val EXPIRES_AT_KEY = longPreferencesKey("expires_at")
private val OAUTH_CLIENT_ID_KEY = stringPreferencesKey("oauth_client_id")

private const val AUTH_TYPE_TOKEN = "token"
private const val AUTH_TYPE_OAUTH = "oauth"

/**
 * Persists the active Home Assistant connection in a [DataStore] file.
 * Sensitive fields (tokens) are encrypted using a hardware-backed key from the Android Keystore.
 */
class ConnectionStore(private val context: Context) {

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val ciphertext = cipher.doFinal(plaintext.encodeToByteArray())
        val iv = cipher.iv
        // Store as IV|Ciphertext
        return Base64.encodeToString(iv + ciphertext, Base64.NO_WRAP)
    }

    private fun decrypt(encryptedBase64: String): String? {
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val ivSize = 12 // GCM default IV size
            val iv = combined.sliceArray(0 until ivSize)
            val ciphertext = combined.sliceArray(ivSize until combined.size)

            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
            String(cipher.doFinal(ciphertext))
        } catch (_: Exception) {
            null
        }
    }

    suspend fun read(): HaConnectionConfig? = context.dataStore.data.map { prefs ->
        val baseUrl = prefs[BASE_URL_KEY] ?: return@map null
        val encryptedAccessToken = prefs[ACCESS_TOKEN_KEY] ?: return@map null
        val accessToken = decrypt(encryptedAccessToken) ?: return@map null

        when (prefs[AUTH_TYPE_KEY]) {
            AUTH_TYPE_OAUTH -> {
                val encryptedRefreshToken = prefs[REFRESH_TOKEN_KEY] ?: return@map null
                val refreshToken = decrypt(encryptedRefreshToken) ?: return@map null
                val expiresAt = prefs[EXPIRES_AT_KEY] ?: 0L
                val oauthClientId = prefs[OAUTH_CLIENT_ID_KEY]
                HaConnectionConfig.withOAuthSession(baseUrl, accessToken, refreshToken, expiresAt, oauthClientId)
            }
            else -> HaConnectionConfig.withToken(baseUrl, accessToken)
        }
    }.first()

    suspend fun save(config: HaConnectionConfig) {
        context.dataStore.edit { prefs ->
            prefs[BASE_URL_KEY] = config.baseUrl
            when (val credentials = config.credentials) {
                is HaCredentials.LongLivedToken -> {
                    prefs[AUTH_TYPE_KEY] = AUTH_TYPE_TOKEN
                    prefs[ACCESS_TOKEN_KEY] = encrypt(credentials.token)
                    prefs.remove(REFRESH_TOKEN_KEY)
                    prefs.remove(EXPIRES_AT_KEY)
                }
                is HaCredentials.OAuthSession -> {
                    prefs[AUTH_TYPE_KEY] = AUTH_TYPE_OAUTH
                    prefs[ACCESS_TOKEN_KEY] = encrypt(credentials.accessToken)
                    prefs[REFRESH_TOKEN_KEY] = encrypt(credentials.refreshToken)
                    prefs[EXPIRES_AT_KEY] = credentials.expiresAtEpochMillis
                    credentials.oauthClientId?.let { prefs[OAUTH_CLIENT_ID_KEY] = it }
                        ?: prefs.remove(OAUTH_CLIENT_ID_KEY)
                }
            }
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
