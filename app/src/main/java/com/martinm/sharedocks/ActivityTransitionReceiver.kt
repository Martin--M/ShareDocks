package com.martinm.sharedocks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

class ActivityTransitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) {
            return
        }
        val result = ActivityTransitionResult.extractResult(intent)!!
        for (event in result.transitionEvents) {
            if (event.activityType == DetectedActivity.ON_BICYCLE) {
                if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                    if (ConfigurationHandler.getTrackingEnabled()) {
                        context.startForegroundService(
                            Intent().setClass(
                                context,
                                DocksTracker::class.java
                            ).setAction(DocksTracker.FOREGROUND_SERVICE_START)
                        )
                    }
                } else if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                    context.startService(
                        Intent().setClass(
                            context,
                            DocksTracker::class.java
                        ).setAction(DocksTracker.FOREGROUND_SERVICE_STOP)
                    )
                }
            }
        }
    }
}