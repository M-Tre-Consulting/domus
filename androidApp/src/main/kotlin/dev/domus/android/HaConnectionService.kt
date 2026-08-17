package dev.domus.android

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dev.domus.android.data.HaSessionHolder
import dev.domus.shared.data.WebSocketState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the process (and [HaSessionHolder]'s realtime WebSocket)
 * alive while the app is backgrounded, so widgets/tiles/notifications stay current instead of
 * silently going stale the moment Android reclaims the process. It doesn't open a connection
 * of its own — it just shields the existing session and mirrors its status in the notification.
 */
class HaConnectionService : Service() {
    private var wsStateJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val session = HaSessionHolder.session
        if (session == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification(WebSocketState.Connecting))

        wsStateJob?.cancel()
        wsStateJob = CoroutineScope(Dispatchers.Default).launch {
            session.repository.wsState.collect { state ->
                ContextCompat.getSystemService(this@HaConnectionService, android.app.NotificationManager::class.java)
                    ?.notify(NOTIFICATION_ID, buildNotification(state))
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        wsStateJob?.cancel()
        super.onDestroy()
    }

    private fun buildNotification(state: WebSocketState): Notification {
        val statusText = when (state) {
            WebSocketState.Connected -> getString(R.string.service_connected)
            WebSocketState.Connecting -> getString(R.string.service_connecting)
            WebSocketState.Reconnecting -> getString(R.string.service_reconnecting)
        }
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "domus_connection"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, HaConnectionService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, HaConnectionService::class.java))
        }
    }
}
