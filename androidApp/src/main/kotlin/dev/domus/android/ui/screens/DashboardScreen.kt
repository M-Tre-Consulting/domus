package dev.domus.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.domus.shared.DesignTokens
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.HaEntityState
import dev.domus.shared.model.HaServiceCall
import dev.domus.shared.model.brightnessPercent
import dev.domus.shared.model.currentPosition
import dev.domus.shared.model.currentTemperature
import dev.domus.shared.model.friendlyName
import dev.domus.shared.model.maxValue
import dev.domus.shared.model.mediaArtist
import dev.domus.shared.model.mediaTitle
import dev.domus.shared.model.minValue
import dev.domus.shared.model.numericValue
import dev.domus.shared.model.options
import dev.domus.shared.model.percentage
import dev.domus.shared.model.targetTemperature
import dev.domus.shared.model.temperatureUnit
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive

/** Domains where `homeassistant.toggle` is a meaningful action, not just a read-only sensor. */
private val TOGGLEABLE_DOMAINS = setOf("light", "switch", "fan", "automation", "input_boolean", "siren")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    session: HaSession,
    favoriteEntityIds: Set<String>,
    onEditEntities: () -> Unit,
    onLogout: () -> Unit,
    onOpenClimateDetail: (String) -> Unit,
) {
    val entities by session.repository.entities.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(session) {
        try {
            session.repository.refresh()
            session.repository.startRealtimeUpdates(this)
        } catch (e: Exception) {
            errorMessage = "Couldn't load entities: ${e.message}"
        }
    }

    fun refresh() {
        scope.launch {
            isRefreshing = true
            try {
                session.repository.refresh()
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Couldn't refresh: ${e.message}")
            } finally {
                isRefreshing = false
            }
        }
    }

    val groupedEntities = entities.values
        .filter { it.entityId in favoriteEntityIds }
        .sortedBy { it.friendlyName }
        .groupBy { domainLabel(it.domain) }
        .toSortedMap()

    fun callService(call: HaServiceCall) {
        scope.launch {
            try {
                session.repository.callService(call)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Couldn't update ${call.entityId}: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Domus") },
                actions = {
                    IconButton(onClick = onEditEntities) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "Choose entities")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = "Disconnect")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = ::refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when {
                errorMessage != null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                }

                favoriteEntityIds.isEmpty() -> Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = "No entities chosen yet.")
                    Button(
                        onClick = onEditEntities,
                        modifier = Modifier.padding(top = DesignTokens.Spacing.md.dp),
                    ) {
                        Text("Choose entities")
                    }
                }

                entities.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(DesignTokens.Spacing.md.dp),
                ) {
                    groupedEntities.forEach { (label, entitiesInGroup) ->
                        stickyHeader(key = "header_$label") {
                            DomainHeader(label)
                        }
                        items(entitiesInGroup, key = { it.entityId }) { entity ->
                            EntityCard(
                                entity = entity,
                                onCallService = ::callService,
                                onOpenDetail = if (entity.domain == "climate" || entity.domain == "water_heater") {
                                    { onOpenClimateDetail(entity.entityId) }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DomainHeader(label: String) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = DesignTokens.Spacing.sm.dp),
        )
    }
}

@Composable
private fun EntityCard(
    entity: HaEntityState,
    onCallService: (HaServiceCall) -> Unit,
    onOpenDetail: (() -> Unit)? = null,
) {
    val isToggleable = entity.domain in TOGGLEABLE_DOMAINS &&
        (entity.state.equals("on", ignoreCase = true) || entity.state.equals("off", ignoreCase = true))

    val cardModifier = if (onOpenDetail != null) {
        Modifier.padding(bottom = DesignTokens.Spacing.sm.dp).clickable(onClick = onOpenDetail)
    } else {
        Modifier.padding(bottom = DesignTokens.Spacing.sm.dp)
    }

    Card(modifier = cardModifier) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.md.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = iconForDomain(entity.domain),
                    contentDescription = null,
                    modifier = Modifier.padding(end = DesignTokens.Spacing.sm.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = entity.friendlyName, style = MaterialTheme.typography.titleSmall)
                    Text(text = entity.state, style = MaterialTheme.typography.bodyMedium)
                }
                if (isToggleable) {
                    Switch(
                        checked = entity.state.equals("on", ignoreCase = true),
                        onCheckedChange = {
                            onCallService(
                                HaServiceCall(domain = "homeassistant", service = "toggle", entityId = entity.entityId),
                            )
                        },
                    )
                }
            }

            if (entity.domain == "light" && entity.state.equals("on", ignoreCase = true)) {
                BrightnessSlider(entity = entity, onCallService = onCallService)
            }

            if (entity.domain == "fan" && entity.state.equals("on", ignoreCase = true)) {
                FanSpeedSlider(entity = entity, onCallService = onCallService)
            }

            if (entity.domain == "climate" || entity.domain == "water_heater") {
                TemperatureControls(entity = entity, onCallService = onCallService)
            }

            if (entity.domain == "media_player") {
                MediaPlayerControls(entity = entity, onCallService = onCallService)
            }

            if (entity.domain == "cover") {
                OpenCloseControls(entity = entity, domain = "cover", onCallService = onCallService)
            }

            if (entity.domain == "valve") {
                OpenCloseControls(entity = entity, domain = "valve", onCallService = onCallService)
            }

            if (entity.domain == "lock") {
                LockControls(entity = entity, onCallService = onCallService)
            }

            if (entity.domain == "vacuum") {
                VacuumControls(entity = entity, onCallService = onCallService)
            }

            if (entity.domain == "lawn_mower") {
                LawnMowerControls(entity = entity, onCallService = onCallService)
            }

            if (entity.domain == "scene") {
                ActionButton(entity = entity, label = "Activate", domain = "scene", service = "turn_on", onCallService = onCallService)
            }

            if (entity.domain == "script") {
                ActionButton(entity = entity, label = "Run", domain = "script", service = "turn_on", onCallService = onCallService)
            }

            if (entity.domain == "button") {
                ActionButton(entity = entity, label = "Press", domain = "button", service = "press", onCallService = onCallService)
            }

            if (entity.domain == "number" || entity.domain == "input_number") {
                NumberSlider(entity = entity, onCallService = onCallService)
            }

            if (entity.domain == "select" || entity.domain == "input_select") {
                SelectDropdown(entity = entity, onCallService = onCallService)
            }
        }
    }
}

