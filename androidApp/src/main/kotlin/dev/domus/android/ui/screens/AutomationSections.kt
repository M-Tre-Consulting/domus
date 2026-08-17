package dev.domus.android.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import dev.domus.android.R
import dev.domus.android.data.SettingsStore
import dev.domus.android.geofence.GeofenceManager
import dev.domus.android.nfc.NfcTagWriter
import dev.domus.shared.DesignTokens
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.friendlyName
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

private val NFC_ELIGIBLE_DOMAINS = setOf("scene", "script")
private val GEOFENCE_ENTITY_ELIGIBLE_DOMAINS = setOf("input_boolean", "light", "switch", "fan", "automation", "script")

private sealed interface NfcWriteState {
    data object Idle : NfcWriteState
    data object Writing : NfcWriteState
    data class Success(val name: String) : NfcWriteState
    data class Failure(val message: String) : NfcWriteState
}

@Composable
fun NfcSection(
    settingsStore: SettingsStore,
    session: HaSession?,
    favoriteEntityIds: Set<String>,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val entities by (session?.repository?.entities ?: remember { MutableStateFlow(emptyMap()) })
        .collectAsState(initial = emptyMap())
    var selectedEntityId by remember { mutableStateOf<String?>(null) }
    var writeState by remember { mutableStateOf<NfcWriteState>(NfcWriteState.Idle) }
    val writer = remember(activity) { activity?.let { NfcTagWriter(it) } }

    DisposableEffect(writer) {
        onDispose { writer?.stop() }
    }

    SectionHeader(stringResource(R.string.nfc_section_title))
    Text(
        text = stringResource(R.string.nfc_section_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = DesignTokens.Spacing.sm.dp),
    )

    if (writer == null || !writer.isAvailable) {
        Text(
            text = stringResource(R.string.nfc_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val eligible = favoriteEntityIds
        .mapNotNull { entities[it] }
        .filter { it.domain in NFC_ELIGIBLE_DOMAINS }
        .sortedBy { it.friendlyName }

    if (eligible.isEmpty()) {
        Text(
            text = stringResource(R.string.nfc_pick_scene_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    eligible.forEach { entity ->
        Row(
            modifier = Modifier.fillMaxWidth().clickable { selectedEntityId = entity.entityId },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = entity.entityId == selectedEntityId, onClick = { selectedEntityId = entity.entityId })
            Text(entity.friendlyName, modifier = Modifier.padding(start = DesignTokens.Spacing.sm.dp))
        }
    }

    val entityId = selectedEntityId
    val entityName = entities[entityId]?.friendlyName ?: entityId.orEmpty()
    Button(
        enabled = entityId != null && writeState != NfcWriteState.Writing,
        onClick = {
            val id = entityId ?: return@Button
            writeState = NfcWriteState.Writing
            writer.startWriting(id) { result ->
                writeState = result.fold(
                    onSuccess = { NfcWriteState.Success(entityName) },
                    onFailure = { NfcWriteState.Failure(it.message ?: it.toString()) },
                )
            }
        },
        modifier = Modifier.padding(top = DesignTokens.Spacing.sm.dp),
    ) {
        Text(stringResource(R.string.nfc_write_button))
    }

    when (val state = writeState) {
        is NfcWriteState.Writing -> Text(
            text = stringResource(R.string.nfc_writing),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = DesignTokens.Spacing.xs.dp),
        )
        is NfcWriteState.Success -> Text(
            text = stringResource(R.string.nfc_write_success, state.name),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = DesignTokens.Spacing.xs.dp),
        )
        is NfcWriteState.Failure -> Text(
            text = stringResource(R.string.nfc_write_failed, state.message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = DesignTokens.Spacing.xs.dp),
        )
        NfcWriteState.Idle -> {}
    }
}

@Composable
fun GeofenceSection(
    settingsStore: SettingsStore,
    session: HaSession?,
    favoriteEntityIds: Set<String>,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val entities by (session?.repository?.entities ?: remember { MutableStateFlow(emptyMap()) })
        .collectAsState(initial = emptyMap())
    val geofenceEnabled by settingsStore.geofenceEnabled.collectAsState(initial = false)
    val geofenceLatitude by settingsStore.geofenceLatitude.collectAsState(initial = null)
    val geofenceLongitude by settingsStore.geofenceLongitude.collectAsState(initial = null)
    val geofenceRadiusMeters by settingsStore.geofenceRadiusMeters.collectAsState(initial = 150)
    val geofenceEntityId by settingsStore.geofenceEntityId.collectAsState(initial = null)

    var hasFineLocation by remember { mutableStateOf(GeofenceManager.hasLocationPermission(context)) }
    var hasBackgroundLocation by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED,
        )
    }
    var isLocating by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasBackgroundLocation = granted }

    val fineLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasFineLocation = granted
        if (granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    SectionHeader(stringResource(R.string.geofence_section_title))
    Text(
        text = stringResource(R.string.geofence_section_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = DesignTokens.Spacing.sm.dp),
    )

    if (!hasFineLocation || !hasBackgroundLocation) {
        Text(
            text = stringResource(R.string.geofence_permission_needed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = { fineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
            modifier = Modifier.padding(top = DesignTokens.Spacing.sm.dp),
        ) {
            Text(stringResource(R.string.geofence_grant_button))
        }
        return
    }

    Button(
        enabled = !isLocating,
        onClick = {
            isLocating = true
            locationError = null
            scope.launch {
                try {
                    val location = lastKnownLocation(context)
                    if (location != null) {
                        settingsStore.setGeofenceLocation(location.first, location.second)
                    } else {
                        locationError = context.getString(R.string.geofence_no_fix_yet)
                    }
                } catch (e: Exception) {
                    locationError = e.message ?: e.toString()
                } finally {
                    isLocating = false
                }
            }
        },
    ) {
        Text(stringResource(R.string.geofence_use_current_location))
    }
    if (isLocating) {
        CircularProgressIndicator(modifier = Modifier.padding(top = DesignTokens.Spacing.sm.dp).size(20.dp))
    }
    locationError?.let {
        Text(
            text = stringResource(R.string.geofence_no_location, it),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
    if (geofenceLatitude != null && geofenceLongitude != null) {
        Text(
            text = "%.5f, %.5f".format(geofenceLatitude, geofenceLongitude),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Text(
        text = stringResource(R.string.geofence_radius_title, geofenceRadiusMeters),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = DesignTokens.Spacing.sm.dp),
    )
    var sliderRadius by remember(geofenceRadiusMeters) { mutableFloatStateOf(geofenceRadiusMeters.toFloat()) }
    Slider(
        value = sliderRadius,
        onValueChange = { sliderRadius = it },
        onValueChangeFinished = { scope.launch { settingsStore.setGeofenceRadiusMeters(sliderRadius.toInt()) } },
        valueRange = 50f..1000f,
    )

    Text(
        text = stringResource(R.string.geofence_pick_entity_title),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = DesignTokens.Spacing.sm.dp),
    )
    val entityEligible = favoriteEntityIds
        .mapNotNull { entities[it] }
        .filter { it.domain in GEOFENCE_ENTITY_ELIGIBLE_DOMAINS }
        .sortedBy { it.friendlyName }
    if (entityEligible.isEmpty()) {
        Text(
            text = stringResource(R.string.geofence_pick_entity_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        entityEligible.forEach { entity ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { scope.launch { settingsStore.setGeofenceEntityId(entity.entityId) } },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = entity.entityId == geofenceEntityId,
                    onClick = { scope.launch { settingsStore.setGeofenceEntityId(entity.entityId) } },
                )
                Text(entity.friendlyName, modifier = Modifier.padding(start = DesignTokens.Spacing.sm.dp))
            }
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.md.dp))

    val canEnable = geofenceLatitude != null && geofenceLongitude != null && geofenceEntityId != null
    if (!canEnable) {
        Text(
            text = stringResource(R.string.geofence_not_configured),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = DesignTokens.Spacing.sm.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.geofence_enabled_title), style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = geofenceEnabled && canEnable,
            enabled = canEnable,
            onCheckedChange = { enabled ->
                scope.launch {
                    settingsStore.setGeofenceEnabled(enabled)
                    val lat = geofenceLatitude
                    val lng = geofenceLongitude
                    if (enabled && lat != null && lng != null) {
                        GeofenceManager.register(context, lat, lng, geofenceRadiusMeters.toFloat())
                    } else {
                        GeofenceManager.unregister(context)
                    }
                }
            },
        )
    }
    Text(
        text = stringResource(R.string.geofence_enabled_subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Reads the last cached fix rather than requesting a fresh GPS lock, to keep this simple and
 *  avoid holding a location callback open past the composable's lifetime. */
@Suppress("MissingPermission")
private suspend fun lastKnownLocation(context: Context): Pair<Double, Double>? =
    suspendCancellableCoroutine { continuation ->
        LocationServices.getFusedLocationProviderClient(context).lastLocation
            .addOnSuccessListener { location ->
                if (location != null) continuation.resume(location.latitude to location.longitude)
                else continuation.resume(null)
            }
            .addOnFailureListener { continuation.resume(null) }
    }
