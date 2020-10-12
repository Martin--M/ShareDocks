package com.martinm.bixidocks

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHandler {
    private lateinit var mNotificationManager: NotificationManager

    private const val CHANNEL_ID_UPDATES: String = "DocksUpdates"
    private const val CHANNEL_ID_TRACKING: String = "DocksTracking"

    fun initialize(notificationManager: NotificationManager) {
        mNotificationManager = notificationManager
        val chanUpdates = NotificationChannel(
            CHANNEL_ID_UPDATES,
            "DocksChannel",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "DocksDescription"
        }
        val chanTracking = NotificationChannel(
            CHANNEL_ID_TRACKING,
            "DocksTrackingChannel",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Channel used for sending tracking notification"
        }
        mNotificationManager.createNotificationChannel(chanUpdates)
        mNotificationManager.createNotificationChannel(chanTracking)
    }

    fun showNotification(context: Context, message: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_UPDATES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentTitle("Station full")
            .setContentText(message)

        with(NotificationManagerCompat.from(context)) {
            notify(100001, builder.build())
        }
    }

    fun getForegroundNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID_TRACKING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Station tracking in progress").build()
    }
}