package com.martinm.sharedocks

import android.content.Context
import android.content.SharedPreferences
import java.lang.StringBuilder

object ConfigurationHandler {
    private lateinit var mContext: Context
    private lateinit var mSettings: SharedPreferences

    fun initialize(context: Context) {
        mContext = context
        mSettings = context.getSharedPreferences("com.martinm.bixidocks_preferences", Context.MODE_PRIVATE)
    }

    fun storeStationList(list: MutableList<BixiStation>) {
        val strBuilder = StringBuilder()
        list.forEach {
            strBuilder.append(it.id)
                .append(",")
        }
        with(mSettings.edit()) {
            putString("user_docks", strBuilder.toString())
            apply()
        }
    }

    fun stationIdListFromStorageString(): MutableList<Int> {
        val list = mutableListOf<Int>()
        mSettings.getString("user_docks", "")?.split(",")?.forEach {
            if (it != "") {
                list.add(it.toInt())
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
}