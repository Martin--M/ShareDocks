package com.martinm.bixidocks

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlin.concurrent.thread

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap
    private val mBixi = BixiApiHandler
    private var mIsPopupPresent: Boolean = false

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
                    mMap.addMarker(MarkerOptions().position(it.value.location))
                        .tag = it.value.id
                    mMap.setOnMarkerClickListener(this)
                }
            });
        }
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        if (p0 == null || mIsPopupPresent) {
            // Have to return true, else the default snippet pops up
            return true
        }

        val popupView = (getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            .inflate(R.layout.dock_popup, null)
        val popupWindow = PopupWindow(
            popupView,
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )

        popupView.findViewById<Button>(R.id.close_popup_button).setOnClickListener {
            popupWindow.dismiss()
            mIsPopupPresent = false
        }

        popupView.findViewById<TextView>(R.id.bikes_value).text = mBixi.docks[p0.tag]?.availableBikes.toString()
        popupView.findViewById<TextView>(R.id.docks_value).text = mBixi.docks[p0.tag]?.availableDocks.toString()
        popupView.findViewById<TextView>(R.id.dock_name).text = mBixi.docks[p0.tag]?.name

        popupWindow.showAtLocation(
            findViewById(R.id.map),
            Gravity.CENTER,
            0, 0
        )

        mIsPopupPresent = true
        return true
    }
}