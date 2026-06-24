package dev.domus.shared.api

import dev.domus.shared.model.HaConnectionConfig
import dev.domus.shared.model.HaEntityState
import dev.domus.shared.model.HaServiceCall
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class HaApiException(message: String, val statusCode: Int? = null) : Exception(message)

/** Thin wrapper over Home Assistant's REST API (https://developers.home-assistant.io/docs/api/rest/). */
class HaRestApi(
    private val client: HttpClient,
    private val config: HaConnectionConfig,
) {
    suspend fun getStates(): List<HaEntityState> {
        val response = client.get("${config.baseUrl}/api/states") {
            header("Authorization", "Bearer ${config.accessToken}")
        }
        if (!response.status.isSuccess()) {
            throw HaApiException("Failed to fetch states", response.status.value)
        }
        return response.body()
    }

    suspend fun getState(entityId: String): HaEntityState {
        val response = client.get("${config.baseUrl}/api/states/$entityId") {
            header("Authorization", "Bearer ${config.accessToken}")
        }
        if (!response.status.isSuccess()) {
            throw HaApiException("Failed to fetch state for $entityId", response.status.value)
        }
        return response.body()
    }

    suspend fun callService(call: HaServiceCall): List<HaEntityState> {
        val body = buildMap<String, JsonElement> {
            call.entityId?.let { put("entity_id", JsonPrimitive(it)) }
            putAll(call.data)
        }
        val response = client.post("${config.baseUrl}/api/services/${call.domain}/${call.service}") {
            header("Authorization", "Bearer ${config.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(JsonObject(body))
        }
        if (!response.status.isSuccess()) {
            throw HaApiException("Failed to call ${call.domain}.${call.service}", response.status.value)
        }
        return response.body()
    }

    suspend fun checkConnection(): Boolean {
        val response = client.get("${config.baseUrl}/api/") {
            header("Authorization", "Bearer ${config.accessToken}")
        }
        return response.status.isSuccess()
    }
}
