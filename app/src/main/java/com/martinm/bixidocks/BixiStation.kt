package com.martinm.bixidocks

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import java.time.Instant

class BixiStation(
    var location: LatLng,
    val id: Int,
    val name: String,
    var availableDocks: Int,
    var availableBikes: Int,
    var isActive: Boolean,
    var lastUpdate: Instant
    ): Comparable<BixiStation> {

    var hue: Float = 0F

    init {
        hue = getNewHue()
    }

    companion object {
        var userLocation = LatLng(0.0, 0.0)
    }

    private fun getAvailablePercent(): Float {
        if (availableBikes + availableDocks == 0 || !isActive) {
            return Float.NaN
        }
        return availableBikes.toFloat() / (availableBikes + availableDocks)
    }

    private fun getNewHue(): Float {
        /*
         * Ranges from 30 (Warm Red) to 90 (Yellow Green)
         * Special case for disabled Stations at 240 (Blue), empty stations at 0 (Red), and full
         * stations at 120 (Green)
         */
        val availability = getAvailablePercent()
        if (availability.isNaN()) {
            return 240F
        }
        return when (availability) {
            0F -> 0F
            1F -> 120F
            else -> 30 + availability * 60
        }
    }

    override fun compareTo(other: BixiStation): Int {
        val results: FloatArray = floatArrayOf(1F)
        Location.distanceBetween(location.latitude, location.longitude,
            userLocation.latitude, userLocation.longitude, results)
        val thisDistance = results[0]
        Location.distanceBetween(other.location.latitude, other.location.longitude,
            userLocation.latitude, userLocation.longitude, results)
        val otherDistance = results[0]

        // Meter precision. Should be good enough
        return (thisDistance - otherDistance).toInt()
    }
}