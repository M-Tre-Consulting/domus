package dev.domus.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import dev.domus.shared.model.currentTemperature
import dev.domus.shared.model.friendlyName
import dev.domus.shared.model.mediaArtist
import dev.domus.shared.model.mediaTitle
import dev.domus.shared.model.targetTemperature
import dev.domus.shared.model.temperatureUnit
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive

/** Domains where `homeassistant.toggle` is a meaningful action, not just a read-only sensor. */
private val TOGGLEABLE_DOMAINS = setOf("light", "switch", "fan", "automation", "input_boolean", "siren")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    session: HaSession,
    favoriteEntityIds: Set<String>,
    onEditEntities: () -> Unit,
    onLogout: () -> Unit,
) {
    val entities by session.repository.entities.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
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

    val visibleEntities = entities.values
        .filter { it.entityId in favoriteEntityIds }
        .sortedBy { it.friendlyName }

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
        when {
            errorMessage != null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
            }

            favoriteEntityIds.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
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
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(DesignTokens.Spacing.md.dp),
            ) {
                items(visibleEntities, key = { it.entityId }) { entity ->
                    EntityCard(entity = entity, onCallService = ::callService)
                }
            }
        }
    }
}

@Composable
private fun EntityCard(entity: HaEntityState, onCallService: (HaServiceCall) -> Unit) {
    val isToggleable = entity.domain in TOGGLEABLE_DOMAINS &&
        (entity.state.equals("on", ignoreCase = true) || entity.state.equals("off", ignoreCase = true))

    Card(modifier = Modifier.padding(bottom = DesignTokens.Spacing.sm.dp)) {
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

            if (entity.domain == "climate") {
                ClimateControls(entity = entity, onCallService = onCallService)
            }

            if (entity.domain == "media_player") {
                MediaPlayerControls(entity = entity, onCallService = onCallService)
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

@Composable
private fun ClimateControls(entity: HaEntityState, onCallService: (HaServiceCall) -> Unit) {
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
                    domain = "climate",
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
                    domain = "climate",
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
