package dev.domus.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dev.domus.android.data.ConnectionStore
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.HaConnectionConfig
import dev.domus.shared.model.HaServiceCall
import kotlinx.coroutines.launch

/**
 * Headless trampoline launched from a long-press launcher shortcut: opens a short-lived
 * connection, toggles one entity, and finishes without ever showing UI.
 */
class ShortcutToggleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val entityId = intent.getStringExtra(EXTRA_ENTITY_ID)
        val label = intent.getStringExtra(EXTRA_LABEL) ?: entityId
        if (entityId == null) {
            finish()
            return
        }
        lifecycleScope.launch {
            val message = try {
                val connectionStore = ConnectionStore(applicationContext)
                val config = connectionStore.read()
                if (config == null) {
                    getString(R.string.shortcut_not_connected)
                } else {
                    val session = HaSession(config) { refreshed ->
                        connectionStore.save(HaConnectionConfig(config.baseUrl, refreshed))
                    }
                    session.restApi.callService(
                        HaServiceCall(domain = "homeassistant", service = "toggle", entityId = entityId),
                    )
                    getString(R.string.shortcut_toggled, label)
                }
            } catch (e: Exception) {
                getString(R.string.shortcut_failed)
            }
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    companion object {
        const val EXTRA_ENTITY_ID = "entity_id"
        const val EXTRA_LABEL = "label"
    }
}
