package com.martinm.bixidocks

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.GoogleMap

class FavoritesAdapter(private val docks: MutableList<BixiStation>, private val map: GoogleMap) :
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
        holder.textView.text = docks[position].name
        holder.textView.setOnClickListener {
            BixiStation.userLocation = docks[position].location
            Utils.centerMap(map, 17F)
            Utils.favoritesPopup?.dismiss()
        }
    }
}