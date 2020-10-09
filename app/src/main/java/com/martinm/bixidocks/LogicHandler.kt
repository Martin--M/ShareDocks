package com.martinm.bixidocks

import android.content.Context
import android.os.CountDownTimer

object LogicHandler {
    var userDocks = mutableListOf<BixiStation>()
    var isTracking: Boolean = false
    private val mBixi = BixiApiHandler

    private val mTrackingTimer = object : CountDownTimer(45 * 60 * 1000, 30 * 1000) {
        override fun onFinish() {
            isTracking = false
        }

        override fun onTick(p0: Long) {
            isTracking = true
            mBixi.updateDockLocations()
            userDocks.forEach {
                // There's been a change that affects the user
                if (!mBixi.docks[it.id]!!.isActive && it.isActive &&
                    mBixi.docks[it.id]!!.availableDocks == 0 && it.availableDocks != 0
                ) {
                    NotificationHandler.showNotification("0 docks at " + it.name)
                }
                mBixi.updateStation(mBixi.docks[it.id]!!, it)
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

    fun startTracking() {
        if (isTracking) {
            return
        }
        mTrackingTimer.start()
    }

    fun stopTracking() {
        mTrackingTimer.cancel()
        isTracking = false
    }
}