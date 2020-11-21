package com.martinm.sharedocks

import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.concurrent.CountDownLatch

object MapHandler {
    private val mApi = ShareApiHandler
    var isMapLoading = false
    var stopLoadRequest = false
    var requireVisualsUpdate = false

    fun overrideMapLoad() {
        if (isMapLoading) {
            stopLoadRequest = true
            while (isMapLoading) {
                Thread.sleep(10)
            }
        }
    }

    fun centerMap(map: GoogleMap, zoom: Float) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(ShareStation.userLocation, zoom))
    }

    fun setupMap(context: AppCompatActivity, map: GoogleMap, markers: MutableList<Marker>) {
        var latch = CountDownLatch(1)
        isMapLoading = true

        context.runOnUiThread {
            markers.forEach {
                it.remove()
            }
            latch.countDown()
        }
        latch.await()
        markers.clear()
        latch = CountDownLatch(1)

        Utils.safeLoadDockLocations(context)
        Utils.safeUpdateDockStatus(context)
        Utils.loadUserDocks()
        mApi.sortableDocks.sort()

        context.runOnUiThread {
            context.findViewById<ImageButton>(R.id.button_favorites).visibility = View.VISIBLE
            mApi.sortableDocks.forEach { station ->
                val marker = map.addMarker(MarkerOptions().position(station.location))
                marker.tag = station
                markers.add(marker)
            }
            latch.countDown()
        }
        latch.await()
        isMapLoading = false
    }

}