package com.martinm.bixidocks

import android.content.Context

object LogicHandler {
    var userDockIds = mutableListOf<Int>()
    private var mBixi = BixiApiHandler

    fun toggleUserDockId(id: Int) {
        if (!userDockIds.contains(id)) {
            userDockIds.add(id)
        } else {
            userDockIds.remove(id)
        }
    }

    fun getButtonStringForId(context: Context, id: Int): String {
        val res = context.resources
        return if (userDockIds.contains(id)) {
            res.getString(res.getIdentifier("popup_button_remove", "string", context.packageName))
        } else {
            res.getString(res.getIdentifier("popup_button_add", "string", context.packageName))
        }
    }
}