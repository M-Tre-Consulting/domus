package dev.domus.shared.data

import dev.domus.shared.api.HaRestApi
import dev.domus.shared.api.HaWebSocketClient
import dev.domus.shared.model.HaEntityState
import dev.domus.shared.model.HaServiceCall
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.time.Duration.Companion.milliseconds

enum class WebSocketState { Connecting, Connected, Reconnecting }

/**
 * Single source of truth for Home Assistant entity state: seeds from the REST snapshot,
 * then keeps it fresh from the WebSocket `state_changed` event stream.
 */
class HaRepository(
    private val restApi: HaRestApi,
    private val webSocketClient: HaWebSocketClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _entities = MutableStateFlow<Map<String, HaEntityState>>(emptyMap())
    val entities: StateFlow<Map<String, HaEntityState>> = _entities.asStateFlow()

    /** entityId → human-readable area name; empty until the first WS connection completes. */
    val areaEntityMap: StateFlow<Map<String, String>> = webSocketClient.areaEntityMap

    /** Short diagnostic string from the last registry fetch; for temporary in-app display. */
    val registryDiag: StateFlow<String> = webSocketClient.registryDiag

    private val _wsState = MutableStateFlow(WebSocketState.Connecting)
    val wsState: StateFlow<WebSocketState> = _wsState.asStateFlow()

    suspend fun refresh() {
        val states = restApi.getStates()
        _entities.value = states.associateBy { it.entityId }
    }

    suspend fun callService(call: HaServiceCall) {
        restApi.callService(call)
    }

    fun startRealtimeUpdates(scope: CoroutineScope) {
        scope.launch {
            var isFirstConnect = true
            while (true) {
                _wsState.value = if (isFirstConnect) WebSocketState.Connecting else WebSocketState.Reconnecting
                isFirstConnect = false
                try {
                    webSocketClient.connectAndListen(onConnected = { _wsState.value = WebSocketState.Connected })
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    println("Domus: WebSocket dropped (${e::class.simpleName}: ${e.message}), reconnecting in 5 s")
                }
                _wsState.value = WebSocketState.Reconnecting
                delay(5_000.milliseconds)
                try { refresh() } catch (_: Exception) {}
            }
        }
        scope.launch {
            webSocketClient.events.collect { event ->
                val newState = event["event"]
                    ?.jsonObject?.get("data")
                    ?.jsonObject?.get("new_state")
                    ?.takeIf { it.toString() != "null" }
                    ?: return@collect
                val entity = json.decodeFromJsonElement<HaEntityState>(newState)
                _entities.value = _entities.value + (entity.entityId to entity)
            }
        }
    }
}
