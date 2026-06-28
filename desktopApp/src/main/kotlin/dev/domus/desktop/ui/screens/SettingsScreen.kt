package dev.domus.desktop.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.domus.desktop.data.SettingsStore
import dev.domus.shared.DesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsStore: SettingsStore, onBack: () -> Unit) {
    val showDebugDiag by settingsStore.showDebugDiag.collectAsState()
    val groupByRoom by settingsStore.groupByRoom.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.md.dp),
            ) {
                Text(
                    text = "General",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = DesignTokens.Spacing.sm.dp),
                )

                SettingsToggle(
                    title = "Group by Room",
                    subtitle = "Organise entities by area instead of device type.",
                    checked = groupByRoom,
                    onCheckedChange = { settingsStore.setGroupByRoom(it) },
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.md.dp))

                Text(
                    text = "Advanced",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = DesignTokens.Spacing.sm.dp),
                )

                SettingsToggle(
                    title = "Show Debug Info",
                    subtitle = "Show entity registry counts in the dashboard title.",
                    checked = showDebugDiag,
                    onCheckedChange = { settingsStore.setShowDebugDiag(it) },
                )
            }
        }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DesignTokens.Spacing.sm.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
