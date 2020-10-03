package com.martinm.bixidocks

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlin.concurrent.thread

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val mBixi = BixiApiHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        thread(start = true) {
            mBixi.loadDockLocations()
            this.runOnUiThread(Runnable {
                mBixi.docks.forEach {
                    mMap.addMarker(
                        MarkerOptions()
                            .position(it.value.location)
                            .title(it.value.name)
                            .snippet("Bikes: " + it.value.availableBikes + System.lineSeparator() +
                                     "Docks: " + it.value.availableDocks + System.lineSeparator() +
                                     "ID: " + it.value.id))
                }
            });
        }
    }
}