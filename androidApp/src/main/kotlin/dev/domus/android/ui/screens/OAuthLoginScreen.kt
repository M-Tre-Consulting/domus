package dev.domus.android.ui.screens

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import dev.domus.android.data.HaSessionHolder
import dev.domus.shared.api.HaOAuthClient
import dev.domus.shared.createHttpClient
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.HaConnectionConfig
import dev.domus.shared.model.HaCredentials
import kotlinx.coroutines.launch

/**
 * Renders Home Assistant's own login page in a WebView, the same approach the official
 * mobile apps use — HA's server handles the credential form and any 2FA prompt natively, so
 * this screen only needs to intercept the final redirect and exchange the auth code for an
 * access/refresh token pair.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OAuthLoginScreen(
    baseUrl: String,
    onConnected: (HaConnectionConfig) -> Unit,
    onCredentialsRefreshed: suspend (HaCredentials.OAuthSession) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val httpClient = remember { createHttpClient() }
    val oauthClient = remember { HaOAuthClient(httpClient, baseUrl) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign in to Home Assistant") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(padding),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val url = request.url.toString()
                            // Fix: Use exact match for the redirect URI path (ignoring query params)
                            // "callbacks".startsWith("callback") was true, causing issues with external providers.
                            val redirectUri = oauthClient.redirectUri
                            val isRedirect = url.split('?').first() == redirectUri
                            if (!isRedirect) return false

                            val code = request.url.getQueryParameter("code")
                            scope.launch {
                                if (code == null) {
                                    snackbarHostState.showSnackbar("Sign-in was cancelled or denied.")
                                    return@launch
                                }
                                try {
                                    val tokenResponse = oauthClient.exchangeCode(code)
                                    val refreshToken = tokenResponse.refreshToken
                                        ?: throw IllegalStateException("Home Assistant didn't return a refresh token")
                                    val config = HaConnectionConfig.withOAuthSession(
                                        baseUrl = baseUrl,
                                        accessToken = tokenResponse.accessToken,
                                        refreshToken = refreshToken,
                                        expiresAtEpochMillis = System.currentTimeMillis() + tokenResponse.expiresIn * 1000,
                                    )
                                    val session = HaSession(config, onCredentialsRefreshed)
                                    if (session.restApi.checkConnection()) {
                                        HaSessionHolder.connect(session)
                                        onConnected(config)
                                    } else {
                                        snackbarHostState.showSnackbar("Home Assistant rejected the new session.")
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Sign-in failed: ${e.message}")
                                }
                            }
                            return true
                        }
                    }
                    loadUrl(oauthClient.authorizeUrl())
                }
            },
        )
    }
}
