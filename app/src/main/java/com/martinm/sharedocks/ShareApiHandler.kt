package com.martinm.sharedocks

import com.google.android.gms.maps.model.LatLng
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.Instant

object ShareApiHandler {
    var docks = mutableMapOf<String, ShareStation>()
    var sortableDocks = mutableListOf<ShareStation>()

    private lateinit var mStationInfoUrl: URL
    private lateinit var mStationStatusUrl: URL
    lateinit var lastCalled: Instant

    /*
     * Note: all API information is available at: https://github.com/NABSA/gbfs
     */

    fun loadStationUrls() {
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

                // Montreal-only
                try {
                    docks[id]!!.availableEbikes = obj.getInt("num_ebikes_available")
                } catch (e: Exception) {
                }

                docks[id]!!.updateHue()
            }
        }
    }

    fun loadDockLocations() {
        // Avoid calling the API too often
        if (this::lastCalled.isInitialized && Duration.between(
                lastCalled,
                Instant.now()
            ).seconds < 5
        ) {
            return
        } else {
            lastCalled = Instant.now()
        }
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

            var tmpStr = obj.getString("lat")
            val lat = if (tmpStr == "null") {
                0.0
            } else {
                tmpStr.toDouble()
            }
            tmpStr = obj.getString("lon")
            val lon = if (tmpStr == "null") {
                0.0
            } else {
                tmpStr.toDouble()
            }
            docks[id]!!.location = LatLng(lat, lon)
            docks[id]!!.name = obj.getString("name")
            docks[id]!!.updateHue()

            if (docks[id]!!.location.longitude == 0.0 && docks[id]!!.location.latitude == 0.0) {
                docks.remove(id)
            } else {
                sortableDocks.add(docks[id]!!)
            }
        }
    }
}