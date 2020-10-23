package com.martinm.sharedocks

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap

object Utils {
    private val mApi = ShareApiHandler
    var favoritesPopup: PopupWindow? = null

    private fun containsId(list: MutableList<ShareStation>, id: Int): ShareStation? {
        list.forEach {
            if (it.id == id) {
                return it
            }
        }
        return null
    }

    private fun addStation(list: MutableList<ShareStation>, station: ShareStation) {
        if (containsId(list, station.id) == null) {
            list.add(station)
        }
    }

    private fun removeStation(list: MutableList<ShareStation>, station: ShareStation) {
        val listStation: ShareStation? = containsId(list, station.id)
        if (listStation != null) {
            list.remove(listStation)
        }
    }

    fun loadUserDocks() {
        ConfigurationHandler.stationIdListFromStorageString().forEach {
            if (mApi.docks[it] != null) {
                addStation(LogicHandler.userDocks, mApi.docks[it]!!)
            }
        }
    }

    fun toggleUserDock(station: ShareStation) {
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
            mApi.loadDockLocations()
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

    fun safeUpdateDockStatus(context: Context) {
        try {
            mApi.updateDockStatus()
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
        userStations: MutableList<ShareStation>,
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
        if (mApi.docks[stationId] == null) {
            return ""
        }

        val stationTTS = mApi.docks[stationId]!!.name.replace(
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
            context.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            context.title = "Tracked stations"
            val favoritesView =
                (context.getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(
                    R.layout.tracked_stations_view,
                    context.findViewById(R.id.map),
                    false
                ) as RecyclerView
            val favoritesAdapter = FavoritesAdapter(LogicHandler.userDocks, map)
            favoritesView.setHasFixedSize(true)
            favoritesView.layoutManager = GridLayoutManager(context, 1)
            favoritesView.adapter = favoritesAdapter
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

            val touchHelper = ItemTouchHelper(object :
                ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    if (LogicHandler.userDocks.size > viewHolder.layoutPosition) {
                        LogicHandler.userDocks.removeAt(viewHolder.layoutPosition)
                        ConfigurationHandler.storeStationList(LogicHandler.userDocks)
                    }
                    favoritesAdapter.notifyItemRemoved(viewHolder.layoutPosition)
                }

            })
            touchHelper.attachToRecyclerView(favoritesView)
        }
    }

    fun setupSettingsButtonCallback(context: AppCompatActivity) {
        context.findViewById<ImageButton>(R.id.button_settings).setOnClickListener {
            context.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            context.title = "Preferences"
            context.supportFragmentManager
                .beginTransaction()
                .replace(R.id.map, BackgroundOverlayFragment())
                .replace(R.id.settings_background_fragment, SettingsFragment())
                .commit()
        }
    }

    fun centerMap(map: GoogleMap, zoom: Float) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(ShareStation.userLocation, zoom))
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings_definition, rootKey)
        }
    }

    class BackgroundOverlayFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout.settings_background_fragment, container, false)
        }
    }
}