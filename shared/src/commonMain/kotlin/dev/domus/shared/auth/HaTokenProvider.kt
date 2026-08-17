package dev.domus.shared.auth

import dev.domus.shared.api.HaOAuthClient
import dev.domus.shared.api.HaOAuthException
import dev.domus.shared.model.HaCredentials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

private const val REFRESH_BUFFER_MILLIS = 60_000L

/**
 * Resolves a valid bearer token for REST/WebSocket calls. For [HaCredentials.LongLivedToken]
 * this just returns the static token; for [HaCredentials.OAuthSession] it transparently
 * refreshes the access token shortly before it expires and reports the new credentials back
 * via [onRefreshed] so the caller can persist them.
 */
class HaTokenProvider(
    private val oauthClient: HaOAuthClient,
    initialCredentials: HaCredentials,
    private val onRefreshed: suspend (HaCredentials.OAuthSession) -> Unit = {},
) {
    private var credentials: HaCredentials = initialCredentials
    private val mutex = Mutex()

    private val _authExpired = MutableStateFlow(false)
    /** True once the server has rejected our refresh_token — the session cannot recover on
     *  its own and needs a fresh interactive login. */
    val authExpired: StateFlow<Boolean> = _authExpired.asStateFlow()

    suspend fun accessToken(): String = mutex.withLock {
        when (val current = credentials) {
            is HaCredentials.LongLivedToken -> current.token
            is HaCredentials.OAuthSession -> {
                val now = Clock.System.now().toEpochMilliseconds()
                if (now < current.expiresAtEpochMillis - REFRESH_BUFFER_MILLIS) {
                    current.accessToken
                } else {
                    val response = try {
                        oauthClient.refresh(current.refreshToken)
                    } catch (e: HaOAuthException) {
                        _authExpired.value = true
                        throw e
                    }
                    val refreshed = HaCredentials.OAuthSession(
                        accessToken = response.accessToken,
                        refreshToken = response.refreshToken ?: current.refreshToken,
                        expiresAtEpochMillis = now + response.expiresIn * 1000,
                        oauthClientId = current.oauthClientId,
                    )
                    credentials = refreshed
                    _authExpired.value = false
                    onRefreshed(refreshed)
                    refreshed.accessToken
                }
            }
        }
    }
}
