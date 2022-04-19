package com.martinm.sharedocks

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.widget.Toast
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import java.lang.Thread.sleep
import kotlin.concurrent.thread

object LogicHandler {
    var userDocks = mutableListOf<ShareStation>()
    var isTracking: Boolean = false
    private val mUnavailableIds = mutableListOf<String>()
    private lateinit var mTrackingTimer: CountDownTimer

    private fun createTrackingTimer(context: Context, updatePeriodSec: Int) {
        var mIsFirstTick = true
        mTrackingTimer =
            object : CountDownTimer(30 * 60 * 1000, (updatePeriodSec * 1000).toLong()) {
                override fun onFinish() {
                    // The tracking service will ensure the timer ends. Else we continue tracking
                    start()
                }

                override fun onTick(p0: Long) {
                    thread(start = true) {
                        val currentChanges = mutableMapOf<String, Boolean>()
                        if (ShareApiHandler.docks.isEmpty()) {
                            Utils.safeLoadDockLocations(context)
                            Utils.loadUserDocks()
                        } else {
                            try {
                                ShareApiHandler.loadStationUrls()
                            } catch (e: Exception) {
                                // No internet, skip tick
                                return@thread
                            }

                        }
                        Utils.safeUpdateDockStatus(context)
                        val isUpdateNeeded = Utils.isStationStatusChanged(
                            userDocks,
                            mUnavailableIds,
                            currentChanges
                        )

                        if (mIsFirstTick) {
                            if (ConfigurationHandler.getNotifyOnStart() && mUnavailableIds.isEmpty()) {
                                NotificationHandler.showNotification(
                                    context,
                                    context.getString(R.string.notification_on_start_title),
                                    context.getString(R.string.notification_on_start_summary)
                                )
                                if (ConfigurationHandler.getTtsEnabled()) {
                                    TtsHandler.initialize(context)
                                    // Wait for the notification alert to finish
                                    sleep(2000)
                                    TtsHandler.utteranceCount++
                                    TtsHandler.speak(context.getString(R.string.tts_on_start))
                                }
                            }
                            mIsFirstTick = false
                        }

                        if (!isUpdateNeeded
                        ) {
                            return@thread
                        }

                        if (ShareApiHandler.docks.isEmpty()) {
                            return@thread
                        }

                        if (shutdownDetected(ShareApiHandler.docks)) {
                            return@thread
                        }

                        val notificationDetails =
                            NotificationHandler.buildTrackingNotificationMessage(mUnavailableIds)

                        if (mUnavailableIds.isEmpty()) {
                            NotificationHandler.removeTrackingNotifications(
                                context.getSystemService(
                                    Context.NOTIFICATION_SERVICE
                                ) as NotificationManager
                            )
                        } else {
                            val content = context.resources.getQuantityString(
                                R.plurals.notification_update_content,
                                mUnavailableIds.size,
                                mUnavailableIds.size
                            )
                            NotificationHandler.showNotification(
                                context,
                                context.getString(R.string.notification_update_title),
                                content,
                                notificationDetails
                            )
                        }

                        if (ConfigurationHandler.getTtsEnabled()) {
                            TtsHandler.initialize(context)
                            // Wait for the notification alert to finish
                            sleep(2000)
                            currentChanges.forEach {
                                TtsHandler.utteranceCount++
                                TtsHandler.speak(
                                    TtsHandler.buildTrackingTTS(context, it.key, it.value)
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
        createTrackingTimer(context, ConfigurationHandler.getTrackingUpdatePeriodSec())
        thread(start = true) {
            // Load docks again in case the whole context has been lost
            CityUtils.currentCity = ConfigurationHandler.getCityId()
            Utils.safeLoadDockLocations(context)
            Utils.loadUserDocks()

            mUnavailableIds.clear()

            mTrackingTimer.cancel()
            mTrackingTimer.start()
        }
    }

    fun stopTracking(context: Context) {
        if (this::mTrackingTimer.isInitialized) {
            mTrackingTimer.cancel()
            NotificationHandler.removeTrackingNotifications(
                context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager
            )
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
            Utils.RECEIVER_REQUEST_ID_ACTIVITY_TRANSITION,
            Intent().setClass(context, ActivityTransitionReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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

    private fun shutdownDetected(stations: MutableMap<String, ShareStation>): Boolean {
        stations.forEach { (_, value) ->
            if (value.isActive) {
                return false
            }
        }
        return true
    }
}