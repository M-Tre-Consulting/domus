package dev.domus.android.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.domus.android.ui.components.TemperatureDial
import dev.domus.shared.DesignTokens
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.HaServiceCall
import dev.domus.shared.model.currentHumidity
import dev.domus.shared.model.currentTemperature
import dev.domus.shared.model.fanMode
import dev.domus.shared.model.fanModes
import dev.domus.shared.model.friendlyName
import dev.domus.shared.model.hvacAction
import dev.domus.shared.model.hvacMode
import dev.domus.shared.model.hvacModes
import dev.domus.shared.model.maxTemp
import dev.domus.shared.model.minTemp
import dev.domus.shared.model.swingMode
import dev.domus.shared.model.swingModes
import dev.domus.shared.model.targetHumidity
import dev.domus.shared.model.targetTempStep
import dev.domus.shared.model.targetTemperature
import dev.domus.shared.model.temperatureUnit
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive

/**
 * Full detail screen for a `climate`/`water_heater` entity: power, mode/fan/swing
 * selection, and a draggable temperature dial. Mirrors the standard HA app's more-info
 * dialog content, rebuilt as a native Material 3 screen instead of a web dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClimateDetailScreen(session: HaSession, entityId: String, onBack: () -> Unit) {
    val entities by session.repository.entities.collectAsState()
    val entity = entities[entityId]
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun callService(call: HaServiceCall) {
        scope.launch {
            try {
                session.repository.callService(call)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Couldn't update: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(entity?.friendlyName ?: entityId) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (entity == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(DesignTokens.Spacing.lg.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entity.hvacAction?.replaceFirstChar { it.uppercase() } ?: "Power",
                    style = MaterialTheme.typography.titleMedium,
                )
                Switch(
                    checked = !entity.hvacMode.equals("off", ignoreCase = true),
                    onCheckedChange = { checked ->
                        val service = if (checked) "turn_on" else "turn_off"
                        callService(HaServiceCall(domain = entity.domain, service = service, entityId = entity.entityId))
                    },
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg.dp))

            val target = entity.targetTemperature
            if (target != null) {
                TemperatureDial(
                    target = target,
                    current = entity.currentTemperature,
                    minValue = entity.minTemp,
                    maxValue = entity.maxTemp,
                    step = entity.targetTempStep,
                    unit = entity.temperatureUnit,
                    onTargetChange = { newTarget ->
                        callService(
                            HaServiceCall(
                                domain = entity.domain,
                                service = "set_temperature",
                                entityId = entity.entityId,
                                data = mapOf("temperature" to JsonPrimitive(newTarget)),
                            ),
                        )
                    },
                )
            }

            val humidity = entity.currentHumidity ?: entity.targetHumidity
            if (humidity != null) {
                Text(
                    text = "Humidity: $humidity%",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = DesignTokens.Spacing.sm.dp),
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg.dp))

            if (entity.hvacModes.isNotEmpty()) {
                ModeSection(
                    label = "Mode",
                    options = entity.hvacModes,
                    selected = entity.hvacMode,
                    onSelect = { mode ->
                        callService(
                            HaServiceCall(
                                domain = entity.domain,
                                service = "set_hvac_mode",
                                entityId = entity.entityId,
                                data = mapOf("hvac_mode" to JsonPrimitive(mode)),
                            ),
                        )
                    },
                )
            }

            if (entity.fanModes.isNotEmpty()) {
                ModeSection(
                    label = "Fan",
                    options = entity.fanModes,
                    selected = entity.fanMode,
                    onSelect = { mode ->
                        callService(
                            HaServiceCall(
                                domain = entity.domain,
                                service = "set_fan_mode",
                                entityId = entity.entityId,
                                data = mapOf("fan_mode" to JsonPrimitive(mode)),
                            ),
                        )
                    },
                )
            }

            if (entity.swingModes.isNotEmpty()) {
                ModeSection(
                    label = "Swing",
                    options = entity.swingModes,
                    selected = entity.swingMode,
                    onSelect = { mode ->
                        callService(
                            HaServiceCall(
                                domain = entity.domain,
                                service = "set_swing_mode",
                                entityId = entity.entityId,
                                data = mapOf("swing_mode" to JsonPrimitive(mode)),
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ModeSection(
    label: String,
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = DesignTokens.Spacing.md.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = DesignTokens.Spacing.sm.dp),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm.dp),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option.equals(selected, ignoreCase = true),
                    onClick = { onSelect(option) },
                    label = { Text(option.replace('_', ' ').replaceFirstChar { it.uppercase() }) },
                )
            }
        }
    }
}