@Composable
private fun BrightnessSlider(entity: HaEntityState, onCallService: (HaServiceCall) -> Unit) {
    val remotePercent = entity.brightnessPercent ?: return
    var sliderPercent by remember(entity.entityId) { mutableFloatStateOf(remotePercent.toFloat()) }

    Slider(
        value = sliderPercent,
        onValueChange = { sliderPercent = it },
        onValueChangeFinished = {
            onCallService(
                HaServiceCall(
                    domain = "light",
                    service = "turn_on",
                    entityId = entity.entityId,
                    data = mapOf("brightness_pct" to JsonPrimitive(sliderPercent.toInt())),
                ),
            )
        },
        valueRange = 1f..100f,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = DesignTokens.Spacing.sm.dp),
    )
}

/** Shared by `climate` and `water_heater`: both expose current/target temperature + `set_temperature`. */
@Composable
private fun TemperatureControls(entity: HaEntityState, onCallService: (HaServiceCall) -> Unit) {
    val target = entity.targetTemperature ?: return
    val current = entity.currentTemperature

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = DesignTokens.Spacing.sm.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (current != null) {
            Text(
                text = "Current: $current${entity.temperatureUnit}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
        }
        IconButton(onClick = {
            onCallService(
                HaServiceCall(
                    domain = entity.domain,
                    service = "set_temperature",
                    entityId = entity.entityId,
                    data = mapOf("temperature" to JsonPrimitive(target - 0.5)),
                ),
            )
        }) {
            Icon(imageVector = Icons.Filled.Remove, contentDescription = "Decrease target temperature")
        }
        Text(text = "$target${entity.temperatureUnit}", style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = {
            onCallService(
                HaServiceCall(
                    domain = entity.domain,
                    service = "set_temperature",
                    entityId = entity.entityId,
                    data = mapOf("temperature" to JsonPrimitive(target + 0.5)),
                ),
            )
        }) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Increase target temperature")
        }
    }
}

@Composable
private fun FanSpeedSlider(entity: HaEntityState, onCallService: (HaServiceCall) -> Unit) {
    val remotePercent = entity.percentage ?: return
    var sliderPercent by remember(entity.entityId) { mutableFloatStateOf(remotePercent.toFloat()) }

    Slider(
        value = sliderPercent,
        onValueChange = { sliderPercent = it },
        onValueChangeFinished = {
            onCallService(
                HaServiceCall(
                    domain = "fan",
                    service = "set_percentage",
                    entityId = entity.entityId,
                    data = mapOf("percentage" to JsonPrimitive(sliderPercent.toInt())),
                ),
            )
        },
        valueRange = 0f..100f,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = DesignTokens.Spacing.sm.dp),
    )
}

