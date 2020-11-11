package com.martinm.sharedocks

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import java.time.Instant

class ShareStation(val id: String) : Comparable<ShareStation> {

    lateinit var name: String
    lateinit var lastUpdate: Instant
    var location: LatLng = LatLng(0.0, 0.0)
    var availableDocks: Int = 0
    var availableBikes: Int = 0
    var isActive: Boolean = false
    var hue: Float = 0F

    companion object {
        var userLocation = LatLng(0.0, 0.0)
    }

    private fun getAvailablePercent(): Float {
        if (availableBikes + availableDocks == 0 || !isActive) {
            return Float.NaN
        }
        return availableDocks.toFloat() / (availableBikes + availableDocks)
    }

    fun updateHue() {
        /*
         * Ranges from 15 (Warm Red) to 95 (Yellow Green)
         * Special case for disabled Stations at 240 (Blue), full stations at 0 (Red), and empty
         * stations at 120 (Green)
         */
        val availability = getAvailablePercent()
        hue = if (availability.isNaN()) {
            240F
        } else {
            when (availability) {
                0F -> 0F
                1F -> 120F
                else -> 15 + availability * 80
            }
        }
    }

    override fun compareTo(other: ShareStation): Int {
        val results: FloatArray = floatArrayOf(1F)
        Location.distanceBetween(
            location.latitude, location.longitude,
            userLocation.latitude, userLocation.longitude, results
        )
        val thisDistance = results[0]
        Location.distanceBetween(
            other.location.latitude, other.location.longitude,
            userLocation.latitude, userLocation.longitude, results
        )
        val otherDistance = results[0]

        // Meter precision. Should be good enough
        return (thisDistance - otherDistance).toInt()
    }
}