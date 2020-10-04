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

    companion object {
        var userLocation = LatLng(0.0, 0.0)
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