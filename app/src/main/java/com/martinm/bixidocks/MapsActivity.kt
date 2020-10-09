package com.martinm.bixidocks

import android.app.NotificationManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.gms.maps.CameraUpdateFactory

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.lang.Thread.sleep
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap

    private val mBixi = BixiApiHandler
    private val mLogic = LogicHandler
    private var mIsPopupPresent: Boolean = false
    private var mMarkers: MutableList<Marker> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        NotificationHandler.initialize(
            this,
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        )
        ConfigurationHandler.initialize(this)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMarkerClickListener(this)

        centerMap()

        thread(start = true) {
            val latch = CountDownLatch(1)
            mBixi.loadDockLocations()
            mLogic.loadUserDocks()
            mBixi.sortableDocks.sort()
            this.runOnUiThread {
                mBixi.sortableDocks.forEach {
                    val marker = mMap.addMarker(MarkerOptions().position(it.location))
                    marker.tag = it
                    mMarkers.add(marker)
                }
                latch.countDown()
            }
            // Sync with the initialization of the markers
            latch.await()
            mMarkers.forEach {
                this.runOnUiThread {
                    it.setIcon(
                        BitmapDescriptorFactory.defaultMarker(
                            (it.tag as BixiStation).hue
                        )
                    )
                }
                // Unblock UI thread to allow for responsiveness
                sleep(10)
            }
        }
    }

    private fun centerMap() {
        // Default to downtown Montreal
        BixiStation.userLocation = LatLng(45.5005302, -73.5686184)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BixiStation.userLocation, 14F))
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        if (p0 == null || mIsPopupPresent) {
            // Have to return true, else the default snippet pops up
            return true
        }

        val station = p0.tag as BixiStation

        val popupView = (getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            .inflate(R.layout.dock_popup, findViewById(R.id.map), false)
        val popupWindow = PopupWindow(
            popupView,
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )

        // Listener for the Close button
        popupView.findViewById<Button>(R.id.close_popup_button).setOnClickListener {
            popupWindow.dismiss()
            mIsPopupPresent = false
        }

        // Listener for the Toggle button
        popupView.findViewById<Button>(R.id.toggle_dock_button).setOnClickListener {
            mLogic.toggleUserDock(station)
            popupView.findViewById<Button>(R.id.toggle_dock_button).text =
                mLogic.getButtonStringForId(baseContext, station.id)
            ConfigurationHandler.storeStationList(mLogic.userDocks)
        }

        // Load labels with appropriate values
        popupView.findViewById<TextView>(R.id.bikes_value).text = station.availableBikes.toString()
        popupView.findViewById<TextView>(R.id.docks_value).text = station.availableDocks.toString()
        popupView.findViewById<TextView>(R.id.dock_name).text = station.name
        popupView.findViewById<Button>(R.id.toggle_dock_button).text =
            mLogic.getButtonStringForId(baseContext, station.id)

        popupWindow.showAtLocation(
            findViewById(R.id.map),
            Gravity.CENTER,
            0, 0
        )

        mIsPopupPresent = true
        return true
    }
}