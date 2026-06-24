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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
 * First-run screen: connect either with a long-lived access token, or by signing in through
 * Home Assistant's own login page (handles 2FA natively, no token needed). Verifies the
 * connection against `/api/` before handing the validated [HaConnectionConfig] to the caller.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(
    onConnected: (HaConnectionConfig) -> Unit,
    onLoginWithHomeAssistant: (baseUrl: String) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(0) }
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

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DesignTokens.Spacing.lg.dp),
        ) {
            SegmentedButton(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0; errorMessage = null },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) {
                Text("Log in", maxLines = 1)
            }
            SegmentedButton(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1; errorMessage = null },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) {
                Text("Token", maxLines = 1)
            }
        }

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

        if (selectedTab == 1) {
            OutlinedTextField(
                value = token,
                onValueChange = { token = it; errorMessage = null },
                label = { Text("Long-lived access token") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = DesignTokens.Spacing.sm.dp),
            )
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = DesignTokens.Spacing.sm.dp),
            )
        }

        Button(
            enabled = !isConnecting && baseUrl.isNotBlank() && (selectedTab == 0 || token.isNotBlank()),
            onClick = {
                if (selectedTab == 0) {
                    onLoginWithHomeAssistant(baseUrl.trim().trimEnd('/'))
                    return@Button
                }
                errorMessage = null
                isConnecting = true
                scope.launch {
                    try {
                        val config = HaConnectionConfig.withToken(baseUrl, token)
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DesignTokens.Spacing.lg.dp),
        ) {
            if (isConnecting) {
                CircularProgressIndicator(modifier = Modifier.padding(end = DesignTokens.Spacing.sm.dp))
            }
            Text(if (selectedTab == 0) "Continue" else "Connect")
        }
    }
}
