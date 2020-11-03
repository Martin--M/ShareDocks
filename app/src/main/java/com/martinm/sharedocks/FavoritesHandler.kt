package com.martinm.sharedocks

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.GoogleMap

object FavoritesHandler {
    private lateinit var mFavoritesAdapter: FavoritesAdapter

    private val mTouchHelper = ItemTouchHelper(object :
        ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            Utils.moveItems(
                LogicHandler.userDocks,
                viewHolder.layoutPosition,
                target.layoutPosition
            )
            mFavoritesAdapter.notifyItemMoved(viewHolder.layoutPosition, target.layoutPosition)
            return true
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            ConfigurationHandler.storeStationList(LogicHandler.userDocks)
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            if (LogicHandler.userDocks.size > viewHolder.layoutPosition) {
                LogicHandler.userDocks.removeAt(viewHolder.layoutPosition)
            }
            mFavoritesAdapter.notifyItemRemoved(viewHolder.layoutPosition)
        }

    })

    fun loadFavoritesPage(context: AppCompatActivity, map: GoogleMap) {
        context.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        context.title = context.getString(R.string.actionbar_title_favorites)
        val favoritesView =
            (context.getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(
                R.layout.tracked_stations_view,
                context.findViewById(R.id.map),
                false
            ) as RecyclerView
        mFavoritesAdapter = FavoritesAdapter(LogicHandler.userDocks, map)
        favoritesView.setHasFixedSize(true)
        favoritesView.layoutManager = GridLayoutManager(context, 1)
        favoritesView.adapter = mFavoritesAdapter
        favoritesView.addItemDecoration(
            DividerItemDecoration(
                favoritesView.context,
                DividerItemDecoration.VERTICAL
            )
        )

        Utils.favoritesPopup = PopupWindow(
            favoritesView,
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            context.findViewById<View>(R.id.map).height
        )
        Utils.favoritesPopup!!.showAtLocation(
            context.findViewById(R.id.map),
            Gravity.BOTTOM,
            0, 0
        )

        mTouchHelper.attachToRecyclerView(favoritesView)
    }

}