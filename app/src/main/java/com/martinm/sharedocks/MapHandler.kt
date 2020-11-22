package com.martinm.sharedocks

import android.content.Context
import android.graphics.*
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.concurrent.CountDownLatch

object MapHandler {
    private val mApi = ShareApiHandler
    var isMapLoading = false
    var requireVisualsUpdate = false

    fun waitForMapLoad() {
        while (isMapLoading) {
            Thread.sleep(10)
        }
    }

    fun centerMap(map: GoogleMap, zoom: Float) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(ShareStation.userLocation, zoom))
    }

    fun setupMap(context: AppCompatActivity, map: GoogleMap, markers: MutableList<Marker>) {
        var latch = CountDownLatch(1)
        isMapLoading = true

        context.runOnUiThread {
            markers.forEach {
                it.remove()
            }
            latch.countDown()
        }
        latch.await()
        markers.clear()
        latch = CountDownLatch(1)

        Utils.safeLoadDockLocations(context)
        Utils.safeUpdateDockStatus(context)
        Utils.loadUserDocks()
        mApi.sortableDocks.sort()

        context.runOnUiThread {
            val paint = Paint()
            val path = getMarkerPath()
            context.findViewById<ImageButton>(R.id.button_favorites).visibility = View.VISIBLE
            mApi.sortableDocks.forEach { station ->
                val pctFull =
                    station.availableDocks.toFloat() / (station.availableBikes + station.availableDocks)
                val marker = map.addMarker(
                    MarkerOptions().position(station.location).icon(
                        getMarkerIcon(
                            context,
                            paint,
                            path,
                            pctFull,
                            station.isActive
                        )
                    )
                )

                marker.tag = station
                markers.add(marker)

                if (!ConfigurationHandler.getShowUnavailableStations() && !station.isActive) {
                    marker.isVisible = false
                }
            }
            latch.countDown()
        }
        latch.await()
        isMapLoading = false
    }

    private fun getMarkerPath(): Path {
        val path = Path()
        path.moveTo(36.8F, 4F)
        path.rCubicTo(-14.2F, 0F, -26F, 11.6F, -26F, 26F)
        path.rCubicTo(0F, 17.4F, 23.8F, 45.8F, 24.8F, 46.6F)
        path.rCubicTo(0.2F, 0.2F, 0.6F, 0.4F, 1F, 0.4F)
        path.rCubicTo(0.4F, 0F, 0.8F, -0.2F, 1F, -0.4F)
        path.rCubicTo(1F, -1F, 24.8F, -29.2F, 24.8F, -46.6F)
        path.rCubicTo(0.4F, -14.8F, -11.4F, -26.4F, -25.6F, -26F)
        return path
    }

    private fun getMarkerIcon(
        context: Context,
        paint: Paint,
        path: Path,
        pctFull: Float,
        isAvailable: Boolean
    ): BitmapDescriptor? {
        val bitmap = Bitmap.createBitmap(75, 75, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.clipPath(path)

        var canvasFillY = ((canvas.height - 10) * (1 - pctFull)).toInt()
        val fillColor = context.getColor(R.color.colorAccent)
        var borderColor = context.getColor(R.color.colorPrimary)
        var backgroundColor = Color.WHITE

        if (pctFull.isNaN() || !isAvailable) {
            backgroundColor = context.getColor(R.color.colorUnavailable)
            canvasFillY = canvas.height
            borderColor = context.getColor(R.color.colorUnavailable)
        } else if (pctFull == 0F) {
            borderColor = context.getColor(R.color.colorBorderNoDocks)
        } else if (pctFull == 1F) {
            borderColor = context.getColor(R.color.colorBorderAllDocks)
        }

        if (ConfigurationHandler.getColorOnMarkers() && isAvailable && !pctFull.isNaN()) {
            canvasFillY = canvas.height
            var g = (pctFull * 2 * 255).toInt()
            if (g > 255) g = 255
            var r = ((1 - pctFull) * 2 * 255).toInt()
            if (r > 255) r = 255
            backgroundColor = Color.rgb(r, g, 0)
        }

        // Minor offsets to account for the border
        val fill = Rect(
            0,
            canvasFillY + 6,
            canvas.width,
            canvas.height - 3
        )

        /* Background */
        paint.color = backgroundColor
        paint.style = Paint.Style.FILL
        val background = Rect(0, 0, canvas.width, canvas.height)
        canvas.drawRect(background, paint)

        /* Fill */
        paint.color = fillColor
        paint.style = Paint.Style.FILL
        canvas.drawRect(fill, paint)

        /* Border */
        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8F
        canvas.drawPath(path, paint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}