package com.martinm.sharedocks

import android.os.Build
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])

class ShareApiHandlerTest {

    @Test
    fun validateApi() {
        var totalErrorCount = 0
        CityUtils.map.forEach { mapEntry ->
            var warnCount = 0
            var isError = false
            var details = ""
            val timeMs = measureTimeMillis {
                try {
                    ShareApiHandler.lastCalled = Instant.MIN
                    CityUtils.currentCity = mapEntry.key
                    ShareApiHandler.loadStationUrls()
                    ShareApiHandler.loadDockLocations()
                    ShareApiHandler.updateDockStatus()
                    assertNotEquals(0, ShareApiHandler.docks.size)
                    assertNotEquals(0, ShareApiHandler.sortableDocks.size)
                    ShareApiHandler.sortableDocks.forEach { station ->
                        try {
                            assertNotNull(station.lastUpdate)
                        } catch (e: Exception) {
                            warnCount++
                            details += "   + ${station.id}: ${station.name}\n"
                        }
                    }
                } catch (e: Exception) {
                    isError = true
                    totalErrorCount++
                    val sw = StringWriter()
                    e.printStackTrace(PrintWriter(sw))
                    details = sw.toString()
                }
            }
            when (true) {
                isError -> print("[FAIL] ")
                warnCount > 0 -> print("[WARN] ")
                else -> print("[SUCCESS] ")
            }
            println("${mapEntry.value.name} ($timeMs ms)")
            if (isError) {
                println(" - Details: \n$details")
            }
            if (warnCount > 0) {
                println(" - $warnCount stations not initialized\n$details")
            }
        }
        assertEquals(0, totalErrorCount)
    }
}