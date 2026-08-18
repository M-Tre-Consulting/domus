package dev.domus.android.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.domus.android.R
import dev.domus.android.data.SettingsStore
import dev.domus.shared.DesignTokens
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.HaEntityState
import dev.domus.shared.model.friendlyName
import dev.domus.shared.model.unitOfMeasurement

/** A short, human-readable summary of the entity's current value, e.g. "On", "21.5 °C", "Home". */
private fun previewText(entity: HaEntityState): String {
    val label = entity.state.toDisplayLabel()
    val unit = entity.unitOfMeasurement
    return if (!unit.isNullOrBlank() && entity.state.toDoubleOrNull() != null) "$label $unit" else label
}

/** Lets the user choose which entities show up on the dashboard, grouped by domain with an
 *  icon and a live-state preview so entities are recognizable at a glance instead of just a
 *  bare list of entity IDs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityPickerScreen(
    session: HaSession,
    settingsStore: SettingsStore,
    initialSelection: Set<String>,
    onSave: (Set<String>) -> Unit,
) {
    val entities by session.repository.entities.collectAsState()
    var selection by remember(initialSelection) { mutableStateOf(initialSelection) }
    var query by remember { mutableStateOf("") }
    val view = LocalView.current
    val useHapticFeedback by settingsStore.useHapticFeedback.collectAsState(initial = true)

    LaunchedEffect(session) {
        if (entities.isEmpty()) {
            session.repository.refresh()
        }
    }

    fun toggle(entityId: String) {
        if (useHapticFeedback) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        selection = if (entityId in selection) selection - entityId else selection + entityId
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.picker_title))
                        Text(
                            text = stringResource(R.string.picker_selected_count, selection.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { onSave(selection) }) { Text(stringResource(R.string.picker_save)) }
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
            val ungroupedEntities = entities.values
                .filter { entity ->
                    query.isBlank() ||
                        entity.friendlyName.contains(query, ignoreCase = true) ||
                        entity.entityId.contains(query, ignoreCase = true)
                }
                .sortedBy { it.friendlyName }
            // domainLabel() is @Composable, so resolve each label directly in composable scope
            // (a plain for-loop, not inside the sort comparator lambda below) before sorting.
            val domainLabels = HashMap<String, String>()
            for (domain in ungroupedEntities.map { it.domain }.distinct()) {
                domainLabels[domain] = domainLabel(domain)
            }
            val groupedEntities = ungroupedEntities
                .groupBy { it.domain }
                .toSortedMap(compareBy { domainLabels[it] ?: it })

            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.common_search)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing.md.dp, vertical = DesignTokens.Spacing.sm.dp),
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    groupedEntities.forEach { (domain, entitiesInGroup) ->
                        item(key = "header_$domain") {
                            DomainHeader(domainLabels[domain] ?: domain)
                        }
                        items(entitiesInGroup, key = { it.entityId }) { entity ->
                            EntityPickerRow(
                                entity = entity,
                                isSelected = entity.entityId in selection,
                                onToggle = { toggle(entity.entityId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntityPickerRow(
    entity: HaEntityState,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = DesignTokens.Spacing.md.dp, vertical = DesignTokens.Spacing.xs.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntityIconBadge(
            domain = entity.domain,
            isActive = isActiveState(entity.domain, entity.state),
            size = 36,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = DesignTokens.Spacing.sm.dp),
        ) {
            Text(
                text = entity.friendlyName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = previewText(entity),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
    }
}
