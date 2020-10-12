package com.martinm.bixidocks

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.widget.Toast
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import java.lang.Exception
import kotlin.concurrent.thread

object LogicHandler {
    var userDocks = mutableListOf<BixiStation>()
    var isTracking: Boolean = false
    private val mBixi = BixiApiHandler
    private lateinit var mTimerContext: Context

    const val RECEIVER_REQUEST_ID_ACTIVITY_TRANSITION = 0
    const val RECEIVER_REQUEST_ID_STOP_TRACKING = 1

    private val mTrackingTimer = object : CountDownTimer(30 * 60 * 1000, 30 * 1000) {
        override fun onFinish() {
            // The tracking service will ensure the timer ends. Else we continue tracking
            start()
        }

        override fun onTick(p0: Long) {
            isTracking = true
            thread(start = true) {
                try {
                    mBixi.updateDockLocations()
                } catch (e: Exception) {
                    Toast.makeText(
                        mTimerContext,
                        "Error getting station data: $e",
                        Toast.LENGTH_LONG
                    ).show()
                }
                userDocks.forEach {
                    // There's been a change that affects the user
                    if (!mBixi.docks[it.id]!!.isActive && it.isActive &&
                        mBixi.docks[it.id]!!.availableDocks == 0 && it.availableDocks != 0
                    ) {
                        NotificationHandler.showNotification(mTimerContext, "0 docks at " + it.name)
                    }
                    mBixi.updateStation(mBixi.docks[it.id]!!, it)
                }
            }
        }
    }

    private fun containsId(list: MutableList<BixiStation>, id: Int): BixiStation? {
        list.forEach {
            if (it.id == id) {
                return it
            }
        }
        return null
    }

    private fun addStation(list: MutableList<BixiStation>, station: BixiStation) {
        if (containsId(list, station.id) == null) {
            list.add(station)
        }
    }

    private fun removeStation(list: MutableList<BixiStation>, station: BixiStation) {
        val listStation: BixiStation? = containsId(list, station.id)
        if (listStation != null) {
            list.remove(listStation)
        }
    }

    fun loadUserDocks() {
        ConfigurationHandler.stationIdListFromStorageString().forEach {
            if (mBixi.docks[it] != null) {
                addStation(userDocks, mBixi.docks[it]!!.copy())
            }
        }
    }

    fun toggleUserDock(station: BixiStation) {
        if (containsId(userDocks, station.id) == null) {
            addStation(userDocks, station.copy())
        } else {
            removeStation(userDocks, station)
        }
    }

    fun getButtonStringForId(context: Context, id: Int): String {
        val res = context.resources
        return if (containsId(userDocks, id) != null) {
            res.getString(res.getIdentifier("popup_button_remove", "string", context.packageName))
        } else {
            res.getString(res.getIdentifier("popup_button_add", "string", context.packageName))
        }
    }

    fun startTracking(context: Context) {
        if (isTracking) {
            return
        }
        thread(start = true) {
            // Load docks again in case the whole context has been lost
            try {
                mBixi.loadDockLocations()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error getting station data: $e",
                    Toast.LENGTH_LONG
                ).show()
            }

            loadUserDocks()
            mTimerContext = context
            mTrackingTimer.start()
        }
    }

    fun stopTracking() {
        mTrackingTimer.cancel()
        isTracking = false
    }

    fun setupActivityRecognitionCallback(context: Context) {
        val activityTransitions = listOf<ActivityTransition>(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )
        val activityTransitionRequest = ActivityTransitionRequest(activityTransitions)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            RECEIVER_REQUEST_ID_ACTIVITY_TRANSITION,
            Intent().setClass(context, ActivityTransitionReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val task = ActivityRecognition.getClient(context)
            .requestActivityTransitionUpdates(activityTransitionRequest, pendingIntent)

        task.addOnSuccessListener {
            Toast.makeText(
                context,
                "Connected to the Activity Recognition Service",
                Toast.LENGTH_LONG
            ).show()
        }
        task.addOnFailureListener {
            Toast.makeText(
                context,
                "Failed to connect to the Activity Recognition Service. Restart the application",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}