package com.martinm.sharedocks

import android.app.Service
import android.content.Intent
import android.os.IBinder

class DocksTracker : Service() {
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action.equals(FOREGROUND_SERVICE_START)) {
            startForeground(
                NotificationHandler.NOTIFICATION_ID_TRACKING,
                NotificationHandler.getForegroundNotification(baseContext)
            )
            LogicHandler.startTracking(baseContext)
            return START_STICKY
        }
        // No need to check for FOREGROUND_SERVICE_STOP, just stop for safety
        LogicHandler.stopTracking(baseContext)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return START_NOT_STICKY
    }

    companion object {
        const val FOREGROUND_SERVICE_START = "com.martinm.sharedocks.action.starttracking"
        const val FOREGROUND_SERVICE_STOP = "com.martinm.sharedocks.action.stoptracking"
    }
}