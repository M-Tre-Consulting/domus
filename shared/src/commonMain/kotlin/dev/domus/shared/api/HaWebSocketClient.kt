package dev.domus.shared.api

import dev.domus.shared.model.HaConnectionConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * Streams realtime state-changed events from Home Assistant's WebSocket API
 * (https://developers.home-assistant.io/docs/api/websocket/).
 */
class HaWebSocketClient(
    private val client: HttpClient,
    private val config: HaConnectionConfig,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _events = MutableSharedFlow<JsonObject>(extraBufferCapacity = 64)
    val events: SharedFlow<JsonObject> = _events.asSharedFlow()

    suspend fun connectAndListen() {
        client.webSocket(config.websocketUrl) {
            var messageId = 1

            // The first server message is always `auth_required`.
            val authRequired = json.parseToJsonElement((incoming.receive() as Frame.Text).readText()).jsonObject
            check(authRequired["type"]?.toString()?.contains("auth_required") == true) {
                "Unexpected handshake from Home Assistant"
            }

            send(Frame.Text(buildJsonObject {
                put("type", "auth")
                put("access_token", config.accessToken)
            }.toString()))

            val authResult = json.parseToJsonElement((incoming.receive() as Frame.Text).readText()).jsonObject
            if (authResult["type"]?.toString()?.contains("auth_ok") != true) {
                close()
                error("Home Assistant authentication failed")
            }

            send(Frame.Text(buildJsonObject {
                put("id", messageId++)
                put("type", "subscribe_events")
                put("event_type", "state_changed")
            }.toString()))

            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val message = json.parseToJsonElement(frame.readText()).jsonObject
                    _events.emit(message)
                }
            }
        }
    }
}
