package com.martinm.bixidocks

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import java.lang.Thread.sleep
import java.util.*
import kotlin.concurrent.thread

object LogicHandler {
    var userDocks = mutableListOf<BixiStation>()
    var isTracking: Boolean = false
    private val mBixi = BixiApiHandler
    private lateinit var mTimerContext: Context
    private lateinit var mTextToSpeech: TextToSpeech

    const val RECEIVER_REQUEST_ID_ACTIVITY_TRANSITION = 0
    const val RECEIVER_REQUEST_ID_STOP_TRACKING = 1

    private val listener = TextToSpeech.OnInitListener {
        if (it == TextToSpeech.SUCCESS) {
            mTextToSpeech.language = Locale.getDefault()
        }
    }

    private val mTrackingTimer = object : CountDownTimer(30 * 60 * 1000, 30 * 1000) {
        override fun onFinish() {
            // The tracking service will ensure the timer ends. Else we continue tracking
            start()
        }

        override fun onTick(p0: Long) {
            isTracking = true
            thread(start = true) {
                Utils.safeUpdateDockLocations(mTimerContext)
                userDocks.forEach {
                    // There's been a change that affects the user
                    if (!mBixi.docks[it.id]!!.isActive && it.isActive &&
                        mBixi.docks[it.id]!!.availableDocks == 0 && it.availableDocks != 0
                    ) {
                        NotificationHandler.showNotification(
                            mTimerContext,
                            mTimerContext.getString(R.string.notification_update_content, it.name)
                        )
                        // Wait for the notification alert to finish
                        sleep(2000)
                        mTextToSpeech.speak(
                            mTimerContext.getString(
                                R.string.tts_update,
                                it.name.replace(
                                    "/",
                                    mTimerContext.getString(R.string.tts_replace_intersection)
                                )
                            ),
                            TextToSpeech.QUEUE_ADD,
                            null,
                            ""
                        )
                    }
                    mBixi.updateStation(mBixi.docks[it.id]!!, it)
                }
            }
        }
    }

    fun startTracking(context: Context) {
        if (isTracking) {
            return
        }
        mTextToSpeech = TextToSpeech(context, listener)
        thread(start = true) {
            // Load docks again in case the whole context has been lost
            Utils.safeLoadDockLocations(context)

            Utils.loadUserDocks()
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
                context.getString(R.string.toast_activity_recognition_connection_success),
                Toast.LENGTH_LONG
            ).show()
        }
        task.addOnFailureListener {
            Toast.makeText(
                context,
                context.getString(R.string.toast_activity_recognition_connection_failure),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}