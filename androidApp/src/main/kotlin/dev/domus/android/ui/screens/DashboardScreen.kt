package dev.domus.android.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

@Composable
fun DashboardScreen(session: HaSession) {
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

    when {
        errorMessage != null -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
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
            items(entities.values.sortedBy { it.entityId }, key = { it.entityId }) { entity ->
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
