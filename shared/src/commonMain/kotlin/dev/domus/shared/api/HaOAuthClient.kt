package dev.domus.shared.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.URLBuilder
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HaTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("token_type") val tokenType: String = "Bearer",
)

class HaOAuthException(message: String) : Exception(message)

/**
 * Implements Home Assistant's OAuth2 authorization-code flow
 * (https://developers.home-assistant.io/docs/auth_api/), the same one the official mobile
 * apps use — the login form (including any 2FA) is HA's own server-rendered page, loaded in
 * a WebView; this client only handles the code-for-token exchange and refresh.
 *
 * [clientId] and [redirectUri] are set to the instance's own base URL so Home Assistant's
 * same-origin trust rule applies (a client redirecting back to its own host is implicitly
 * trusted) — no app registration or hosting a real client_id page is required. The redirect
 * never actually needs to resolve server-side: the host app intercepts the navigation to
 * [redirectUri] before it hits the network and extracts the `code` query parameter.
 */
class HaOAuthClient(
    private val client: HttpClient,
    val baseUrl: String,
) {
    val clientId: String = "$baseUrl/"
    val redirectUri: String = "$baseUrl/auth/external/callback"

    fun authorizeUrl(): String {
        val builder = URLBuilder("$baseUrl/auth/authorize")
        builder.parameters.append("response_type", "code")
        builder.parameters.append("client_id", clientId)
        builder.parameters.append("redirect_uri", redirectUri)
        return builder.buildString()
    }

    suspend fun exchangeCode(code: String): HaTokenResponse {
        val response = client.submitForm(
            url = "$baseUrl/auth/token",
            formParameters = parameters {
                append("grant_type", "authorization_code")
                append("code", code)
                append("client_id", clientId)
            },
        )
        if (!response.status.isSuccess()) throw HaOAuthException("Token exchange failed: ${response.status}")
        return response.body()
    }

    suspend fun refresh(refreshToken: String): HaTokenResponse {
        val response = client.submitForm(
            url = "$baseUrl/auth/token",
            formParameters = parameters {
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
                append("client_id", clientId)
            },
        )
        if (!response.status.isSuccess()) throw HaOAuthException("Token refresh failed: ${response.status}")
        return response.body()
    }
}
