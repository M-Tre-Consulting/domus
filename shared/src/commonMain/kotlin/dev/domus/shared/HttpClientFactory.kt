package dev.domus.shared

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Platform-specific Ktor engine (OkHttp on Android, CIO on Desktop). */
expect fun httpClientEngine(): HttpClient

fun createHttpClient(): HttpClient = httpClientEngine().config {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(WebSockets) {
        // Keep connection alive through WiFi NAT timeouts, which are typically
        // 30-120 s on consumer routers but much longer on mobile carrier infra.
        pingIntervalMillis = 20_000
    }
    install(Logging) {
        level = LogLevel.INFO
    }
}
