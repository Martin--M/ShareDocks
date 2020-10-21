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
) : Comparable<BixiStation> {

    var hue: Float = 0F

    init {
        hue = getNewHue()
    }

    companion object {
        var userLocation = LatLng(0.0, 0.0)
    }

    fun copy(): BixiStation {
        return BixiStation(
            location = LatLng(this.location.latitude, this.location.longitude),
            name = this.name,
            lastUpdate = this.lastUpdate,
            isActive = this.isActive,
            availableDocks = this.availableDocks,
            availableBikes = this.availableBikes,
            id = this.id
        )
    }

    private fun getAvailablePercent(): Float {
        if (availableBikes + availableDocks == 0 || !isActive) {
            return Float.NaN
        }
        return availableBikes.toFloat() / (availableBikes + availableDocks)
    }

    private fun getNewHue(): Float {
        /*
         * Ranges from 15 (Warm Red) to 95 (Yellow Green)
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
            else -> 15 + availability * 80
        }
    }

    override fun compareTo(other: BixiStation): Int {
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