package com.martinm.bixidocks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHandler {
    private lateinit var mNotificationManager: NotificationManager
    private const val mChannelID: String = "DocksUpdates"
    private lateinit var mBuilder: NotificationCompat.Builder
    private lateinit var mChannel: NotificationChannel

    fun initialize(notificationManager: NotificationManager) {
        mNotificationManager = notificationManager
        mChannel =
            NotificationChannel(mChannelID, "DocksChannel", NotificationManager.IMPORTANCE_HIGH)
        mChannel.apply {
            description = "DocksDescription"
        }
        mNotificationManager.createNotificationChannel(mChannel)
    }

    fun showNotification(context: Context, message: String) {
        mBuilder = NotificationCompat.Builder(context, mChannelID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentTitle("Station full")

        mBuilder.setContentText(message)
        with(NotificationManagerCompat.from(context)) {
            notify(100001, mBuilder.build())
        }
    }
}