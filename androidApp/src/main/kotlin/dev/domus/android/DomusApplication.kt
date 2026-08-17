package dev.domus.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class DomusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                HaConnectionService.CHANNEL_ID,
                "Connection status",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shown while Domus keeps the Home Assistant connection alive in the background."
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
