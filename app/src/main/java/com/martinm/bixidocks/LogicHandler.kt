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
}