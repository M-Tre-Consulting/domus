package dev.domus.shared.data

import dev.domus.shared.api.HaRestApi
import dev.domus.shared.api.HaWebSocketClient
import dev.domus.shared.model.HaEntityState
import dev.domus.shared.model.HaServiceCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

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

    suspend fun refresh() {
        val states = restApi.getStates()
        _entities.value = states.associateBy { it.entityId }
    }

    suspend fun callService(call: HaServiceCall) {
        restApi.callService(call)
    }

    fun startRealtimeUpdates(scope: CoroutineScope) {
        scope.launch { webSocketClient.connectAndListen() }
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
