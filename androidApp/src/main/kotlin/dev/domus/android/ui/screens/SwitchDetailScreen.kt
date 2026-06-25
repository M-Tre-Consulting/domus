package dev.domus.android.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.domus.shared.DesignTokens
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.HaServiceCall
import dev.domus.shared.model.currentMa
import dev.domus.shared.model.currentPowerW
import dev.domus.shared.model.friendlyName
import dev.domus.shared.model.todayEnergyKwh
import dev.domus.shared.model.voltageV
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwitchDetailScreen(session: HaSession, entityId: String, onBack: () -> Unit) {
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
                title = {
                    Text(
                        text = entity?.friendlyName ?: entityId,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DesignTokens.Spacing.lg.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(DesignTokens.Spacing.xl.dp))

            FilledIconToggleButton(
                checked = isOn,
                onCheckedChange = { on ->
                    callService(
                        HaServiceCall("switch", if (on) "turn_on" else "turn_off", entity.entityId),
                    )
                },
                modifier = Modifier.size(96.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.PowerSettingsNew,
                    contentDescription = "Power",
                    modifier = Modifier.size(48.dp),
                )
            }

            Text(
                text = entity.state.toDisplayLabel(),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = DesignTokens.Spacing.md.dp),
            )

            // Energy monitoring card — only shown when the integration reports power attributes
            val powerRows = buildList {
                entity.currentPowerW?.let { add("Power" to "%.1f W".format(it)) }
                entity.voltageV?.let { add("Voltage" to "%.1f V".format(it)) }
                entity.currentMa?.let {
                    if (it > 1000) add("Current" to "%.2f A".format(it / 1000.0))
                    else add("Current" to "%.0f mA".format(it))
                }
                entity.todayEnergyKwh?.let { add("Today's energy" to "%.3f kWh".format(it)) }
            }

            if (powerRows.isNotEmpty()) {
                Spacer(Modifier.height(DesignTokens.Spacing.xl.dp))
                Text(
                    text = "Energy monitoring",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start),
                )
                Spacer(Modifier.height(DesignTokens.Spacing.sm.dp))
                InfoCard(rows = powerRows)
            }

            Spacer(Modifier.height(DesignTokens.Spacing.xl.dp))
        }
    }
}
