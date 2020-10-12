package com.martinm.bixidocks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StopTrackingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.startForegroundService(
            Intent().setClass(
                context,
                DocksTracker::class.java
            ).setAction(DocksTracker.FOREGROUND_SERVICE_STOP)
        )
    }
}