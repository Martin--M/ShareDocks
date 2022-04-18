package com.martinm.sharedocks

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.GoogleMap
import kotlinx.android.synthetic.main.tracked_stations_entry.view.*
import java.lang.StringBuilder

class FavoritesAdapter(private val docks: MutableList<ShareStation>, private val map: GoogleMap) :
    RecyclerView.Adapter<FavoritesAdapter.FavoritesViewHolder>() {

    class FavoritesViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun setText(text: String) {
            view.tracked_stations_entry_text_view.text = text
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoritesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.tracked_stations_entry, parent, false)

        val viewHolder = FavoritesViewHolder(view)

        view.sort_icon.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    FavoritesHandler.startDragging(viewHolder)
                }
                MotionEvent.ACTION_UP -> v.performClick()
            }

            return@setOnTouchListener true
        }
        return viewHolder
    }

    override fun getItemCount(): Int {
        return docks.size
    }

    override fun onBindViewHolder(holder: FavoritesViewHolder, position: Int) {
        var displayName = ConfigurationHandler.getNickname(docks[position].id)
        if (displayName == "") {
            displayName = docks[position].name
        }
        val simpleBikes = docks[position].availableBikes - docks[position].availableEbikes
        val stationStr = StringBuilder()
            .append(displayName)
            .append("\n\uD83D\uDEB2: ")
            .append(String.format("%-8s", simpleBikes.toString()))
            .append("⚡: ")
            .append(String.format("%-8s", docks[position].availableEbikes.toString()))
            .append("\uD83D\uDCCD: ")
            .append(docks[position].availableDocks)
        holder.setText(stationStr.toString())
        holder.view.setOnClickListener {
            ShareStation.userLocation = docks[position].location
            MapHandler.centerMap(map, 17F)
            Utils.favoritesPopup?.dismiss()
        }
    }
}