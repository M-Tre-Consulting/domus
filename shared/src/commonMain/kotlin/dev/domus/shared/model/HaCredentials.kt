package dev.domus.shared.model

/** How the app is authenticated against a Home Assistant instance. */
sealed interface HaCredentials {
    /** A user-generated long-lived access token (Profile > Security > Long-Lived Access Tokens). Never expires. */
    data class LongLivedToken(val token: String) : HaCredentials

    /**
     * An OAuth2 access/refresh token pair obtained by signing in through HA's own login page —
     * the same flow the official mobile apps use, so 2FA is handled entirely by HA's server-side
     * login form. The access token expires roughly every 30 minutes and is refreshed
     * transparently by `HaTokenProvider`.
     */
    data class OAuthSession(
        val accessToken: String,
        val refreshToken: String,
        val expiresAtEpochMillis: Long,
    ) : HaCredentials
}
