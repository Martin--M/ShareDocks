package com.martinm.bixidocks

import android.content.Context
import android.location.Location

object LogicHandler {
    var userDockIds = mutableListOf<Int>()
    var closestStations = mutableListOf<BixiStation>()
    private var mBixi = BixiApiHandler

    fun toggleUserDockId(id: Int) {
        if (!userDockIds.contains(id)) {
            userDockIds.add(id)
        } else {
            userDockIds.remove(id)
        }
    }

    fun getButtonStringForId(context: Context, id: Int): String {
        val res = context.resources
        return if (userDockIds.contains(id)) {
            res.getString(res.getIdentifier("popup_button_remove", "string", context.packageName))
        } else {
            res.getString(res.getIdentifier("popup_button_add", "string", context.packageName))
        }
    }

    fun isReorderingNeeded(newLocation: Location): Boolean {
        val results: FloatArray = floatArrayOf(1F)
        Location.distanceBetween(
            newLocation.latitude, newLocation.longitude,
            BixiStation.userLocation.latitude, BixiStation.userLocation.longitude, results
        )
        // We only reorder stations if the user has moved more than 100m
        if (results[0] <= 100) {
            return false
        }
        return true
    }

    fun isUserCloseToStation(): Boolean {
        val results: FloatArray = floatArrayOf(1F)
        if (mBixi.sortableDocks.size < 10) {
            return false
        }
        for (i in 0 until 10) {
            Location.distanceBetween(
                mBixi.sortableDocks[i].location.latitude, mBixi.sortableDocks[i].location.longitude,
                BixiStation.userLocation.latitude, BixiStation.userLocation.longitude, results
            )
            // Closer than 3m to the station, assume user is taking a bike
            if (results[0] <= 3) {
                return true
            }
        }
        return false
    }
}