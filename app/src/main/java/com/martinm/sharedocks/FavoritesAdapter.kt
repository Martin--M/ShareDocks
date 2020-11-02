package com.martinm.sharedocks

import android.view.LayoutInflater
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

        return FavoritesViewHolder(view)
    }

    override fun getItemCount(): Int {
        return docks.size
    }

    override fun onBindViewHolder(holder: FavoritesViewHolder, position: Int) {
        val stationStr = StringBuilder()
            .append(docks[position].name)
            .append("\n\uD83D\uDEB2: ")
            .append(String.format("%-13s", docks[position].availableBikes.toString()))
            .append("\uD83D\uDCCD: ")
            .append(docks[position].availableDocks)
        holder.setText(stationStr.toString())
        holder.view.setOnClickListener {
            ShareStation.userLocation = docks[position].location
            Utils.centerMap(map, 17F)
            Utils.favoritesPopup?.dismiss()
        }
    }
}