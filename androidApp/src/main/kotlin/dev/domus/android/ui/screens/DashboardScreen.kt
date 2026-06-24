package dev.domus.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.domus.shared.DesignTokens
import dev.domus.shared.data.HaSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    session: HaSession,
    favoriteEntityIds: Set<String>,
    onEditEntities: () -> Unit,
) {
    val entities by session.repository.entities.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                },
            )
        },
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
                    Card(modifier = Modifier.padding(bottom = DesignTokens.Spacing.sm.dp)) {
                        Column(modifier = Modifier.padding(DesignTokens.Spacing.md.dp)) {
                            Text(text = entity.entityId, style = MaterialTheme.typography.titleSmall)
                            Text(text = entity.state, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
