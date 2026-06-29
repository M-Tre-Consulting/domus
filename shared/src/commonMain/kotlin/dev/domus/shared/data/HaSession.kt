package dev.domus.shared.data

import dev.domus.shared.api.HaOAuthClient
import dev.domus.shared.api.HaRestApi
import dev.domus.shared.api.HaWebSocketClient
import dev.domus.shared.auth.HaTokenProvider
import dev.domus.shared.createHttpClient
import dev.domus.shared.model.HaConnectionConfig
import dev.domus.shared.model.HaCredentials

/**
 * Bundles the REST client, WebSocket client, and repository for one Home Assistant
 * connection. [onCredentialsRefreshed] fires whenever an OAuth access token is silently
 * renewed, so the caller can persist the new token pair; it's a no-op for long-lived tokens.
 */
class HaSession(
    config: HaConnectionConfig,
    onCredentialsRefreshed: suspend (HaCredentials.OAuthSession) -> Unit = {},
) {
    private val httpClient = createHttpClient()
    private val tokenProvider = HaTokenProvider(
        oauthClient = HaOAuthClient(
            client = httpClient,
            baseUrl = config.baseUrl,
            clientId = (config.credentials as? HaCredentials.OAuthSession)?.oauthClientId
                ?: "${config.baseUrl}/",
        ),
        initialCredentials = config.credentials,
        onRefreshed = onCredentialsRefreshed,
    )
    val restApi = HaRestApi(httpClient, config.baseUrl, tokenProvider)
    private val webSocketClient = HaWebSocketClient(httpClient, config.websocketUrl, tokenProvider)
    val repository = HaRepository(restApi, webSocketClient)
}
