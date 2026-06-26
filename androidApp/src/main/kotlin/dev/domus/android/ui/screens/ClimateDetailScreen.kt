package dev.domus.android.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
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
 * Full detail screen for a `climate`/`water_heater` entity, styled after the Google Home
 * app's thermostat dialog: power button in the top bar, a draggable dial, compact
 * mode/fan/swing pill selectors, and a summary card — rebuilt as a native Material 3
 * screen instead of a web dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClimateDetailScreen(session: HaSession, entityId: String, onBack: () -> Unit) {
    val entities by session.repository.entities.collectAsState()
    val entity = entities[entityId]
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    fun callService(call: HaServiceCall) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                title = {
                    Text(
                        text = entity?.friendlyName ?: entityId,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (entity != null) {
                        FilledIconToggleButton(
                            checked = !entity.hvacMode.equals("off", ignoreCase = true),
                            onCheckedChange = { checked ->
                                val service = if (checked) "turn_on" else "turn_off"
                                callService(HaServiceCall(domain = entity.domain, service = service, entityId = entity.entityId))
                            },
                            modifier = Modifier.padding(end = DesignTokens.Spacing.sm.dp),
                        ) {
                            Icon(imageVector = Icons.Filled.PowerSettingsNew, contentDescription = "Power")
                        }
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
                .padding(horizontal = DesignTokens.Spacing.lg.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
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

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm.dp),
            ) {
                if (entity.hvacModes.isNotEmpty()) {
                    SelectorPill(
                        icon = iconForHvacMode(entity.hvacMode),
                        label = "Mode",
                        value = entity.hvacMode,
                        options = entity.hvacModes,
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
                    SelectorPill(
                        icon = Icons.Filled.Air,
                        label = "Fan speed",
                        value = entity.fanMode,
                        options = entity.fanModes,
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
                    SelectorPill(
                        icon = Icons.Filled.SwapVert,
                        label = "Swing",
                        value = entity.swingMode,
                        options = entity.swingModes,
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

            val infoRows = buildList {
                entity.currentTemperature?.let { add("Current temperature" to "%.1f%s".format(it, entity.temperatureUnit)) }
                (entity.currentHumidity ?: entity.targetHumidity)?.let { add("Humidity" to "$it%") }
            }
            InfoCard(rows = infoRows)

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg.dp))
        }
    }
}

@Composable
private fun SelectorPill(
    icon: ImageVector,
    label: String,
    value: String?,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = RoundedCornerShape(DesignTokens.Shape.cornerLarge.dp),
            color = if (expanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clickable { expanded = true },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = DesignTokens.Spacing.md.dp, vertical = DesignTokens.Spacing.sm.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.padding(end = DesignTokens.Spacing.sm.dp))
                Column {
                    Text(text = label, style = MaterialTheme.typography.labelSmall)
                    Text(text = value?.toDisplayLabel() ?: "—", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.toDisplayLabel()) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@Composable
internal fun InfoCard(rows: List<Pair<String, String>>) {
    if (rows.isEmpty()) return
    Card(modifier = Modifier.fillMaxWidth().padding(top = DesignTokens.Spacing.lg.dp)) {
        Column {
            rows.forEachIndexed { index, (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing.md.dp, vertical = DesignTokens.Spacing.sm.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (index != rows.lastIndex) HorizontalDivider()
            }
        }
    }
}
