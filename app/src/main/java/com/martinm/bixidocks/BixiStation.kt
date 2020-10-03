package com.martinm.bixidocks

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
    ) {
}