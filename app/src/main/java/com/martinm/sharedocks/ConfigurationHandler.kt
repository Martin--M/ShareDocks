package com.martinm.sharedocks

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import java.lang.StringBuilder

object ConfigurationHandler {
    private lateinit var mContext: Context
    private lateinit var mSettings: SharedPreferences

    private fun buildStationStorageKey(): String {
        val info = CityUtils.map[CityUtils.currentCity] ?: return "user_docks"

        return "user_docks_${info.country}_${info.name}"
    }

    fun initialize(context: Context) {
        mContext = context
        mSettings = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun storeStationList(list: MutableList<ShareStation>) {
        val strBuilder = StringBuilder()
        list.forEach {
            strBuilder.append(it.id)
                .append(",")
        }
        with(mSettings.edit()) {
            putString(buildStationStorageKey(), strBuilder.toString())
            apply()
        }
    }

    fun stationIdListFromStorageString(): MutableList<String> {
        val list = mutableListOf<String>()
        mSettings.getString(buildStationStorageKey(), "")?.split(",")?.forEach {
            if (it != "") {
                list.add(it)
            }
        }
        return list
    }

    fun getTrackingEnabled(): Boolean {
        return mSettings.getBoolean("settings_enable_tracking", true)
    }

    fun getUIResponsiveness(): Int {
        return mSettings.getInt("settings_load_responsiveness", 50)
    }

    fun getColorOnMarkers(): Boolean {
        return mSettings.getBoolean("settings_is_colors_on_markers", false)
    }

    fun getTrackingUpdatePeriodSec(): Int {
        return mSettings.getInt("tracking_period_s", 30)
    }

    fun getExclusiveAudioEnabled(): Boolean {
        return mSettings.getBoolean("settings_exclusive_audio", true)
    }

    fun getCityId(): Int {
        val strId = mSettings.getString("settings_current_city", "")
        if (strId == null || strId == "") {
            return 0
        }
        return strId.toInt()
    }

    fun getTtsEnabled(): Boolean {
        return mSettings.getBoolean("settings_enable_tts", true)
    }
}