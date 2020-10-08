package com.martinm.bixidocks

import android.content.Context
import android.content.SharedPreferences
import java.lang.StringBuilder

object ConfigurationHandler {
    private lateinit var mContext: Context
    private lateinit var mSettings: SharedPreferences

    fun initialize(context: Context) {
        mContext = context
        mSettings = context.getSharedPreferences("DocksSettings", Context.MODE_PRIVATE)
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
}