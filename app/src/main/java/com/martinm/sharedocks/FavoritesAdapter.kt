package com.martinm.sharedocks

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.GoogleMap
import java.lang.StringBuilder

class FavoritesAdapter(private val docks: MutableList<ShareStation>, private val map: GoogleMap) :
    RecyclerView.Adapter<FavoritesAdapter.FavoritesViewHolder>() {

    class FavoritesViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoritesViewHolder {
        val textView = LayoutInflater.from(parent.context)
            .inflate(R.layout.tracked_stations_entry, parent, false) as TextView

        return FavoritesViewHolder(textView)
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
        holder.textView.text = stationStr.toString()
        holder.textView.setOnClickListener {
            ShareStation.userLocation = docks[position].location
            Utils.centerMap(map, 17F)
            Utils.favoritesPopup?.dismiss()
        }
    }
}