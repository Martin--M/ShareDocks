package com.martinm.bixidocks

import android.content.Context
import android.os.Handler
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap

object Utils {
    private val mBixi = BixiApiHandler
    var favoritesPopup: PopupWindow? = null

    private fun containsId(list: MutableList<BixiStation>, id: Int): BixiStation? {
        list.forEach {
            if (it.id == id) {
                return it
            }
        }
        return null
    }

    private fun addStation(list: MutableList<BixiStation>, station: BixiStation) {
        if (containsId(list, station.id) == null) {
            list.add(station)
        }
    }

    private fun removeStation(list: MutableList<BixiStation>, station: BixiStation) {
        val listStation: BixiStation? = containsId(list, station.id)
        if (listStation != null) {
            list.remove(listStation)
        }
    }

    fun loadUserDocks() {
        ConfigurationHandler.stationIdListFromStorageString().forEach {
            if (mBixi.docks[it] != null) {
                addStation(LogicHandler.userDocks, mBixi.docks[it]!!)
            }
        }
    }

    fun toggleUserDock(station: BixiStation) {
        if (containsId(LogicHandler.userDocks, station.id) == null) {
            addStation(LogicHandler.userDocks, station)
        } else {
            removeStation(LogicHandler.userDocks, station)
        }
    }

    fun getButtonStringForId(context: Context, id: Int): String {
        return if (containsId(LogicHandler.userDocks, id) != null) {
            context.getString(R.string.popup_button_remove)
        } else {
            context.getString(R.string.popup_button_add)
        }
    }

    fun safeLoadDockLocations(context: Context) {
        try {
            mBixi.loadDockLocations()
        } catch (e: Exception) {
            Handler(context.mainLooper).post {
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_error_network),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun safeUpdateDockLocations(context: Context) {
        try {
            mBixi.updateDockLocations()
        } catch (e: Exception) {
            Handler(context.mainLooper).post {
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_error_network),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun isStationStatusChanged(
        userStations: MutableList<BixiStation>,
        currentUnavailableIds: MutableList<Int>,
        newChanges: MutableMap<Int, Boolean>
    ): Boolean {
        var isChanged = false
        userStations.forEach {
            if (!it.isActive || it.availableDocks == 0) {
                if (!currentUnavailableIds.contains(it.id)) {
                    isChanged = true
                    currentUnavailableIds.add(it.id)
                    newChanges[it.id] = false
                }
            } else {
                if (currentUnavailableIds.contains(it.id)) {
                    isChanged = true
                    currentUnavailableIds.remove(it.id)
                    newChanges[it.id] = true
                }
            }
        }
        return isChanged
    }

    fun buildTrackingTTS(context: Context, stationId: Int, isAvailable: Boolean): String {
        if (mBixi.docks[stationId] == null) {
            return ""
        }

        val stationTTS = mBixi.docks[stationId]!!.name.replace(
            "/",
            context.getString(R.string.tts_replace_intersection)
        )

        if (isAvailable) {
            return context.getString(R.string.tts_update_available, stationTTS)
        }

        return context.getString(R.string.tts_update_full, stationTTS)
    }

    fun setupFavoritesButtonCallback(context: AppCompatActivity, map: GoogleMap) {
        context.findViewById<ImageButton>(R.id.button_favorites).setOnClickListener {
            val favoritesView =
                (context.getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(
                    R.layout.tracked_stations_view,
                    context.findViewById(R.id.map),
                    false
                ) as RecyclerView
            favoritesView.setHasFixedSize(true)
            favoritesView.layoutManager = GridLayoutManager(context, 1)
            favoritesView.adapter = FavoritesAdapter(LogicHandler.userDocks, map)
            favoritesView.addItemDecoration(
                DividerItemDecoration(
                    favoritesView.context,
                    DividerItemDecoration.VERTICAL
                )
            )

            favoritesPopup = PopupWindow(
                favoritesView,
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                context.findViewById<View>(R.id.map).height
            )
            favoritesPopup!!.showAtLocation(
                context.findViewById(R.id.map),
                Gravity.BOTTOM,
                0, 0
            )
        }
    }

    fun centerMap(map: GoogleMap, zoom: Float) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(BixiStation.userLocation, zoom))
    }
}