package dev.domus.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import dev.domus.shared.model.friendlyName

/** Lets the user choose which entities show up on the dashboard. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityPickerScreen(
    session: HaSession,
    initialSelection: Set<String>,
    onSave: (Set<String>) -> Unit,
) {
    val entities by session.repository.entities.collectAsState()
    var selection by remember(initialSelection) { mutableStateOf(initialSelection) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(session) {
        if (entities.isEmpty()) {
            session.repository.refresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose entities") },
                actions = {
                    TextButton(onClick = { onSave(selection) }) { Text("Save") }
                },
            )
        },
    ) { padding ->
        if (entities.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            val filteredEntities = entities.values
                .filter { entity ->
                    query.isBlank() ||
                        entity.friendlyName.contains(query, ignoreCase = true) ||
                        entity.entityId.contains(query, ignoreCase = true)
                }
                .sortedBy { it.friendlyName }

            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing.md.dp, vertical = DesignTokens.Spacing.sm.dp),
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredEntities, key = { it.entityId }) { entity ->
                        val isSelected = entity.entityId in selection
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selection = if (isSelected) {
                                        selection - entity.entityId
                                    } else {
                                        selection + entity.entityId
                                    }
                                }
                                .padding(
                                    horizontal = DesignTokens.Spacing.md.dp,
                                    vertical = DesignTokens.Spacing.sm.dp,
                                ),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selection = if (checked) {
                                            selection + entity.entityId
                                        } else {
                                            selection - entity.entityId
                                        }
                                    },
                                )
                                Column(modifier = Modifier.padding(start = DesignTokens.Spacing.sm.dp)) {
                                    Text(text = entity.friendlyName, style = MaterialTheme.typography.bodyMedium)
                                    Text(text = entity.entityId, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
