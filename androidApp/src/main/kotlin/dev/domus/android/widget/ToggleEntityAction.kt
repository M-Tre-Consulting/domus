package dev.domus.android.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dev.domus.android.data.ConnectionStore
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.HaConnectionConfig
import dev.domus.shared.model.HaServiceCall

val entityIdParamKey = ActionParameters.Key<String>("entity_id")

/** Toggles the tapped entity, then refreshes the widget so it reflects the new state. */
class ToggleEntityAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val entityId = parameters[entityIdParamKey] ?: return
        val connectionStore = ConnectionStore(context)
        val config = connectionStore.read() ?: return
        val session = HaSession(config) { refreshed ->
            connectionStore.save(HaConnectionConfig(config.baseUrl, refreshed))
        }
        runCatching {
            session.restApi.callService(
                HaServiceCall(domain = "homeassistant", service = "toggle", entityId = entityId),
            )
        }
        DomusWidget().update(context, glanceId)
    }
}
