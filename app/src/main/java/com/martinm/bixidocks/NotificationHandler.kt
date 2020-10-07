package com.martinm.bixidocks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHandler {
    private lateinit var mContext: Context
    private lateinit var mNotificationManager: NotificationManager
    private const val mChannelID: String = "DocksUpdates"
    private lateinit var mBuilder: NotificationCompat.Builder
    private lateinit var mChannel: NotificationChannel

    fun initialize(context: Context, notificationManager: NotificationManager) {
        mContext = context
        mNotificationManager = notificationManager
        mChannel =
            NotificationChannel(mChannelID, "DocksChannel", NotificationManager.IMPORTANCE_HIGH)
        mBuilder = NotificationCompat.Builder(mContext, mChannelID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentTitle("Station full")
        mChannel.apply {
            description = "DocksDescription"
        }
        mNotificationManager.createNotificationChannel(mChannel)
    }

    fun showNotification(message: String) {
        mBuilder.setContentText(message)
        with(NotificationManagerCompat.from(mContext)) {
            notify(100001, mBuilder.build())
        }
    }
}