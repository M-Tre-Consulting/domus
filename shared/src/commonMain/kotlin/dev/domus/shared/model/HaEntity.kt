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
    val credentials: HaCredentials,
) {
    val websocketUrl: String
        get() = baseUrl.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://") + "/api/websocket"

    companion object {
        /** Normalizes a user-entered URL (trims whitespace and any trailing slash). */
        fun withToken(baseUrl: String, accessToken: String) =
            HaConnectionConfig(baseUrl.trim().trimEnd('/'), HaCredentials.LongLivedToken(accessToken.trim()))

        fun withOAuthSession(baseUrl: String, accessToken: String, refreshToken: String, expiresAtEpochMillis: Long) =
            HaConnectionConfig(
                baseUrl.trim().trimEnd('/'),
                HaCredentials.OAuthSession(accessToken, refreshToken, expiresAtEpochMillis),
            )
    }
}

/** A single point from the `/api/history` endpoint (minimal_response mode). */
@Serializable
data class HaHistoryPoint(
    @SerialName("entity_id") val entityId: String = "",
    val state: String,
    @SerialName("last_changed") val lastChanged: String,
)

/** A request to invoke a Home Assistant service (e.g. `light.turn_on`). */
data class HaServiceCall(
    val domain: String,
    val service: String,
    val entityId: String? = null,
    val data: Map<String, JsonElement> = emptyMap(),
)
