package com.martinm.bixidocks

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FavoritesAdapter(private val docks: MutableList<BixiStation>) : RecyclerView.Adapter<FavoritesAdapter.FavoritesViewHolder>() {

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
    }

}