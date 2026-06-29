package dev.domus.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.domus.desktop.data.HaSessionHolder
import dev.domus.shared.api.HaOAuthClient
import dev.domus.shared.createHttpClient
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.HaConnectionConfig
import dev.domus.shared.model.HaCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.awt.Desktop
import java.net.ServerSocket
import java.net.URI

private sealed interface OAuthStatus {
    data object Opening : OAuthStatus
    data object WaitingForBrowser : OAuthStatus
    data object Exchanging : OAuthStatus
    data class Failed(val message: String) : OAuthStatus
}

/**
 * Opens the system browser to HA's login page, starts a local HTTP server to capture the
 * OAuth redirect, exchanges the auth code for tokens, then calls [onConnected].
 *
 * Uses localhost as both client_id and redirect_uri — HA allows this for native apps per
 * RFC 8252. The port is chosen by the OS (ServerSocket(0)) to avoid conflicts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OAuthLoginScreen(
    baseUrl: String,
    onConnected: (HaConnectionConfig) -> Unit,
    onCredentialsRefreshed: suspend (HaCredentials.OAuthSession) -> Unit,
    onBack: () -> Unit,
) {
    var status by remember { mutableStateOf<OAuthStatus>(OAuthStatus.Opening) }
    val httpClient = remember { createHttpClient() }

    LaunchedEffect(Unit) {
        try {
            // Keep the socket open to hold the port — avoids TOCTOU race between
            // finding a free port and starting to listen.
            val server = withContext(Dispatchers.IO) { ServerSocket(0) }
            val port = server.localPort
            val localUri = "http://localhost:$port/"

            // Both client_id and redirect_uri must be localhost: HA validates that the
            // redirect_uri is under the same origin as client_id (unless both are localhost).
            // Using localUri for both satisfies HA's same-origin rule. The client_id is stored
            // in credentials so HaSession uses the same value when refreshing the token —
            // HA ties the grant to the original client_id and rejects a mismatched refresh.
            val oauthClient = HaOAuthClient(
                client = httpClient,
                baseUrl = baseUrl,
                clientId = localUri,
                redirectUri = localUri,
            )

            withContext(Dispatchers.IO) {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI(oauthClient.authorizeUrl()))
                } else {
                    throw UnsupportedOperationException("Cannot open browser — please open this URL manually:\n${oauthClient.authorizeUrl()}")
                }
            }
            status = OAuthStatus.WaitingForBrowser

            // Wait up to 5 minutes for the browser redirect.
            val code: String? = withContext(Dispatchers.IO) {
                withTimeoutOrNull(300_000L) {
                    server.use {
                        server.accept().use { socket ->
                            val line = socket.getInputStream().bufferedReader().readLine() ?: return@use null
                            // HTTP request line: "GET /?code=XXXX&state=... HTTP/1.1"
                            val query = line.removePrefix("GET /").substringBefore(" HTTP", "").trimStart('?')
                            val code = query.split('&')
                                .mapNotNull { pair ->
                                    val (k, v) = pair.split('=', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
                                    if (k == "code") v.ifBlank { null } else null
                                }
                                .firstOrNull()
                            val body = if (code != null)
                                "<html><body><h1>Signed in to Domus!</h1><p>You can close this tab and return to the app.</p></body></html>"
                            else
                                "<html><body><h1>Sign-in cancelled</h1><p>Close this tab and try again in the app.</p></body></html>"
                            socket.getOutputStream().write(
                                "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${body.length}\r\nConnection: close\r\n\r\n$body".toByteArray()
                            )
                            code
                        }
                    }
                }
            }

            if (code == null) {
                status = OAuthStatus.Failed("Sign-in timed out or was cancelled.")
                return@LaunchedEffect
            }

            status = OAuthStatus.Exchanging
            val tokenResponse = oauthClient.exchangeCode(code)
            val refreshToken = tokenResponse.refreshToken
                ?: throw IllegalStateException("Home Assistant didn't return a refresh token.")

            val config = HaConnectionConfig.withOAuthSession(
                baseUrl = baseUrl,
                accessToken = tokenResponse.accessToken,
                refreshToken = refreshToken,
                expiresAtEpochMillis = System.currentTimeMillis() + tokenResponse.expiresIn * 1000,
                oauthClientId = localUri,
            )
            val session = HaSession(config, onCredentialsRefreshed)
            HaSessionHolder.connect(session)
            onConnected(config)
        } catch (e: Exception) {
            status = OAuthStatus.Failed(e.message ?: "Unknown error")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign in to Home Assistant") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (val s = status) {
                    OAuthStatus.Opening -> {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Text("Opening browser…", style = MaterialTheme.typography.bodyLarge)
                    }
                    OAuthStatus.WaitingForBrowser -> {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Text("Waiting for you to sign in…", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Complete sign-in in the browser window that opened.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = onBack) { Text("Cancel") }
                    }
                    OAuthStatus.Exchanging -> {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Text("Completing sign-in…", style = MaterialTheme.typography.bodyLarge)
                    }
                    is OAuthStatus.Failed -> {
                        Text(s.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = onBack) { Text("Go back") }
                    }
                }
            }
        }
    }
}
