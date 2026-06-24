package dev.domus.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** A single Home Assistant entity state, as returned by `/api/states`. */
@Serializable
data class HaEntityState(
    @SerialName("entity_id") val entityId: String,
    val state: String,
    val attributes: JsonObject = JsonObject(emptyMap()),
    @SerialName("last_changed") val lastChanged: String? = null,
    @SerialName("last_updated") val lastUpdated: String? = null,
) {
    val domain: String get() = entityId.substringBefore('.')

    fun attribute(name: String): JsonElement? = attributes[name]
}

/** Connection details for a Home Assistant instance. */
data class HaConnectionConfig(
    val baseUrl: String,
    val accessToken: String,
) {
    val websocketUrl: String
        get() = baseUrl.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://") + "/api/websocket"

    companion object {
        /** Normalizes a user-entered URL (trims whitespace and any trailing slash). */
        fun of(baseUrl: String, accessToken: String) =
            HaConnectionConfig(baseUrl.trim().trimEnd('/'), accessToken.trim())
    }
}

/** A request to invoke a Home Assistant service (e.g. `light.turn_on`). */
data class HaServiceCall(
    val domain: String,
    val service: String,
    val entityId: String? = null,
    val data: Map<String, JsonElement> = emptyMap(),
)
