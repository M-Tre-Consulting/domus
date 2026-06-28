package dev.domus.desktop.data

import dev.domus.shared.model.HaConnectionConfig
import dev.domus.shared.model.HaCredentials
import java.util.prefs.Preferences

private val PREFS: Preferences = Preferences.userRoot().node("dev/domus/connection")

/** Persists the active Home Assistant connection using the JVM Preferences API. */
class ConnectionStore {
    fun read(): HaConnectionConfig? {
        val baseUrl = PREFS.get("base_url", null) ?: return null
        val accessToken = PREFS.get("access_token", null) ?: return null
        return when (PREFS.get("auth_type", null)) {
            "oauth" -> {
                val refreshToken = PREFS.get("refresh_token", null) ?: return null
                val expiresAt = PREFS.getLong("expires_at", 0L)
                HaConnectionConfig.withOAuthSession(baseUrl, accessToken, refreshToken, expiresAt)
            }
            else -> HaConnectionConfig.withToken(baseUrl, accessToken)
        }
    }

    fun save(config: HaConnectionConfig) {
        PREFS.put("base_url", config.baseUrl)
        when (val creds = config.credentials) {
            is HaCredentials.LongLivedToken -> {
                PREFS.put("auth_type", "token")
                PREFS.put("access_token", creds.token)
                PREFS.remove("refresh_token")
                PREFS.remove("expires_at")
            }
            is HaCredentials.OAuthSession -> {
                PREFS.put("auth_type", "oauth")
                PREFS.put("access_token", creds.accessToken)
                PREFS.put("refresh_token", creds.refreshToken)
                PREFS.putLong("expires_at", creds.expiresAtEpochMillis)
            }
        }
        PREFS.flush()
    }

    fun clear() {
        PREFS.clear()
        PREFS.flush()
    }
}
