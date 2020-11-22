package com.martinm.sharedocks

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

object ConfigurationHandler {
    private lateinit var mSettings: SharedPreferences

    private fun buildStationStorageKey(): String {
        return "user_docks_${CityUtils.currentCity}"
    }

    private fun buildFavoritesStorageKey(stationId: String): String {
        return "${CityUtils.currentCity}_${stationId}"
    }

    fun initialize(context: Context) {
        mSettings = PreferenceManager.getDefaultSharedPreferences(context)
        mSettings.registerOnSharedPreferenceChangeListener { _, key ->
            if (key.startsWith("settings_visuals")) {
                MapHandler.requireVisualsUpdate = true
            }
        }
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

    fun storeNickname(stationId: String, nickname: String) {
        with(mSettings.edit()) {
            putString(buildFavoritesStorageKey(stationId), nickname)
            apply()
        }
    }

    fun getNickname(stationId: String): String {
        return mSettings.getString(buildFavoritesStorageKey(stationId), "") ?: return ""
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

    fun getColorOnMarkers(): Boolean {
        return mSettings.getBoolean("settings_visuals_is_colors_on_markers", false)
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

    fun getShowUnavailableStations(): Boolean {
        return mSettings.getBoolean("settings_visuals_show_unavailable_stations", false)
    }

    fun getNotifyOnStart(): Boolean {
        return mSettings.getBoolean("settings_notify_on_start", false)
    }
}