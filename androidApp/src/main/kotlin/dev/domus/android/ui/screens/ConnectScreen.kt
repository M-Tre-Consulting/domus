package dev.domus.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.domus.android.data.HaSessionHolder
import dev.domus.shared.DesignTokens
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.HaConnectionConfig
import kotlinx.coroutines.launch

/**
 * First-run screen: ask for the Home Assistant base URL and a long-lived access token,
 * verify the connection against `/api/`, then hand a connected [HaSession] to the caller.
 */
@Composable
fun ConnectScreen(onConnected: () -> Unit) {
    var baseUrl by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(DesignTokens.Spacing.lg.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Home,
            contentDescription = null,
            modifier = Modifier.padding(bottom = DesignTokens.Spacing.md.dp),
        )
        Text(text = "Connect to Home Assistant", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it; errorMessage = null },
            label = { Text("Server URL") },
            placeholder = { Text("https://your-instance.example.com") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DesignTokens.Spacing.lg.dp),
        )
        OutlinedTextField(
            value = token,
            onValueChange = { token = it; errorMessage = null },
            label = { Text("Long-lived access token") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DesignTokens.Spacing.sm.dp),
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = DesignTokens.Spacing.sm.dp),
            )
        }

        Button(
            enabled = !isConnecting && baseUrl.isNotBlank() && token.isNotBlank(),
            onClick = {
                errorMessage = null
                isConnecting = true
                scope.launch {
                    try {
                        val config = HaConnectionConfig.of(baseUrl, token)
                        val session = HaSession(config)
                        if (session.restApi.checkConnection()) {
                            HaSessionHolder.session = session
                            onConnected()
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DesignTokens.Spacing.lg.dp),
        ) {
            if (isConnecting) {
                CircularProgressIndicator(modifier = Modifier.padding(end = DesignTokens.Spacing.sm.dp))
            }
            Text("Connect")
        }
    }
}