/** Shared by `cover` and `valve`: both expose open/stop/close + position. */
@Composable
private fun OpenCloseControls(entity: HaEntityState, domain: String, onCallService: (HaServiceCall) -> Unit) {
    val openService = if (domain == "cover") "open_cover" else "open_valve"
    val stopService = if (domain == "cover") "stop_cover" else "stop_valve"
    val closeService = if (domain == "cover") "close_cover" else "close_valve"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = DesignTokens.Spacing.sm.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        entity.currentPosition?.let { position ->
            Text(
                text = "Position: $position%",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
        }
        IconButton(onClick = { onCallService(HaServiceCall(domain, openService, entity.entityId)) }) {
            Icon(imageVector = Icons.Filled.KeyboardArrowUp, contentDescription = "Open")
        }
        IconButton(onClick = { onCallService(HaServiceCall(domain, stopService, entity.entityId)) }) {
            Icon(imageVector = Icons.Filled.Stop, contentDescription = "Stop")
        }
        IconButton(onClick = { onCallService(HaServiceCall(domain, closeService, entity.entityId)) }) {
            Icon(imageVector = Icons.Filled.KeyboardArrowDown, contentDescription = "Close")
        }
    }
}

@Composable
private fun LockControls(entity: HaEntityState, onCallService: (HaServiceCall) -> Unit) {
    val isLocked = entity.state.equals("locked", ignoreCase = true)
    val isUnlocked = entity.state.equals("unlocked", ignoreCase = true)
    if (!isLocked && !isUnlocked) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = DesignTokens.Spacing.sm.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(
            checked = isLocked,
            onCheckedChange = {
                val service = if (isLocked) "unlock" else "lock"
                onCallService(HaServiceCall(domain = "lock", service = service, entityId = entity.entityId))
            },
        )
    }
}

@Composable
private fun VacuumControls(entity: HaEntityState, onCallService: (HaServiceCall) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = DesignTokens.Spacing.sm.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = { onCallService(HaServiceCall("vacuum", "start", entity.entityId)) }) { Text("Start") }
        TextButton(onClick = { onCallService(HaServiceCall("vacuum", "pause", entity.entityId)) }) { Text("Pause") }
        TextButton(onClick = { onCallService(HaServiceCall("vacuum", "return_to_base", entity.entityId)) }) { Text("Dock") }
    }
}

@Composable
private fun LawnMowerControls(entity: HaEntityState, onCallService: (HaServiceCall) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = DesignTokens.Spacing.sm.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = { onCallService(HaServiceCall("lawn_mower", "start_mowing", entity.entityId)) }) { Text("Start") }
        TextButton(onClick = { onCallService(HaServiceCall("lawn_mower", "pause", entity.entityId)) }) { Text("Pause") }
        TextButton(onClick = { onCallService(HaServiceCall("lawn_mower", "dock", entity.entityId)) }) { Text("Dock") }
    }
}

/** A single-action entity: `scene.turn_on`, `script.turn_on`, `button.press`. */
@Composable
private fun ActionButton(
    entity: HaEntityState,
    label: String,
    domain: String,
    service: String,
    onCallService: (HaServiceCall) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = DesignTokens.Spacing.sm.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Button(onClick = { onCallService(HaServiceCall(domain, service, entity.entityId)) }) {
            Text(label)
        }
    }
}

@Composable
private fun NumberSlider(entity: HaEntityState, onCallService: (HaServiceCall) -> Unit) {
    val value = entity.numericValue ?: return
    val min = entity.minValue ?: return
    val max = entity.maxValue ?: return
    if (min >= max) return

    var sliderValue by remember(entity.entityId) { mutableFloatStateOf(value.toFloat()) }

    Column(modifier = Modifier.padding(top = DesignTokens.Spacing.sm.dp)) {
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = {
                onCallService(
                    HaServiceCall(
                        domain = entity.domain,
                        service = "set_value",
                        entityId = entity.entityId,
                        data = mapOf("value" to JsonPrimitive(sliderValue.toDouble())),
                    ),
                )
            },
            valueRange = min.toFloat()..max.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SelectDropdown(entity: HaEntityState, onCallService: (HaServiceCall) -> Unit) {
    val options = entity.options
    if (options.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(top = DesignTokens.Spacing.sm.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Select option", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = "Choose option")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onCallService(
                            HaServiceCall(
                                domain = entity.domain,
                                service = "select_option",
                                entityId = entity.entityId,
                                data = mapOf("option" to JsonPrimitive(option)),
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun MediaPlayerControls(entity: HaEntityState, onCallService: (HaServiceCall) -> Unit) {
    val title = entity.mediaTitle
    val artist = entity.mediaArtist
    val isPlaying = entity.state.equals("playing", ignoreCase = true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = DesignTokens.Spacing.sm.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (title != null) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium)
            }
            if (artist != null) {
                Text(text = artist, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (entity.state.equals("playing", ignoreCase = true) || entity.state.equals("paused", ignoreCase = true)) {
            IconButton(onClick = {
                onCallService(
                    HaServiceCall(
                        domain = "media_player",
                        service = "media_play_pause",
                        entityId = entity.entityId,
                    ),
                )
            }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                )
            }
        }
    }
}
