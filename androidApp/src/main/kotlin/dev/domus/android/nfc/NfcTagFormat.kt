package dev.domus.android.nfc

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import androidx.core.content.IntentCompat

/**
 * How Domus encodes an entity id (a `scene.*`/`script.*` entity) onto an NFC tag: a single
 * NDEF Forum "external type" record, `dev.domus.android:scene`, whose payload is the raw
 * entity id. Matched by the intent-filter on [dev.domus.android.NfcTagTriggerActivity].
 */
object NfcTagFormat {
    private const val DOMAIN = "dev.domus.android"
    private const val TYPE = "scene"

    fun buildMessage(entityId: String): NdefMessage {
        val record = NdefRecord.createExternal(DOMAIN, TYPE, entityId.toByteArray(Charsets.UTF_8))
        return NdefMessage(arrayOf(record))
    }

    fun extractEntityId(intent: Intent): String? {
        val messages = IntentCompat.getParcelableArrayExtra(
            intent,
            NfcAdapter.EXTRA_NDEF_MESSAGES,
            NdefMessage::class.java,
        ) ?: return null
        val message = messages.firstOrNull() as? NdefMessage ?: return null
        val expectedType = "$DOMAIN:$TYPE"
        val record = message.records.firstOrNull {
            it.tnf == NdefRecord.TNF_EXTERNAL_TYPE && String(it.type, Charsets.UTF_8) == expectedType
        } ?: return null
        return String(record.payload, Charsets.UTF_8)
    }
}
