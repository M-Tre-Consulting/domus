package dev.domus.android.ui.screens

import android.content.ComponentName
import android.service.quicksettings.TileService
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.domus.android.DomusQuickToggleTile
import dev.domus.android.R
import dev.domus.android.data.SettingsStore
import dev.domus.shared.DesignTokens
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.friendlyName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Domains meaningful as a single-tap Quick Settings tile toggle. */
private val QUICK_TILE_ELIGIBLE_DOMAINS = setOf("light", "switch", "fan", "input_boolean", "siren")

private val REFRESH_INTERVALS = listOf(5, 10, 30, 60)

private enum class SettingsSection(val icon: ImageVector, val labelRes: Int) {
    APPEARANCE(Icons.Filled.Palette, R.string.settings_section_appearance),
    DASHBOARD(Icons.Filled.Dashboard, R.string.settings_section_dashboard),
    GENERAL(Icons.Filled.Tune, R.string.settings_section_general),
    SECURITY(Icons.Filled.Security, R.string.settings_section_security),
    AUTOMATION(Icons.Filled.Nfc, R.string.settings_section_automation),
    ADVANCED(Icons.Filled.Build, R.string.settings_section_advanced),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsStore: SettingsStore,
    session: HaSession?,
    favoriteEntityIds: Set<String>,
    onBack: () -> Unit,
) {
    var selectedSection by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.common_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = DesignTokens.Spacing.md.dp, vertical = DesignTokens.Spacing.sm.dp),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm.dp),
            ) {
                SettingsSection.entries.forEachIndexed { index, section ->
                    FilterChip(
                        selected = selectedSection == index,
                        onClick = { selectedSection = index },
                        label = { Text(stringResource(section.labelRes)) },
                        leadingIcon = { Icon(section.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            HorizontalDivider()

            AnimatedContent(
                targetState = selectedSection,
                label = "settings-section",
                transitionSpec = { fadeIn() togetherWith fadeOut() },
            ) { section ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(DesignTokens.Spacing.md.dp),
                ) {
                    when (SettingsSection.entries[section]) {
                        SettingsSection.APPEARANCE -> AppearanceSection(settingsStore)
                        SettingsSection.DASHBOARD -> DashboardSection(settingsStore)
                        SettingsSection.GENERAL -> GeneralSection(settingsStore)
                        SettingsSection.SECURITY -> SecuritySection(settingsStore)
                        SettingsSection.AUTOMATION -> AutomationSection(settingsStore, session, favoriteEntityIds)
                        SettingsSection.ADVANCED -> AdvancedSection(settingsStore)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearanceSection(settingsStore: SettingsStore) {
    val themeMode by settingsStore.themeMode.collectAsState(initial = "system")
    val seedColorArgb by settingsStore.seedColorArgb.collectAsState(initial = 0)
    val uiDensity by settingsStore.uiDensity.collectAsState(initial = "comfortable")
    val scope = rememberCoroutineScope()

    SettingsLabel(title = stringResource(R.string.settings_theme_title), subtitle = stringResource(R.string.settings_theme_subtitle))
    ThreewaySegment(
        options = listOf(
            "system" to stringResource(R.string.settings_theme_system),
            "light" to stringResource(R.string.settings_theme_light),
            "dark" to stringResource(R.string.settings_theme_dark),
        ),
        selected = themeMode,
        onSelect = { scope.launch { settingsStore.setThemeMode(it) } },
    )

    SettingsLabel(
        title = stringResource(R.string.settings_color_scheme_title),
        subtitle = stringResource(R.string.settings_color_scheme_subtitle),
        modifier = Modifier.padding(top = DesignTokens.Spacing.sm.dp),
    )
    val presetSeedColors = listOf(
        0 to stringResource(R.string.settings_color_auto),
        0xFF1565C0.toInt() to stringResource(R.string.settings_color_blue),
        0xFF6200EE.toInt() to stringResource(R.string.settings_color_purple),
        0xFF00695C.toInt() to stringResource(R.string.settings_color_teal),
        0xFF2E7D32.toInt() to stringResource(R.string.settings_color_green),
        0xFFE65100.toInt() to stringResource(R.string.settings_color_orange),
        0xFFC62828.toInt() to stringResource(R.string.settings_color_red),
        0xFFAD1457.toInt() to stringResource(R.string.settings_color_pink),
    )
    val selectedLabel = stringResource(R.string.settings_color_selected)
    SeedColorRow(
        current = seedColorArgb,
        presets = presetSeedColors,
        selectedLabel = selectedLabel,
        onSelect = { scope.launch { settingsStore.setSeedColorArgb(it) } },
    )

    SettingsLabel(
        title = stringResource(R.string.settings_density_title),
        subtitle = stringResource(R.string.settings_density_subtitle),
        modifier = Modifier.padding(top = DesignTokens.Spacing.sm.dp),
    )
    ThreewaySegment(
        options = listOf(
            "compact" to stringResource(R.string.settings_density_compact),
            "comfortable" to stringResource(R.string.settings_density_comfortable),
            "spacious" to stringResource(R.string.settings_density_spacious),
        ),
        selected = uiDensity,
        onSelect = { scope.launch { settingsStore.setUiDensity(it) } },
    )
}

@Composable
private fun DashboardSection(settingsStore: SettingsStore) {
    val groupByRoom by settingsStore.groupByRoom.collectAsState(initial = true)
    val dashboardLayout by settingsStore.dashboardLayout.collectAsState(initial = "grid2")
    val scope = rememberCoroutineScope()

    SettingsLabel(title = stringResource(R.string.settings_layout_title), subtitle = stringResource(R.string.settings_layout_subtitle))
    ThreewaySegment(
        options = listOf(
            "list" to stringResource(R.string.settings_layout_list),
            "grid2" to stringResource(R.string.settings_layout_grid),
            "grid4" to stringResource(R.string.settings_layout_compact),
        ),
        selected = dashboardLayout,
        onSelect = { scope.launch { settingsStore.setDashboardLayout(it) } },
    )

    SettingsToggle(
        title = stringResource(R.string.settings_group_by_room_title),
        subtitle = stringResource(R.string.settings_group_by_room_subtitle),
        checked = groupByRoom,
        onCheckedChange = { scope.launch { settingsStore.setGroupByRoom(it) } },
    )
}

@Composable
private fun GeneralSection(settingsStore: SettingsStore) {
    val useHapticFeedback by settingsStore.useHapticFeedback.collectAsState(initial = true)
    val keepScreenOn by settingsStore.keepScreenOn.collectAsState(initial = false)
    val keepConnectionAlive by settingsStore.keepConnectionAlive.collectAsState(initial = false)
    val refreshIntervalSeconds by settingsStore.refreshIntervalSeconds.collectAsState(initial = 10)
    val scope = rememberCoroutineScope()

    SettingsToggle(
        title = stringResource(R.string.settings_haptic_title),
        subtitle = stringResource(R.string.settings_haptic_subtitle),
        checked = useHapticFeedback,
        onCheckedChange = { scope.launch { settingsStore.setUseHapticFeedback(it) } },
    )

    SettingsToggle(
        title = stringResource(R.string.settings_keep_screen_on_title),
        subtitle = stringResource(R.string.settings_keep_screen_on_subtitle),
        checked = keepScreenOn,
        onCheckedChange = { scope.launch { settingsStore.setKeepScreenOn(it) } },
    )

    SettingsToggle(
        title = stringResource(R.string.settings_keep_connected_title),
        subtitle = stringResource(R.string.settings_keep_connected_subtitle),
        checked = keepConnectionAlive,
        onCheckedChange = { scope.launch { settingsStore.setKeepConnectionAlive(it) } },
    )

    RefreshIntervalDropdown(
        currentSeconds = refreshIntervalSeconds,
        onSelected = { scope.launch { settingsStore.setRefreshIntervalSeconds(it) } },
    )
}

@Composable
private fun SecuritySection(settingsStore: SettingsStore) {
    val appLockEnabled by settingsStore.appLockEnabled.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    SettingsToggle(
        title = stringResource(R.string.settings_app_lock_title),
        subtitle = stringResource(R.string.settings_app_lock_subtitle),
        checked = appLockEnabled,
        onCheckedChange = { scope.launch { settingsStore.setAppLockEnabled(it) } },
    )
}

@Composable
private fun AutomationSection(
    settingsStore: SettingsStore,
    session: HaSession?,
    favoriteEntityIds: Set<String>,
) {
    val quickTileEntityId by settingsStore.quickTileEntityId.collectAsState(initial = null)
    val entities by (session?.repository?.entities ?: remember { MutableStateFlow(emptyMap()) })
        .collectAsState(initial = emptyMap())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    fun selectQuickTile(entityId: String) {
        scope.launch {
            settingsStore.setQuickTileEntityId(entityId)
            TileService.requestListeningState(context, ComponentName(context, DomusQuickToggleTile::class.java))
        }
    }

    SettingsLabel(
        title = stringResource(R.string.settings_quick_tile_title),
        subtitle = stringResource(R.string.settings_quick_tile_subtitle),
    )
    val tileEligible = favoriteEntityIds
        .mapNotNull { entities[it] }
        .filter { it.domain in QUICK_TILE_ELIGIBLE_DOMAINS }
        .sortedBy { it.friendlyName }
    if (tileEligible.isEmpty()) {
        Text(
            text = stringResource(R.string.settings_quick_tile_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        tileEligible.forEach { entity ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectQuickTile(entity.entityId) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = entity.entityId == quickTileEntityId,
                    onClick = { selectQuickTile(entity.entityId) },
                )
                Text(entity.friendlyName, modifier = Modifier.padding(start = DesignTokens.Spacing.sm.dp))
            }
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.md.dp))

    NfcSection(settingsStore, session, favoriteEntityIds)

    HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.md.dp))

    GeofenceSection(settingsStore, session, favoriteEntityIds)
}

@Composable
private fun AdvancedSection(settingsStore: SettingsStore) {
    val showDebugDiag by settingsStore.showDebugDiag.collectAsState(initial = true)
    val scope = rememberCoroutineScope()

    SettingsToggle(
        title = stringResource(R.string.settings_debug_title),
        subtitle = stringResource(R.string.settings_debug_subtitle),
        checked = showDebugDiag,
        onCheckedChange = { scope.launch { settingsStore.setShowDebugDiag(it) } },
    )
}

@Composable
internal fun SectionHeader(title: String) {
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
private fun SeedColorRow(current: Int, presets: List<Pair<Int, String>>, selectedLabel: String, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DesignTokens.Spacing.xs.dp),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm.dp),
    ) {
        presets.forEach { (argb, name) ->
            ColorSwatch(argb = argb, name = name, selected = current == argb, selectedLabel = selectedLabel, onSelect = onSelect)
        }
    }
}

@Composable
private fun ColorSwatch(argb: Int, name: String, selected: Boolean, selectedLabel: String, onSelect: (Int) -> Unit) {
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
                contentDescription = selectedLabel,
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
            Text(stringResource(R.string.settings_refresh_interval_title), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.settings_refresh_interval_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = stringResource(R.string.settings_refresh_interval_value, currentSeconds),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(0.35f),
                singleLine = true,
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                REFRESH_INTERVALS.forEach { seconds ->
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_refresh_interval_value, seconds)) },
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
