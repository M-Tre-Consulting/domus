package dev.domus.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.domus.shared.DesignTokens

/**
 * First-run screen: ask for the Home Assistant base URL and a long-lived access token.
 * Wiring this up to [dev.domus.shared.data.HaRepository] and persisting the result is
 * the next step once the scaffold is in place.
 */
@Composable
fun ConnectScreen(onConnected: () -> Unit) {
    var baseUrl by remember { mutableStateOf("http://homeassistant.local:8123") }
    var token by remember { mutableStateOf("") }

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
            onValueChange = { baseUrl = it },
            label = { Text("Server URL") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DesignTokens.Spacing.lg.dp),
        )
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Long-lived access token") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DesignTokens.Spacing.sm.dp),
        )

        Button(
            onClick = onConnected,
            shape = ButtonDefaults.shape,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DesignTokens.Spacing.lg.dp),
        ) {
            Text("Connect")
        }
    }
}
