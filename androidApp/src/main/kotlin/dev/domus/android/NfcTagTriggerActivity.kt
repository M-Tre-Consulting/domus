package dev.domus.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dev.domus.android.data.ConnectionStore
import dev.domus.android.nfc.NfcTagFormat
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.HaConnectionConfig
import dev.domus.shared.model.HaServiceCall
import kotlinx.coroutines.launch

/**
 * Headless trampoline the system launches directly when the user taps a Domus-written NFC tag
 * (see [dev.domus.android.nfc.NfcTagFormat] and the intent-filter in the manifest) — works even
 * if the app isn't already running. Activates the encoded scene/script and finishes.
 */
class NfcTagTriggerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val entityId = NfcTagFormat.extractEntityId(intent)
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
                    val domain = entityId.substringBefore('.', missingDelimiterValue = "scene")
                    session.restApi.callService(
                        HaServiceCall(domain = domain, service = "turn_on", entityId = entityId),
                    )
                    getString(R.string.nfc_triggered, entityId)
                }
            } catch (e: Exception) {
                getString(R.string.shortcut_failed)
            }
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
