package com.martinm.bixidocks

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        NotificationHandler.initialize(
            this,
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        )
        ConfigurationHandler.initialize(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this.applicationContext,
                    "android.permission.ACTIVITY_RECOGNITION"
                )
                == PackageManager.PERMISSION_GRANTED
            ) {
                mLogic.setupActivityRecognitionCallback(applicationContext)
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf("android.permission.ACTIVITY_RECOGNITION"),
                    REQUEST_CODE_ACTIVITY_RECOGNITION
                )
            }
        } else {
            mLogic.setupActivityRecognitionCallback(applicationContext)
        }

        // Default user location to downtown Montreal
        BixiStation.userLocation = LatLng(45.5005302, -73.5686184)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                Utils.favoritesPopup?.dismiss()
                return true
            }
        }
        return false
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMarkerClickListener(this)
        Utils.setupFavoritesButtonCallback(this, mMap)

        Utils.centerMap(mMap, 14F)

        thread(start = true) {
            val latch = CountDownLatch(1)

            Utils.safeLoadDockLocations(this)
            Utils.loadUserDocks()
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
                sleep(50)
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
            Utils.toggleUserDock(station)
            popupView.findViewById<Button>(R.id.toggle_dock_button).text =
                Utils.getButtonStringForId(baseContext, station.id)
            ConfigurationHandler.storeStationList(mLogic.userDocks)
        }

        // Load labels with appropriate values
        popupView.findViewById<TextView>(R.id.bikes_value).text = station.availableBikes.toString()
        popupView.findViewById<TextView>(R.id.docks_value).text = station.availableDocks.toString()
        popupView.findViewById<TextView>(R.id.dock_name).text = station.name
        popupView.findViewById<Button>(R.id.toggle_dock_button).text =
            Utils.getButtonStringForId(baseContext, station.id)

        popupWindow.showAtLocation(
            findViewById(R.id.map),
            Gravity.CENTER,
            0, 0
        )

        mIsPopupPresent = true
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_ACTIVITY_RECOGNITION -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    mLogic.setupActivityRecognitionCallback(applicationContext)
                }
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_ACTIVITY_RECOGNITION: Int = 1
    }
}