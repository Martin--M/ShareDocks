package com.martinm.sharedocks

import com.google.android.gms.maps.model.LatLng
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.Instant

object ShareApiHandler {
    var docks = mutableMapOf<Int, ShareStation>()
    var sortableDocks = mutableListOf<ShareStation>()

    private const val STATION_INFO_JSON_PATH = "station_information.json"
    private const val STATION_STATUS_JSON_PATH = "station_status.json"

    private lateinit var mLastCalled: Instant

    /*
     * Note: all API information is available at: https://github.com/NABSA/gbfs
     */

    private fun getDocksJson(url: URL): JSONArray {
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"
            inputStream.bufferedReader().use {
                return JSONObject(it.readText()).getJSONObject("data").getJSONArray("stations")
            }
        }
    }

    private fun getDocksInfoJson(): JSONArray {
        // Avoid calling the API too often
        if (this::mLastCalled.isInitialized && Duration.between(
                mLastCalled,
                Instant.now()
            ).seconds < 5
        ) {
            return JSONArray()
        } else {
            mLastCalled = Instant.now()
        }
        return getDocksJson(URL(CityUtils.map[1]?.baseUrl!! + STATION_INFO_JSON_PATH))
    }

    private fun getDocksStatusJson(): JSONArray {
        return getDocksJson(URL(CityUtils.map[1]?.baseUrl!! + STATION_STATUS_JSON_PATH))
    }

    fun updateDockStatus() {
        val stations = getDocksStatusJson()
        for (i in 0 until stations.length()) {
            val obj = stations.getJSONObject(i)
            val id = obj.getInt("station_id")
            if (docks[id] != null) {
                docks[id]!!.availableBikes = obj.getInt("num_bikes_available")
                docks[id]!!.availableDocks = obj.getInt("num_docks_available")
                docks[id]!!.lastUpdate = Instant.ofEpochMilli(obj.getLong("last_reported"))
                // The "is_renting" property is not needed for tracking
                docks[id]!!.isActive =
                    obj.getInt("is_installed") == 1 && obj.getInt("is_returning") == 1
                docks[id]!!.updateHue()
            }
        }
    }

    fun loadDockLocations() {
        val stations = getDocksInfoJson()
        for (i in 0 until stations.length()) {
            val obj = stations.getJSONObject(i)
            val id = obj.getInt("station_id")
            if (docks[id] == null) {
                docks[id] = ShareStation(id)
            }

            docks[id]!!.location = LatLng(obj.getDouble("lat"), obj.getDouble("lon"))
            docks[id]!!.name = obj.getString("name")

            if (docks[id]!!.location.longitude == 0.0 && docks[id]!!.location.latitude == 0.0) {
                docks.remove(id)
            } else {
                sortableDocks.add(docks[id]!!)
            }
        }
    }
}