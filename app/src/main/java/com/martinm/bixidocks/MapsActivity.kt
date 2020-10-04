package com.martinm.bixidocks

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlin.concurrent.thread

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var mLocationProvider: FusedLocationProviderClient
    private val mBixi = BixiApiHandler
    private var mIsPopupPresent: Boolean = false
    private var mIsLocationEnabled: Boolean = false
    private var mMarkers: MutableList<Marker> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        mLocationProvider = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMarkerClickListener(this)

        if (ContextCompat.checkSelfPermission(this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            mIsLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_FINE_LOCATION)
        }
        centerMap()
        mMap.isMyLocationEnabled = mIsLocationEnabled

        thread(start = true) {
            mBixi.loadDockLocations()
            mBixi.sortableDocks.sort()
            this.runOnUiThread {
                mBixi.sortableDocks.forEach {
                    val marker = mMap.addMarker(MarkerOptions().position(it.location))
                    marker.tag = it
                    mMarkers.add(marker)
                }
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
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BixiStation.userLocation, 14F))
                    }
                }
                mMap.isMyLocationEnabled = mIsLocationEnabled
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
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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

        popupView.findViewById<Button>(R.id.close_popup_button).setOnClickListener {
            popupWindow.dismiss()
            mIsPopupPresent = false
        }
        popupView.findViewById<TextView>(R.id.bikes_value).text = station.availableBikes.toString()
        popupView.findViewById<TextView>(R.id.docks_value).text = station.availableDocks.toString()
        popupView.findViewById<TextView>(R.id.dock_name).text = station.name

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