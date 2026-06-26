package dev.domus.shared.api

import dev.domus.shared.auth.HaTokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * Streams realtime state-changed events from Home Assistant's WebSocket API
 * (https://developers.home-assistant.io/docs/api/websocket/).
 *
 * After authenticating, it fetches the area/device/entity registries to build a
 * stable [entityId → areaName] map before entering the event loop. This lets the
 * UI group entities by room without any extra round-trips later.
 *
 * Registry responses are matched by their request ID rather than assumed to arrive
 * in order, so interleaved messages from HA plugins or cloud extensions don't
 * de-sync the reader.
 */
class HaWebSocketClient(
    private val client: HttpClient,
    private val websocketUrl: String,
    private val tokenProvider: HaTokenProvider,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _events = MutableSharedFlow<JsonObject>(extraBufferCapacity = 64)
    val events: SharedFlow<JsonObject> = _events.asSharedFlow()

    private val _areaEntityMap = MutableStateFlow<Map<String, String>>(emptyMap())
    /** Maps entityId → human-readable area name. Empty until the first WS connection completes registry fetch. */
    val areaEntityMap: StateFlow<Map<String, String>> = _areaEntityMap.asStateFlow()

    private val _registryDiag = MutableStateFlow("connecting…")
    /** Short diagnostic string from the last registry fetch; shown in the dashboard title bar. */
    val registryDiag: StateFlow<String> = _registryDiag.asStateFlow()

    suspend fun connectAndListen() {
        client.webSocket(websocketUrl) {
            var messageId = 1

            val authRequired = json.parseToJsonElement((incoming.receive() as Frame.Text).readText()).jsonObject
            check(authRequired["type"]?.toString()?.contains("auth_required") == true) {
                "Unexpected handshake from Home Assistant"
            }

            send(Frame.Text(buildJsonObject {
                put("type", "auth")
                put("access_token", tokenProvider.accessToken())
            }.toString()))

            val authResult = json.parseToJsonElement((incoming.receive() as Frame.Text).readText()).jsonObject
            if (authResult["type"]?.toString()?.contains("auth_ok") != true) {
                close()
                error("Home Assistant authentication failed")
            }

            // --- Registry fetches (one-shot, before the subscription loop) ---
            // Uses ID-based response matching: any interleaved messages that arrive before
            // our expected response are skipped rather than de-syncing the sequential reader.
            val areasJson = fetchRegistry("config/area_registry/list", messageId++)
            val devicesJson = fetchRegistry("config/device_registry/list", messageId++)
            val entityRegJson = fetchRegistry("config/entity_registry/list", messageId++)

            val map = buildAreaEntityMap(areasJson, devicesJson, entityRegJson)
            val diag = "a=${areasJson.size} d=${devicesJson.size} e=${entityRegJson.size} → ${map.size} pairs"
            println("Domus: registry fetch done — $diag")
            _registryDiag.value = diag
            _areaEntityMap.value = map

            // --- Realtime event subscription ---
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

    /**
     * Sends a one-shot registry list command and waits up to 5 s for the response that
     * carries the matching [id]. Any frames that arrive first with a different ID are
     * logged and skipped — they will be consumed by the event loop once it starts.
     */
    private suspend fun DefaultWebSocketSession.fetchRegistry(commandType: String, id: Int): JsonArray {
        send(Frame.Text(buildJsonObject {
            put("id", id)
            put("type", commandType)
        }.toString()))
        return try {
            withTimeout(5_000) {
                while (true) {
                    val frame = incoming.receive()
                    if (frame !is Frame.Text) continue
                    val msg = runCatching {
                        json.parseToJsonElement(frame.readText()).jsonObject
                    }.getOrNull() ?: continue

                    val msgId = (msg["id"] as? JsonPrimitive)?.intOrNull
                    if (msgId != id) {
                        println("Domus: skipped out-of-order message id=$msgId while waiting for $commandType (id=$id)")
                        continue
                    }
                    val success = msg["success"]?.toString()?.contains("true") == true
                    if (!success) {
                        println("Domus: $commandType (id=$id) returned error: ${msg["error"]}")
                        return@withTimeout JsonArray(emptyList())
                    }
                    val result = msg["result"] as? JsonArray
                    if (result == null) {
                        println("Domus: $commandType (id=$id) result was not a JSON array: ${msg["result"]?.toString()?.take(120)}")
                        return@withTimeout JsonArray(emptyList())
                    }
                    println("Domus: $commandType (id=$id) → ${result.size} entries")
                    return@withTimeout result
                }
                @Suppress("UNREACHABLE_CODE")
                JsonArray(emptyList())
            }
        } catch (e: TimeoutCancellationException) {
            println("Domus: $commandType (id=$id) timed out after 5 s")
            JsonArray(emptyList())
        } catch (e: Exception) {
            println("Domus: $commandType (id=$id) failed: ${e::class.simpleName}: ${e.message}")
            JsonArray(emptyList())
        }
    }

    /**
     * Combines the three registry arrays into a flat [entityId → areaName] map.
     * Entity-level area assignment takes priority; device-level area is the fallback.
     */
    private fun buildAreaEntityMap(
        areasJson: JsonArray,
        devicesJson: JsonArray,
        entityRegJson: JsonArray,
    ): Map<String, String> {
        val areas: Map<String, String> = areasJson.mapNotNull { elem ->
            val obj = elem as? JsonObject ?: return@mapNotNull null
            val id = (obj["area_id"] as? JsonPrimitive)?.content ?: return@mapNotNull null
            val name = (obj["name"] as? JsonPrimitive)?.content ?: return@mapNotNull null
            id to name
        }.toMap()

        val deviceAreas: Map<String, String> = devicesJson.mapNotNull { elem ->
            val obj = elem as? JsonObject ?: return@mapNotNull null
            val id = (obj["id"] as? JsonPrimitive)?.content ?: return@mapNotNull null
            // area_id is JSON null for devices not assigned to an area; JsonNull is a JsonPrimitive
            // whose .content = "null", so we must guard against it explicitly.
            val areaId = (obj["area_id"] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
                ?: return@mapNotNull null
            id to areaId
        }.toMap()

        return entityRegJson.mapNotNull { elem ->
            val obj = elem as? JsonObject ?: return@mapNotNull null
            val entityId = (obj["entity_id"] as? JsonPrimitive)?.content ?: return@mapNotNull null
            // Entity area_id is JSON null when the area is inherited from the device, not set directly.
            // Without the JsonNull guard, .content = "null" (string) blocks the device fallback.
            val areaId = (obj["area_id"] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
                ?: (obj["device_id"] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
                    ?.let { deviceAreas[it] }
                ?: return@mapNotNull null
            val areaName = areas[areaId] ?: return@mapNotNull null
            entityId to areaName
        }.toMap()
    }
}
