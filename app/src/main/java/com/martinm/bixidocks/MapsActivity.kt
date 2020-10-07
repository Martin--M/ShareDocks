package com.martinm.bixidocks

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
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
    private lateinit var mLocationProvider: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest

    private val mBixi = BixiApiHandler
    private val mLogic = LogicHandler
    private var mIsPopupPresent: Boolean = false
    private var mIsLocationEnabled: Boolean = false
    private var mMarkers: MutableList<Marker> = mutableListOf()

    private var mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return
            if (locationResult.locations.size > 0) {
                if (mLogic.isReorderingNeeded(locationResult.locations[0])) {
                    mBixi.sortableDocks.sort()
                }
                BixiStation.userLocation = LatLng(
                    locationResult.locations[0].latitude,
                    locationResult.locations[0].longitude
                )
            }
            if (mLogic.isUserCloseToStation()) {
                mLogic.startTracking()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        NotificationHandler.initialize(
            this,
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        )
        mLocationProvider = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        mLocationRequest = LocationRequest.create()
            .setInterval(10000)
            .setFastestInterval(5000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMarkerClickListener(this)

        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            mIsLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_FINE_LOCATION
            )
        }
        centerMap()
        mMap.isMyLocationEnabled = mIsLocationEnabled

        thread(start = true) {
            val latch = CountDownLatch(1)
            mBixi.loadDockLocations()
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
        if (mIsLocationEnabled) {
            try {
                mLocationProvider.lastLocation.addOnCompleteListener {
                    if (it.isSuccessful && it.result != null) {
                        BixiStation.userLocation = LatLng(it.result.latitude, it.result.longitude)
                        mMap.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                BixiStation.userLocation,
                                14F
                            )
                        )
                    }
                }
                mMap.isMyLocationEnabled = mIsLocationEnabled
                // Start location updates
                mLocationProvider.requestLocationUpdates(
                    mLocationRequest,
                    mLocationCallback,
                    Looper.getMainLooper()
                )

                return
            } catch (e: SecurityException) {
                // Shouldn't happen since we check already if the user granted permissions
            }
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BixiStation.userLocation, 14F))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_FINE_LOCATION -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    mIsLocationEnabled = true
                    centerMap()
                }
            }
        }
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

    companion object {
        private const val REQUEST_CODE_FINE_LOCATION: Int = 1
    }
}