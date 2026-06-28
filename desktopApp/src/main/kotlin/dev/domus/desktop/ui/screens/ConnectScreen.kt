package dev.domus.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
fun ConnectScreen(onConnected: (HaConnectionConfig) -> Unit) {
    var baseUrl by remember { mutableStateOf("https://") }
    var token by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .padding(DesignTokens.Spacing.lg.dp),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = null,
                modifier = Modifier.padding(bottom = DesignTokens.Spacing.sm.dp),
            )
            Text(
                text = "Connect to Home Assistant",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Enter your Home Assistant URL and a long-lived access token. You can create one in your HA profile under Security → Long-lived access tokens.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it; errorMessage = null },
                label = { Text("Server URL") },
                placeholder = { Text("https://your-instance.example.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
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

            Button(
                enabled = !isConnecting && baseUrl.isNotBlank() && token.isNotBlank(),
                onClick = {
                    errorMessage = null
                    isConnecting = true
                    scope.launch {
                        try {
                            val config = HaConnectionConfig.withToken(baseUrl.trim().trimEnd('/'), token.trim())
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
                        modifier = Modifier.padding(end = DesignTokens.Spacing.sm.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text("Connect")
            }
        }
    }
}
