package dev.domus.android.nfc

import android.app.Activity
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Handler
import android.os.Looper
import java.io.IOException

/**
 * Wraps [NfcAdapter.enableReaderMode] to write one [NfcTagFormat] message to the next tag the
 * user taps. [NfcAdapter.ReaderCallback.onTagDiscovered] fires on a binder thread, so results
 * are always delivered back on the main thread.
 */
class NfcTagWriter(private val activity: Activity) {
    private val adapter = NfcAdapter.getDefaultAdapter(activity)
    private val mainHandler = Handler(Looper.getMainLooper())

    val isAvailable: Boolean get() = adapter != null

    fun startWriting(entityId: String, onResult: (Result<Unit>) -> Unit) {
        val nfcAdapter = adapter ?: run {
            onResult(Result.failure(IllegalStateException("NFC not available")))
            return
        }
        nfcAdapter.enableReaderMode(
            activity,
            { tag ->
                val result = runCatching { writeToTag(tag, NfcTagFormat.buildMessage(entityId)) }
                nfcAdapter.disableReaderMode(activity)
                mainHandler.post { onResult(result) }
            },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V,
            null,
        )
    }

    fun stop() {
        adapter?.disableReaderMode(activity)
    }

    private fun writeToTag(tag: Tag, message: NdefMessage) {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            ndef.connect()
            try {
                if (!ndef.isWritable) throw IOException("Tag is read-only")
                if (message.toByteArray().size > ndef.maxSize) throw IOException("Message too large for this tag")
                ndef.writeNdefMessage(message)
            } finally {
                ndef.close()
            }
            return
        }
        val formatable = NdefFormatable.get(tag) ?: throw IOException("This tag doesn't support NDEF")
        formatable.connect()
        try {
            formatable.format(message)
        } finally {
            formatable.close()
        }
    }
}
