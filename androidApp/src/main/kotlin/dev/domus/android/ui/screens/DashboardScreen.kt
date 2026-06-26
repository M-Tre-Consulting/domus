package dev.domus.android.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.text.style.TextOverflow
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
import dev.domus.shared.model.currentPowerW
import dev.domus.shared.model.numericValue
import dev.domus.shared.model.options
import dev.domus.shared.model.percentage
import dev.domus.shared.model.targetTemperature
import dev.domus.shared.model.temperatureUnit
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import dev.domus.android.ui.LocalAnimatedVisibilityScope
import dev.domus.android.ui.LocalSharedTransitionScope
import dev.domus.android.data.SettingsStore
import dev.domus.shared.data.WebSocketState
import dev.domus.shared.model.hueColor

/** Domains where `homeassistant.toggle` is a meaningful action, not just a read-only sensor. */
private val TOGGLEABLE_DOMAINS = setOf("light", "switch", "fan", "automation", "input_boolean", "siren")

/**
 * Domains rendered as compact half-width tiles. Light and switch get full-width EntityCards
 * so they can show inline controls (brightness slider, power reading) and navigate to detail.
 */
private val TILE_DOMAINS = setOf("fan", "automation", "input_boolean", "siren", "binary_sensor", "sensor")

private val ACTIVE_STATES = setOf(
    "on", "playing", "heat", "cool", "heat_cool", "dry", "fan_only", "auto", "cleaning", "home", "triggered",
)

/** Whether an entity is in a state worth visually highlighting (lit up, running, unlocked, open...). */
private fun isActiveState(domain: String, state: String): Boolean {
    val s = state.lowercase()
    return when (domain) {
        "lock" -> s == "unlocked"
        "cover", "valve" -> s == "open"
        else -> s in ACTIVE_STATES
    }
}

/** Derives a badge background color from the light's current hs_color attribute. */
private fun lightBadgeColor(entity: HaEntityState): Color? {
    if (!entity.state.equals("on", ignoreCase = true)) return null
    val hs = entity.hueColor ?: return null
    return Color.hsv(hs.first, (hs.second / 100f * 0.85f).coerceIn(0f, 1f), 0.95f)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    session: HaSession,
    settingsStore: SettingsStore,
    favoriteEntityIds: Set<String>,
    onEditEntities: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    onOpenDetail: (entityId: String) -> Unit,
) {
    val entities by session.repository.entities.collectAsState()
    val areaEntityMap by session.repository.areaEntityMap.collectAsState()
    val registryDiag by session.repository.registryDiag.collectAsState()
    val wsState by session.repository.wsState.collectAsState()
    val showDebugDiag by settingsStore.showDebugDiag.collectAsState(initial = true)
    val groupByRoom by settingsStore.groupByRoom.collectAsState(initial = true)
    val keepScreenOn by settingsStore.keepScreenOn.collectAsState(initial = false)
    val useHapticFeedback by settingsStore.useHapticFeedback.collectAsState(initial = true)
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    DisposableEffect(keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    // Refresh + realtime updates are started once at connect time (HaSessionHolder.connect),
    // not here — a screen's LaunchedEffect gets cancelled as soon as you navigate away from
    // it, which previously killed the WebSocket connection while viewing other screens. This
    // is just a fallback for the rare case where that initial refresh hadn't completed yet.
    LaunchedEffect(session) {
        if (session.repository.entities.value.isEmpty()) {
            try {
                session.repository.refresh()
            } catch (e: Exception) {
                errorMessage = "Couldn't load entities: ${e.message}"
            }
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

    val useRoomGrouping = groupByRoom && areaEntityMap.isNotEmpty()
    val groupedEntities = entities.values
        .filter { it.entityId in favoriteEntityIds }
        .filter { entity ->
            searchQuery.isBlank() ||
            entity.friendlyName.contains(searchQuery, ignoreCase = true) ||
            entity.entityId.contains(searchQuery, ignoreCase = true)
        }
        .sortedBy { it.friendlyName }
        .groupBy { entity ->
            if (useRoomGrouping) areaEntityMap[entity.entityId] ?: "Other"
            else domainLabel(entity.domain)
        }
        .let { groups ->
            if (useRoomGrouping) {
                groups.toSortedMap { a, b ->
                    when {
                        a == "Other" -> 1
                        b == "Other" -> -1
                        else -> a.compareTo(b)
                    }
                }
            } else {
                groups.toSortedMap()
            }
        }

    fun callService(call: HaServiceCall) {
        if (useHapticFeedback) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
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
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search entities…") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { isSearchActive = false; searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close search")
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text("Domus")
                            if (showDebugDiag) {
                                Text(
                                    text = registryDiag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(imageVector = Icons.Filled.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = onEditEntities) {
                            Icon(imageVector = Icons.Filled.Edit, contentDescription = "Choose entities")
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
                        }
                        IconButton(onClick = onLogout) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = "Disconnect")
                        }
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        AnimatedVisibility(visible = wsState != WebSocketState.Connected) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = ::refresh,
            modifier = Modifier.fillMaxSize().weight(1f),
        ) {
            val uiState = when {
                errorMessage != null -> "error"
                favoriteEntityIds.isEmpty() -> "empty"
                entities.isEmpty() -> "loading"
                else -> "content"
            }

            Crossfade(targetState = uiState, label = "dashboard-state") { state ->
                when (state) {
                    "error" -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                    }

                    "empty" -> Column(
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

                    "loading" -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }

                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(DesignTokens.Spacing.md.dp),
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm.dp),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm.dp),
                    ) {
                        groupedEntities.forEach { (label, entitiesInGroup) ->
                            item(key = "header_$label", span = { GridItemSpan(maxLineSpan) }) {
                                DomainHeader(label)
                            }
                            items(
                                entitiesInGroup,
                                key = { it.entityId },
                                span = { entity ->
                                    if (entity.domain in TILE_DOMAINS) GridItemSpan(1) else GridItemSpan(maxLineSpan)
                                },
                            ) { entity ->
                                if (entity.domain in TILE_DOMAINS) {
                                    EntityTile(entity = entity, onCallService = ::callService)
                                } else {
                                    EntityCard(
                                        entity = entity,
                                        onCallService = ::callService,
                                        onOpenDetail = when (entity.domain) {
                                            "climate", "water_heater", "light", "switch", "media_player" -> {
                                                { onOpenDetail(entity.entityId) }
                                            }
                                            else -> null
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        } // Column
    }
}

@Composable
private fun DomainHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DesignTokens.Spacing.sm.dp),
    )
}

@Composable
private fun EntityIconBadge(
    domain: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    overrideColor: Color? = null,
    sharedKey: String? = null,
    size: Int = 40,
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    val bgColor = overrideColor
        ?: if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val iconTint = if (overrideColor != null) {
        val lum = 0.299f * overrideColor.red + 0.587f * overrideColor.green + 0.114f * overrideColor.blue
        if (lum > 0.5f) Color.Black.copy(alpha = 0.87f) else Color.White
    } else {
        if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    }

    val sharedModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null && sharedKey != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = sharedKey),
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    } else Modifier

    Surface(
        shape = CircleShape,
        color = bgColor,
        modifier = modifier.then(sharedModifier).size(size.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = iconForDomain(domain),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size((size * 0.55f).dp),
            )
        }
    }
}

