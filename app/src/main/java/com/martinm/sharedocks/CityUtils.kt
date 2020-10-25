package com.martinm.sharedocks

import com.google.android.gms.maps.model.LatLng
import java.net.URL

object CityUtils {

    var currentCity: Int = 0

    class CityInfo(
        val country: String,
        val name: String,
        val location: LatLng,
        val baseUrl: URL
    )

    // This should be consistent with the entries under array.xml @cityData & @cityId
    val map: Map<Int, CityInfo> = mapOf(
        1 to CityInfo(
            "CA",
            "Montréal",
            LatLng(45.5005302, -73.5686184),
            URL("https://api-core.bixi.com/gbfs/gbfs.json")
        )
    )
}