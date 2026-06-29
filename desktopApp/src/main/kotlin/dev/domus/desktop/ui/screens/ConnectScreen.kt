package dev.domus.desktop.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.domus.desktop.data.HaSessionHolder
import dev.domus.shared.DesignTokens
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.HaConnectionConfig
import kotlinx.coroutines.launch

@Composable
fun ConnectScreen(
    onConnected: (HaConnectionConfig) -> Unit,
    onLoginWithBrowser: (baseUrl: String) -> Unit,
) {
    var baseUrl by remember { mutableStateOf("https://") }
    var showTokenForm by remember { mutableStateOf(false) }
    var token by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val trimmedUrl = baseUrl.trim().trimEnd('/')
    val urlIsValid = trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://")

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.9f)
                .padding(DesignTokens.Spacing.lg.dp),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = null,
                modifier = Modifier.padding(bottom = DesignTokens.Spacing.sm.dp).size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text("Connect to Home Assistant", style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it; errorMessage = null },
                label = { Text("Server URL") },
                placeholder = { Text("https://your-instance.example.com") },
                singleLine = true,
                isError = baseUrl.isNotBlank() && !urlIsValid,
                supportingText = if (baseUrl.isNotBlank() && !urlIsValid) {
                    { Text("Must start with http:// or https://") }
                } else null,
                modifier = Modifier.fillMaxWidth(),
            )

            // Primary: browser login
            Button(
                enabled = urlIsValid,
                onClick = { onLoginWithBrowser(trimmedUrl) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sign in with Home Assistant")
            }

            // Secondary: long-lived token toggle
            TextButton(onClick = { showTokenForm = !showTokenForm; errorMessage = null }) {
                Text(if (showTokenForm) "Hide token form" else "Use a long-lived token instead")
            }

            AnimatedVisibility(visible = showTokenForm) {
                Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md.dp)) {
                    HorizontalDivider()

                    Text(
                        "Create a long-lived access token in your HA profile under Security → Long-lived access tokens.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it; errorMessage = null },
                        label = { Text("Long-lived access token") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    OutlinedButton(
                        enabled = !isConnecting && urlIsValid && token.isNotBlank(),
                        onClick = {
                            errorMessage = null
                            isConnecting = true
                            scope.launch {
                                try {
                                    val config = HaConnectionConfig.withToken(trimmedUrl, token.trim())
                                    val session = HaSession(config)
                                    if (session.restApi.checkConnection()) {
                                        HaSessionHolder.connect(session)
                                        onConnected(config)
                                    } else {
                                        errorMessage = "Connection rejected — check the URL and token."
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Couldn't reach Home Assistant: ${e.message}"
                                } finally {
                                    isConnecting = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = DesignTokens.Spacing.sm.dp).size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                        Text("Connect with token")
                    }
                }
            }
        }
    }
}
