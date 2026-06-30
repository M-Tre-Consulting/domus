package dev.domus.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.domus.android.data.SettingsStore
import dev.domus.shared.DesignTokens
import kotlinx.coroutines.launch

private val PRESET_SEED_COLORS = listOf(
    0                    to "Auto",
    0xFF1565C0.toInt()   to "Blue",
    0xFF6200EE.toInt()   to "Purple",
    0xFF00695C.toInt()   to "Teal",
    0xFF2E7D32.toInt()   to "Green",
    0xFFE65100.toInt()   to "Orange",
    0xFFC62828.toInt()   to "Red",
    0xFFAD1457.toInt()   to "Pink",
)

private val REFRESH_INTERVALS = listOf(5, 10, 30, 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsStore: SettingsStore,
    onBack: () -> Unit,
) {
    val showDebugDiag by settingsStore.showDebugDiag.collectAsState(initial = true)
    val useHapticFeedback by settingsStore.useHapticFeedback.collectAsState(initial = true)
    val groupByRoom by settingsStore.groupByRoom.collectAsState(initial = true)
    val keepScreenOn by settingsStore.keepScreenOn.collectAsState(initial = false)
    val refreshIntervalSeconds by settingsStore.refreshIntervalSeconds.collectAsState(initial = 10)
    val themeMode by settingsStore.themeMode.collectAsState(initial = "system")
    val seedColorArgb by settingsStore.seedColorArgb.collectAsState(initial = 0)
    val uiDensity by settingsStore.uiDensity.collectAsState(initial = "comfortable")
    val dashboardLayout by settingsStore.dashboardLayout.collectAsState(initial = "grid2")
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(DesignTokens.Spacing.md.dp),
        ) {
            // ── Appearance ──────────────────────────────────────────────────
            SectionHeader("Appearance")

            SettingsLabel(title = "Theme", subtitle = "Override the system light/dark preference.")
            ThreewaySegment(
                options = listOf("system" to "System", "light" to "Light", "dark" to "Dark"),
                selected = themeMode,
                onSelect = { scope.launch { settingsStore.setThemeMode(it) } },
            )

            SettingsLabel(
                title = "Color Scheme",
                subtitle = "Seed color for the Material palette. Auto uses your wallpaper.",
                modifier = Modifier.padding(top = DesignTokens.Spacing.sm.dp),
            )
            SeedColorRow(
                current = seedColorArgb,
                onSelect = { scope.launch { settingsStore.setSeedColorArgb(it) } },
            )

            SettingsLabel(
                title = "Density",
                subtitle = "Scale the UI up or down to fit more (or less) on screen.",
                modifier = Modifier.padding(top = DesignTokens.Spacing.sm.dp),
            )
            ThreewaySegment(
                options = listOf("compact" to "Compact", "comfortable" to "Standard", "spacious" to "Spacious"),
                selected = uiDensity,
                onSelect = { scope.launch { settingsStore.setUiDensity(it) } },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.md.dp))

            // ── Dashboard ────────────────────────────────────────────────────
            SectionHeader("Dashboard")

            SettingsLabel(title = "Layout", subtitle = "How many columns to show on the dashboard.")
            ThreewaySegment(
                options = listOf("list" to "List", "grid2" to "Grid", "grid4" to "Compact"),
                selected = dashboardLayout,
                onSelect = { scope.launch { settingsStore.setDashboardLayout(it) } },
            )

            SettingsToggle(
                title = "Group by Room",
                subtitle = "Organise entities by area instead of device type.",
                checked = groupByRoom,
                onCheckedChange = { scope.launch { settingsStore.setGroupByRoom(it) } },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.md.dp))

            // ── General ──────────────────────────────────────────────────────
            SectionHeader("General")

            SettingsToggle(
                title = "Haptic Feedback",
                subtitle = "Vibrate when interacting with switches and sliders.",
                checked = useHapticFeedback,
                onCheckedChange = { scope.launch { settingsStore.setUseHapticFeedback(it) } },
            )

            SettingsToggle(
                title = "Keep Screen On",
                subtitle = "Prevent the screen from sleeping while the dashboard is open.",
                checked = keepScreenOn,
                onCheckedChange = { scope.launch { settingsStore.setKeepScreenOn(it) } },
            )

            RefreshIntervalDropdown(
                currentSeconds = refreshIntervalSeconds,
                onSelected = { scope.launch { settingsStore.setRefreshIntervalSeconds(it) } },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.md.dp))

            // ── Advanced ─────────────────────────────────────────────────────
            SectionHeader("Advanced")

            SettingsToggle(
                title = "Show Debug Info",
                subtitle = "Show entity registry counts in the dashboard title.",
                checked = showDebugDiag,
                onCheckedChange = { scope.launch { settingsStore.setShowDebugDiag(it) } },
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = DesignTokens.Spacing.sm.dp),
    )
}

@Composable
private fun SettingsLabel(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = 4.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThreewaySegment(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = selected == value,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun SeedColorRow(current: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DesignTokens.Spacing.xs.dp),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm.dp),
    ) {
        PRESET_SEED_COLORS.forEach { (argb, name) ->
            ColorSwatch(argb = argb, name = name, selected = current == argb, onSelect = onSelect)
        }
    }
}

@Composable
private fun ColorSwatch(argb: Int, name: String, selected: Boolean, onSelect: (Int) -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .border(2.dp, borderColor, CircleShape)
            .background(if (argb != 0) Color(argb) else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onSelect(argb) },
        contentAlignment = Alignment.Center,
    ) {
        if (argb == 0) {
            Icon(
                imageVector = Icons.Filled.WbSunny,
                contentDescription = name,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        } else if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = "${currentSeconds}s",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(0.35f),
                singleLine = true,
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
