package dev.domus.shared.api

import dev.domus.shared.auth.HaTokenProvider
import dev.domus.shared.model.HaEntityState
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
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.time.Duration.Companion.hours

class HaApiException(message: String, val statusCode: Int? = null) : Exception(message)

/** Thin wrapper over Home Assistant's REST API (https://developers.home-assistant.io/docs/api/rest/). */
class HaRestApi(
    private val client: HttpClient,
    private val baseUrl: String,
    private val tokenProvider: HaTokenProvider,
) {
    private val json = Json { ignoreUnknownKeys = true }
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
}
