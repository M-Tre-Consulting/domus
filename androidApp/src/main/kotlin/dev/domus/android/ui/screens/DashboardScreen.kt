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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.launch

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
        .sortedBy { it.entityId }

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
                    EntityCard(
                        entity = entity,
                        onToggle = {
                            scope.launch {
                                try {
                                    session.repository.callService(
                                        HaServiceCall(
                                            domain = "homeassistant",
                                            service = "toggle",
                                            entityId = entity.entityId,
                                        ),
                                    )
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Couldn't toggle ${entity.entityId}: ${e.message}")
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EntityCard(entity: HaEntityState, onToggle: () -> Unit) {
    val isToggleable = entity.domain in TOGGLEABLE_DOMAINS &&
        (entity.state.equals("on", ignoreCase = true) || entity.state.equals("off", ignoreCase = true))

    Card(modifier = Modifier.padding(bottom = DesignTokens.Spacing.sm.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.Spacing.md.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = entity.entityId, style = MaterialTheme.typography.titleSmall)
                Text(text = entity.state, style = MaterialTheme.typography.bodyMedium)
            }
            if (isToggleable) {
                Switch(
                    checked = entity.state.equals("on", ignoreCase = true),
                    onCheckedChange = { onToggle() },
                )
            }
        }
    }
}
