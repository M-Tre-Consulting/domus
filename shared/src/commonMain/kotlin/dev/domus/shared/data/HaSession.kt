package dev.domus.shared.data

import dev.domus.shared.api.HaRestApi
import dev.domus.shared.api.HaWebSocketClient
import dev.domus.shared.createHttpClient
import dev.domus.shared.model.HaConnectionConfig

/** Bundles the REST client, WebSocket client, and repository for one Home Assistant connection. */
class HaSession(config: HaConnectionConfig) {
    private val httpClient = createHttpClient()
    val restApi = HaRestApi(httpClient, config)
    private val webSocketClient = HaWebSocketClient(httpClient, config)
    val repository = HaRepository(restApi, webSocketClient)
}
