package dev.domus.android.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.domus.android.R
import dev.domus.android.ui.components.StateHistorySection
import dev.domus.shared.DesignTokens
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.HaServiceCall
import dev.domus.shared.model.currentMa
import dev.domus.shared.model.currentPowerW
import dev.domus.shared.model.deviceClass
import dev.domus.shared.model.friendlyName
import dev.domus.shared.model.todayEnergyKwh
import dev.domus.shared.model.unitOfMeasurement
import dev.domus.shared.model.voltageV
import kotlinx.coroutines.launch
import dev.domus.android.ui.LocalAnimatedVisibilityScope
import dev.domus.android.ui.LocalRefreshIntervalSeconds
import dev.domus.android.ui.LocalSharedTransitionScope

private val POWER_DEVICE_CLASSES = setOf("power", "voltage", "current", "energy", "apparent_power")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwitchDetailScreen(session: HaSession, entityId: String, onBack: () -> Unit) {
    val entities by session.repository.entities.collectAsState()
    val entity = entities[entityId]
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val refreshInterval = LocalRefreshIntervalSeconds.current
    val context = LocalContext.current

    LaunchedEffect(entityId, refreshInterval) {
        val switchName = entityId.substringAfter('.')
        while (true) {
            delay(refreshInterval.toLong() * 1000L)
            val ids = session.repository.entities.value.keys
                .filter { it.substringBefore('.') == "sensor" && it.substringAfter('.').startsWith(switchName) }
                .toMutableSet().also { it.add(entityId) }
            try { session.repository.refreshEntities(ids) } catch (_: Exception) {}
        }
    }

    fun callService(call: HaServiceCall) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            try {
                session.repository.callService(call)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(context.getString(R.string.switch_error_update, e.message))
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (entity == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val isOn = entity.state.equals("on", ignoreCase = true)

        val sharedTransitionScope = LocalSharedTransitionScope.current
        val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
        val heroSharedModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            with(sharedTransitionScope) {
                Modifier.sharedElement(
                    sharedContentState = rememberSharedContentState(key = "hero_$entityId"),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
        } else Modifier

        // Collect power monitoring data. Try direct attributes on the switch entity first
        // (integrations like TP-Link Kasa embed power data there). If none are found, look for
        // sibling sensor entities whose entity ID shares the same name prefix and whose
        // device_class is a power-monitoring class — this covers integrations like Shelly or
        // Tasmota that expose separate sensor entities per measurement.
        val powerLabel = stringResource(R.string.switch_power)
        val voltageLabel = stringResource(R.string.switch_voltage)
        val currentLabel = stringResource(R.string.switch_current)
        val energyLabel = stringResource(R.string.switch_energy)
        val apparentPowerLabel = stringResource(R.string.switch_apparent_power)
        val todaysEnergyLabel = stringResource(R.string.switch_todays_energy)
        val powerRows = buildList {
            entity.currentPowerW?.let { add(powerLabel to "%.1f W".format(it)) }
            entity.voltageV?.let { add(voltageLabel to "%.1f V".format(it)) }
            entity.currentMa?.let {
                if (it > 1000) add(currentLabel to "%.2f A".format(it / 1000.0))
                else add(currentLabel to "%.0f mA".format(it))
            }
            entity.todayEnergyKwh?.let { add(todaysEnergyLabel to "%.3f kWh".format(it)) }

            if (isEmpty()) {
                val switchName = entityId.substringAfter('.')
                entities.values
                    .filter { e ->
                        e.domain == "sensor" &&
                        e.entityId.substringAfter('.').startsWith(switchName) &&
                        e.deviceClass in POWER_DEVICE_CLASSES
                    }
                    .sortedBy { e ->
                        when (e.deviceClass) {
                            "power" -> 0
                            "voltage" -> 1
                            "current" -> 2
                            "energy" -> 3
                            else -> 4
                        }
                    }
                    .forEach { sensor ->
                        val label = when (sensor.deviceClass) {
                            "power" -> powerLabel
                            "voltage" -> voltageLabel
                            "current" -> currentLabel
                            "energy" -> energyLabel
                            "apparent_power" -> apparentPowerLabel
                            else -> sensor.friendlyName
                        }
                        val value = buildString {
                            append(sensor.state)
                            sensor.unitOfMeasurement?.let { append(" $it") }
                        }
                        add(label to value)
                    }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DesignTokens.Spacing.lg.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(DesignTokens.Spacing.xl.dp))

            // Tapping the hero badge toggles the switch.
            Surface(
                onClick = {
                    callService(
                        HaServiceCall("switch", if (isOn) "turn_off" else "turn_on", entity.entityId),
                    )
                },
                shape = CircleShape,
                color = if (isOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = heroSharedModifier.size(96.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PowerSettingsNew,
                        contentDescription = stringResource(if (isOn) R.string.switch_turn_off else R.string.switch_turn_on),
                        tint = if (isOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }

            Text(
                text = entity.state.toDisplayLabel(),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = DesignTokens.Spacing.md.dp),
            )

            if (powerRows.isNotEmpty()) {
                Spacer(Modifier.height(DesignTokens.Spacing.xl.dp))
                Text(
                    text = stringResource(R.string.switch_energy_monitoring),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start),
                )
                Spacer(Modifier.height(DesignTokens.Spacing.sm.dp))
                InfoCard(rows = powerRows)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.lg.dp))
            StateHistorySection(session = session, entityId = entityId, entityName = entity.friendlyName, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(DesignTokens.Spacing.xl.dp))
        }
    }
}