/** Compact half-width card for simple on/off entities and read-only sensors. */
@Composable
private fun EntityTile(entity: HaEntityState, onCallService: (HaServiceCall) -> Unit) {
    val isActive = isActiveState(entity.domain, entity.state)
    val isToggleable = entity.domain in TOGGLEABLE_DOMAINS &&
        (entity.state.equals("on", ignoreCase = true) || entity.state.equals("off", ignoreCase = true))

    Card(
        shape = RoundedCornerShape(DesignTokens.Shape.cornerLarge.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.md.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EntityIconBadge(
                    domain = entity.domain,
                    isActive = isActive,
                    overrideColor = lightBadgeColor(entity),
                    sharedKey = "hero_${entity.entityId}",
                )
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
            Text(
                text = entity.friendlyName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = DesignTokens.Spacing.sm.dp),
            )
            Text(
                text = entity.state.toDisplayLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Full-width card for entities that need extra controls (sliders, dials, chips, buttons). */
@Composable
private fun EntityCard(
    entity: HaEntityState,
    onCallService: (HaServiceCall) -> Unit,
    onOpenDetail: (() -> Unit)? = null,
) {
    val isActive = isActiveState(entity.domain, entity.state)
    val isToggleable = entity.domain in TOGGLEABLE_DOMAINS &&
        (entity.state.equals("on", ignoreCase = true) || entity.state.equals("off", ignoreCase = true))

    val cardModifier = if (onOpenDetail != null) {
        Modifier.fillMaxWidth().clickable(onClick = onOpenDetail)
    } else {
        Modifier.fillMaxWidth()
    }

    Card(
        shape = RoundedCornerShape(DesignTokens.Shape.cornerMedium.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ),
        modifier = cardModifier,
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.md.dp).animateContentSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EntityIconBadge(
                    domain = entity.domain,
                    isActive = isActive,
                    overrideColor = lightBadgeColor(entity),
                    sharedKey = "hero_${entity.entityId}",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = DesignTokens.Spacing.md.dp),
                ) {
                    Text(text = entity.friendlyName, style = MaterialTheme.typography.titleSmall)
                    Text(text = entity.state.toDisplayLabel(), style = MaterialTheme.typography.bodyMedium)
                    if (entity.domain == "switch") {
                        entity.currentPowerW?.let { power ->
                            Text(
                                text = "%.1f W".format(power),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
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
