package dev.domus.android

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dev.domus.android.data.ConnectionStore
import dev.domus.android.data.SettingsStore
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.HaConnectionConfig
import dev.domus.shared.model.HaServiceCall
import dev.domus.shared.model.friendlyName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Quick Settings panel tile that toggles the entity chosen in Settings > Quick Settings Tile.
 * Like [ShortcutToggleActivity], it opens a short-lived connection rather than depending on the
 * app already being open.
 */
class DomusQuickToggleTile : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        job?.cancel()
        job = scope.launch {
            val entityId = SettingsStore(applicationContext).quickTileEntityId.first() ?: return@launch
            val connectionStore = ConnectionStore(applicationContext)
            val config = connectionStore.read() ?: return@launch
            val session = sessionFor(config, connectionStore)
            runCatching {
                session.restApi.callService(
                    HaServiceCall(domain = "homeassistant", service = "toggle", entityId = entityId),
                )
            }
            refreshTileFrom(session, entityId)
        }
    }

    private fun refresh() {
        job?.cancel()
        job = scope.launch {
            val entityId = SettingsStore(applicationContext).quickTileEntityId.first()
            val connectionStore = ConnectionStore(applicationContext)
            val config = connectionStore.read()
            if (entityId == null || config == null) {
                markUnavailable()
                return@launch
            }
            refreshTileFrom(sessionFor(config, connectionStore), entityId)
        }
    }

    private suspend fun refreshTileFrom(session: HaSession, entityId: String) {
        val state = runCatching { session.restApi.getState(entityId) }.getOrNull()
        if (state == null) {
            markUnavailable()
            return
        }
        qsTile?.apply {
            label = state.friendlyName
            this.state = if (state.state.equals("on", ignoreCase = true)) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            icon = Icon.createWithResource(this@DomusQuickToggleTile, R.drawable.ic_launcher_monochrome)
            updateTile()
        }
    }

    private fun markUnavailable() {
        qsTile?.apply {
            label = getString(R.string.app_name)
            state = Tile.STATE_UNAVAILABLE
            icon = Icon.createWithResource(this@DomusQuickToggleTile, R.drawable.ic_launcher_monochrome)
            updateTile()
        }
    }

    private fun sessionFor(config: HaConnectionConfig, connectionStore: ConnectionStore) =
        HaSession(config) { refreshed -> connectionStore.save(HaConnectionConfig(config.baseUrl, refreshed)) }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }
}
