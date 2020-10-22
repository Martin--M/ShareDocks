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
    private val mUnavailableIds = mutableListOf<Int>()
    private lateinit var mTimerContext: Context
    private lateinit var mTextToSpeech: TextToSpeech
    private lateinit var mTrackingTimer: CountDownTimer

    const val RECEIVER_REQUEST_ID_ACTIVITY_TRANSITION = 0
    const val RECEIVER_REQUEST_ID_STOP_TRACKING = 1

    private val listener = TextToSpeech.OnInitListener {
        if (it == TextToSpeech.SUCCESS) {
            mTextToSpeech.language = Locale.getDefault()
        }
    }

    private fun createTrackingTimer(updatePeriodSec: Int) {
        mTrackingTimer =
            object : CountDownTimer(30 * 60 * 1000, (updatePeriodSec * 1000).toLong()) {
                override fun onFinish() {
                    // The tracking service will ensure the timer ends. Else we continue tracking
                    start()
                }

                override fun onTick(p0: Long) {
                    thread(start = true) {
                        val currentChanges = mutableMapOf<Int, Boolean>()
                        Utils.safeUpdateDockLocations(mTimerContext)

                        if (Utils.isStationStatusChanged(
                                userDocks,
                                mUnavailableIds,
                                currentChanges
                            )
                        ) {
                            val notificationDetails =
                                NotificationHandler.buildTrackingNotificationMessage(mUnavailableIds)

                            if (notificationDetails == "") {
                                NotificationHandler.removeTrackingNotifications()
                            } else {
                                val content = if (mUnavailableIds.size == 1) {
                                    mTimerContext.getString(R.string.notification_update_content_single)
                                } else {
                                    mTimerContext.getString(
                                        R.string.notification_update_content,
                                        mUnavailableIds.size
                                    )
                                }
                                NotificationHandler.showNotification(
                                    mTimerContext,
                                    mTimerContext.getString(R.string.notification_update_title),
                                    content,
                                    notificationDetails
                                )
                            }
                            // Wait for the notification alert to finish
                            sleep(2000)
                            currentChanges.forEach {
                                mTextToSpeech.speak(
                                    Utils.buildTrackingTTS(mTimerContext, it.key, it.value),
                                    TextToSpeech.QUEUE_ADD,
                                    null,
                                    ""
                                )
                            }
                        }
                    }
                }
            }
    }

    fun startTracking(context: Context) {
        if (isTracking) {
            return
        }
        isTracking = true
        mTextToSpeech = TextToSpeech(context, listener)
        createTrackingTimer(ConfigurationHandler.getTrackingUpdatePeriodSec())
        thread(start = true) {
            // Load docks again in case the whole context has been lost
            Utils.safeLoadDockLocations(context)
            Utils.loadUserDocks()

            mUnavailableIds.clear()
            mTimerContext = context

            mTrackingTimer.cancel()
            mTrackingTimer.start()
        }
    }

    fun stopTracking() {
        if (this::mTrackingTimer.isInitialized) {
            mTrackingTimer.cancel()
        }
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