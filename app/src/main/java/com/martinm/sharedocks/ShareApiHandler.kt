package com.martinm.sharedocks

import com.google.android.gms.maps.model.LatLng
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
import java.lang.Thread.sleep
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.Instant

object ShareApiHandler {
    var docks = mutableMapOf<String, ShareStation>()
    var sortableDocks = mutableListOf<ShareStation>()

    private lateinit var mStationInfoUrl: URL
    private lateinit var mStationStatusUrl: URL
    private lateinit var mLastCalled: Instant

    /*
     * Note: all API information is available at: https://github.com/NABSA/gbfs
     */

    private fun loadStationUrls() {
        with(CityUtils.map[CityUtils.currentCity]?.baseUrl!!.openConnection() as HttpURLConnection) {
            requestMethod = "GET"
            inputStream.bufferedReader().use { reader ->
                val obj = JSONObject(reader.readText()).getJSONObject("data")
                val nextKey = obj.keys().next()
                val arr = if (nextKey != "feeds") {
                    obj.getJSONObject(nextKey).getJSONArray("feeds")
                } else {
                    obj.getJSONArray("feeds")
                }
                for (i in 0 until arr.length()) {
                    val feed = arr.getJSONObject(i).getString("name")
                    val url = arr.getJSONObject(i).getString("url").replace("http:", "https:")
                    when (feed) {
                        "station_information" -> mStationInfoUrl = URL(url)
                        "station_status" -> mStationStatusUrl = URL(url)
                    }
                }
            }
        }
        sleep(100)
    }

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
        return getDocksJson(mStationInfoUrl)
    }

    private fun getDocksStatusJson(): JSONArray {
        return getDocksJson(mStationStatusUrl)
    }

    fun updateDockStatus() {
        val stations = getDocksStatusJson()
        for (i in 0 until stations.length()) {
            val obj = stations.getJSONObject(i)
            val id = obj.getString("station_id")
            if (docks[id] != null) {
                docks[id]!!.availableBikes = obj.getInt("num_bikes_available")
                docks[id]!!.availableDocks = obj.getInt("num_docks_available")
                docks[id]!!.lastUpdate = Instant.ofEpochMilli(obj.getLong("last_reported"))
                // The "is_renting" property is not needed for tracking
                try {
                    docks[id]!!.isActive =
                        obj.getInt("is_installed") == 1 && obj.getInt("is_returning") == 1
                } catch (e: Exception) {
                    docks[id]!!.isActive =
                        obj.getBoolean("is_installed") && obj.getBoolean("is_returning")
                }

                docks[id]!!.updateHue()
            }
        }
    }

    fun loadDockLocations() {
        sortableDocks.clear()
        docks.clear()
        loadStationUrls()
        val stations = getDocksInfoJson()
        for (i in 0 until stations.length()) {
            val obj = stations.getJSONObject(i)
            val id = obj.getString("station_id")
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