package com.martinm.sharedocks

import com.google.android.gms.maps.model.LatLng

object CityUtils {
    class CityInfo(
        val country: String,
        val name: String,
        val location: LatLng,
        val baseUrl: String
    )

    // This should be consistent with the entries under array.xml @cityData & @cityId
    val map: Map<Int, CityInfo> = mapOf(
        1 to CityInfo(
            "CA",
            "Montréal",
            LatLng(45.5005302, -73.5686184),
            "https://api-core.bixi.com/gbfs/en/"
        )
    )
}