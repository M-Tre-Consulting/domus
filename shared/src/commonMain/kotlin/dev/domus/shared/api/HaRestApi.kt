package dev.domus.shared.api

import dev.domus.shared.auth.HaTokenProvider
import dev.domus.shared.model.HaConversationResponse
import dev.domus.shared.model.HaEntityState
import dev.domus.shared.model.HaForecastEntry
import dev.domus.shared.model.HaHistoryPoint
import dev.domus.shared.model.HaServiceCall
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.time.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.time.Duration.Companion.hours

class HaApiException(message: String, val statusCode: Int? = null) : Exception(message)

/** Thin wrapper over Home Assistant's REST API (https://developers.home-assistant.io/docs/api/rest/). */
class HaRestApi(
    private val client: HttpClient,
    private val baseUrl: String,
    private val tokenProvider: HaTokenProvider,
) {
    private val json = Json { ignoreUnknownKeys = true }
    suspend fun getState(entityId: String): HaEntityState {
        val response = client.get("$baseUrl/api/states/$entityId") {
            header("Authorization", "Bearer ${tokenProvider.accessToken()}")
        }
        if (!response.status.isSuccess()) throw HaApiException("Failed to fetch state: $entityId", response.status.value)
        return response.body()
    }

    suspend fun getStates(): List<HaEntityState> {
        val response = client.get("$baseUrl/api/states") {
            header("Authorization", "Bearer ${tokenProvider.accessToken()}")
        }
        if (!response.status.isSuccess()) {
            throw HaApiException("Failed to fetch states", response.status.value)
        }
        return response.body()
    }

    suspend fun callService(call: HaServiceCall): List<HaEntityState> {
        val body = buildMap<String, JsonElement> {
            call.entityId?.let { put("entity_id", JsonPrimitive(it)) }
            putAll(call.data)
        }
        val response = client.post("$baseUrl/api/services/${call.domain}/${call.service}") {
            header("Authorization", "Bearer ${tokenProvider.accessToken()}")
            contentType(ContentType.Application.Json)
            setBody(JsonObject(body))
        }
        if (!response.status.isSuccess()) {
            throw HaApiException("Failed to call ${call.domain}.${call.service}", response.status.value)
        }
        return response.body()
    }

    /** Returns state history for [entityId] over the last [hours] hours (default 24).
     *  Returns an empty list on any error so callers can show "no data" gracefully. */
    suspend fun getHistory(entityId: String, hours: Int = 24): List<HaHistoryPoint> {
        val now = Clock.System.now()
        val startTime = now - hours.hours
        return try {
            val response = client.get("$baseUrl/api/history/period/$startTime") {
                header("Authorization", "Bearer ${tokenProvider.accessToken()}")
                parameter("filter_entity_id", entityId)
                parameter("end_time", now.toString())
                parameter("minimal_response", "true")
                parameter("no_attributes", "true")
            }
            if (!response.status.isSuccess()) return emptyList()
            val raw = response.body<JsonElement>()
            val innerList = (raw as? JsonArray)?.firstOrNull() as? JsonArray ?: return emptyList()
            json.decodeFromJsonElement<List<HaHistoryPoint>>(innerList)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Calls `weather.get_forecasts` (the modern replacement for the old `forecast` state
     *  attribute, removed in HA 2023.9) and returns the entries for [entityId]. [type] is
     *  "hourly", "daily" or "twice_daily" - not every weather integration supports every
     *  type, so an unsupported request just yields an empty list rather than throwing. */
    suspend fun getForecast(entityId: String, type: String): List<HaForecastEntry> {
        return try {
            val response = client.post("$baseUrl/api/services/weather/get_forecasts") {
                header("Authorization", "Bearer ${tokenProvider.accessToken()}")
                parameter("return_response", "true")
                contentType(ContentType.Application.Json)
                setBody(JsonObject(mapOf("entity_id" to JsonPrimitive(entityId), "type" to JsonPrimitive(type))))
            }
            if (!response.status.isSuccess()) return emptyList()
            val raw = response.body<JsonObject>()
            val forecastArray = raw["service_response"]?.jsonObject
                ?.get(entityId)?.jsonObject
                ?.get("forecast") as? JsonArray
                ?: return emptyList()
            json.decodeFromJsonElement<List<HaForecastEntry>>(forecastArray)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Sends [text] to HA's conversation agent (Assist) via `conversation.process` and returns
     *  its reply. Pass back the previous [conversationId] on follow-up turns so the agent keeps
     *  context; omit it to start a fresh conversation. Never throws - network/parse failures
     *  come back as a [HaConversationResponse] with a null [HaConversationResponse.speech] and
     *  a populated [HaConversationResponse.error] so the chat UI can show a failure bubble. */
    suspend fun converse(text: String, conversationId: String?, language: String?): HaConversationResponse {
        return try {
            val body = buildMap<String, JsonElement> {
                put("text", JsonPrimitive(text))
                conversationId?.let { put("conversation_id", JsonPrimitive(it)) }
                language?.let { put("language", JsonPrimitive(it)) }
            }
            val response = client.post("$baseUrl/api/services/conversation/process") {
                header("Authorization", "Bearer ${tokenProvider.accessToken()}")
                parameter("return_response", "true")
                contentType(ContentType.Application.Json)
                setBody(JsonObject(body))
            }
            if (!response.status.isSuccess()) {
                return HaConversationResponse(speech = null, conversationId = null, error = "HTTP ${response.status.value}")
            }
            val raw = response.body<JsonObject>()
            val serviceResponse = raw["service_response"]?.jsonObject
            val speech = serviceResponse
                ?.get("response")?.jsonObject
                ?.get("speech")?.jsonObject
                ?.get("plain")?.jsonObject
                ?.get("speech") as? JsonPrimitive
            val newConversationId = serviceResponse?.get("conversation_id") as? JsonPrimitive
            HaConversationResponse(
                speech = speech?.contentOrNull,
                conversationId = newConversationId?.contentOrNull,
            )
        } catch (e: Exception) {
            HaConversationResponse(speech = null, conversationId = null, error = e.message)
        }
    }

    /**
     * Returns true if the server responded successfully.
     * Throws [HaApiException] with status 401/403 when credentials are invalid (caller
     * should prompt re-login). Other network/HTTP errors propagate as plain exceptions so
     * callers can distinguish "auth rejected" from "server temporarily unreachable."
     */
    suspend fun checkConnection(): Boolean {
        val response = client.get("$baseUrl/api/") {
            header("Authorization", "Bearer ${tokenProvider.accessToken()}")
        }
        if (response.status.value == 401 || response.status.value == 403) {
            throw HaApiException("Authentication failed", response.status.value)
        }
        return response.status.isSuccess()
    }

    /** A single still JPEG frame from a `camera.*` entity's snapshot proxy. */
    suspend fun getCameraSnapshot(entityId: String): ByteArray {
        val response = client.get("$baseUrl/api/camera_proxy/$entityId") {
            header("Authorization", "Bearer ${tokenProvider.accessToken()}")
        }
        if (!response.status.isSuccess()) {
            throw HaApiException("Failed to fetch camera snapshot: $entityId", response.status.value)
        }
        return response.body()
    }
}
