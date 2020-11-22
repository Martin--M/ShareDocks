package com.martinm.sharedocks

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Marker
import java.time.Instant
import kotlin.concurrent.thread

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap

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
        Utils.setupSettingsButtonCallback(this)

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

        CityUtils.currentCity = ConfigurationHandler.getCityId()

        if (CityUtils.currentCity != 0) {
            ShareStation.userLocation = CityUtils.map[CityUtils.currentCity]?.location!!
        }
    }

    private fun handleBackButton() {
        if (Utils.isNestedSetting) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.map, Utils.BackgroundOverlayFragment())
                .replace(R.id.settings_background_fragment, Utils.SettingsFragment())
                .commit()
            Utils.isNestedSetting = false
            return
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        title = getString(R.string.app_name)
        Utils.favoritesPopup?.dismiss()
        for (fragment in supportFragmentManager.fragments) {
            if (fragment is SupportMapFragment) {
                continue
            }
            supportFragmentManager.beginTransaction().remove(fragment).commit()
        }

        if (CityUtils.currentCity != ConfigurationHandler.getCityId()) {
            CityUtils.currentCity = ConfigurationHandler.getCityId()
            ShareApiHandler.lastCalled = Instant.MIN
            ShareStation.userLocation = CityUtils.map[CityUtils.currentCity]?.location!!
            findViewById<ImageButton>(R.id.button_favorites).visibility = View.GONE
            MapHandler.centerMap(mMap, 14F)
            thread(start = true) {
                MapHandler.waitForMapLoad()
                MapHandler.setupMap(this, mMap, mMarkers)
            }
        } else if (MapHandler.requireVisualsUpdate) {
            thread(start = true) {
                //TODO: Update visuals
            }
        }
    }

    override fun onBackPressed() {
        handleBackButton()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                handleBackButton()
                return true
            }
            R.id.menu_refresh_button ->
                if (ShareApiHandler.docks.isEmpty()) {
                    thread(start = true) {
                        MapHandler.waitForMapLoad()
                        MapHandler.setupMap(this, mMap, mMarkers)
                    }
                } else {
                    thread(start = true) {
                        Utils.safeUpdateDockStatus(this)
                        //TODO: Update visuals
                    }
                }
        }
        return false
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMarkerClickListener(this)
        Utils.setupFavoritesButtonCallback(this, mMap)

        if (CityUtils.currentCity != 0) {
            MapHandler.centerMap(mMap, 14F)
            thread(start = true) {
                MapHandler.setupMap(this, mMap, mMarkers)
            }
        } else {
            thread(start = true) {
                Utils.showNoCitySelectedPopup(this)
            }
        }
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        if (p0 == null || mIsPopupPresent) {
            // Have to return true, else the default snippet pops up
            return true
        }

        val station = p0.tag as ShareStation

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
        }

        // Listener for the Toggle button
        popupView.findViewById<Button>(R.id.toggle_dock_button).setOnClickListener {
            Utils.toggleUserDock(station)
            popupView.findViewById<Button>(R.id.toggle_dock_button).text =
                Utils.getButtonStringForId(baseContext, station.id)
            ConfigurationHandler.storeStationList(mLogic.userDocks)
        }

        val nicknameView = popupView.findViewById<TextView>(R.id.current_nickname_text)
        popupView.findViewById<Button>(R.id.set_nickname_button).setOnClickListener {
            val nicknameFragment = NicknameEdit(
                station.id,
                popupView.parent as ViewGroup,
                nicknameView
            )
            nicknameFragment.show(supportFragmentManager.beginTransaction(), "nickNameEditTag")
        }

        nicknameView.text = ConfigurationHandler.getNickname(station.id)
        if (nicknameView.text == "") {
            nicknameView.visibility = View.GONE
            popupView.findViewById<TextView>(R.id.dock_name).visibility = View.VISIBLE
        } else {
            nicknameView.visibility = View.VISIBLE
            popupView.findViewById<TextView>(R.id.dock_name).visibility = View.GONE
        }

        // Load labels with appropriate values
        popupView.findViewById<TextView>(R.id.bikes_value).text = station.availableBikes.toString()
        popupView.findViewById<TextView>(R.id.docks_value).text = station.availableDocks.toString()
        popupView.findViewById<TextView>(R.id.dock_name).text = station.name
        popupView.findViewById<Button>(R.id.toggle_dock_button).text =
            Utils.getButtonStringForId(baseContext, station.id)

        popupWindow.setOnDismissListener {
            mIsPopupPresent = false
        }

        popupWindow.isFocusable = true

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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.support_bar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    companion object {
        private const val REQUEST_CODE_ACTIVITY_RECOGNITION: Int = 1
    }
}