package dev.domus.android.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dev.domus.android.data.ConnectionStore
import dev.domus.android.data.SettingsStore
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.HaConnectionConfig
import dev.domus.shared.model.HaServiceCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fired by Play services on geofence enter/exit. Toggles the configured entity via
 * `homeassistant.turn_on`/`turn_off` — generic enough to work whether the user pointed it at an
 * input_boolean, a light, a switch, or an automation-triggering helper.
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        val entering = event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER
        val exiting = event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT
        if (!entering && !exiting) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val entityId = SettingsStore(appContext).geofenceEntityId.first() ?: return@launch
                val connectionStore = ConnectionStore(appContext)
                val config = connectionStore.read() ?: return@launch
                val session = HaSession(config) { refreshed ->
                    connectionStore.save(HaConnectionConfig(config.baseUrl, refreshed))
                }
                session.restApi.callService(
                    HaServiceCall(
                        domain = "homeassistant",
                        service = if (entering) "turn_on" else "turn_off",
                        entityId = entityId,
                    ),
                )
            } catch (_: Exception) {
                // Best-effort: no connectivity, session expired, etc. Nothing user-facing to do
                // from a background receiver; the next transition will retry.
            } finally {
                pendingResult.finish()
            }
        }
    }
}
