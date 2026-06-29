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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val refreshIntervalSeconds by settingsStore.refreshIntervalSeconds.collectAsState()

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

                RefreshIntervalDropdown(
                    currentSeconds = refreshIntervalSeconds,
                    onSelected = { settingsStore.setRefreshIntervalSeconds(it) },
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

private val REFRESH_INTERVALS = listOf(5, 10, 30, 60)

@Composable
private fun RefreshIntervalDropdown(currentSeconds: Int, onSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DesignTokens.Spacing.sm.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Detail Refresh Interval", style = MaterialTheme.typography.bodyLarge)
            Text(
                "How often detail screens poll for the latest sensor values.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            OutlinedButton(onClick = { expanded = true }) { Text("${currentSeconds}s") }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                REFRESH_INTERVALS.forEach { seconds ->
                    DropdownMenuItem(
                        text = { Text("${seconds}s") },
                        onClick = { onSelected(seconds); expanded = false },
                    )
                }
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
