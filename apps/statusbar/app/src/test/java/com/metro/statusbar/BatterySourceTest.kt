package com.metro.statusbar

import android.content.Intent
import android.os.BatteryManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(org.robolectric.RobolectricTestRunner::class)
class BatterySourceTest {
    @Test
    fun parse_showsChargingWhenPluggedAndNotCharging() {
        val status = BatterySource.parse(
            batteryIntent(
                level = 92,
                scale = 100,
                status = BatteryManager.BATTERY_STATUS_NOT_CHARGING,
                plugged = BatteryManager.BATTERY_PLUGGED_AC,
            ),
        )
        assertTrue(status.charging)
        assertEquals(92, status.percent)
    }

    @Test
    fun parse_showsChargingWhenPluggedAndCharging() {
        val status = BatterySource.parse(
            batteryIntent(
                level = 45,
                scale = 100,
                status = BatteryManager.BATTERY_STATUS_CHARGING,
                plugged = BatteryManager.BATTERY_PLUGGED_USB,
            ),
        )
        assertTrue(status.charging)
    }

    @Test
    fun parse_hidesChargingWhenUnplugged() {
        val status = BatterySource.parse(
            batteryIntent(
                level = 100,
                scale = 100,
                status = BatteryManager.BATTERY_STATUS_FULL,
                plugged = 0,
            ),
        )
        assertFalse(status.charging)
    }

    @Test
    fun parse_hidesChargingWhenDischargingEvenIfPluggedExtraStale() {
        val status = BatterySource.parse(
            batteryIntent(
                level = 60,
                scale = 100,
                status = BatteryManager.BATTERY_STATUS_DISCHARGING,
                plugged = BatteryManager.BATTERY_PLUGGED_AC,
            ),
        )
        assertFalse(status.charging)
    }

    private fun batteryIntent(
        level: Int,
        scale: Int,
        status: Int,
        plugged: Int,
    ): Intent =
        Intent(Intent.ACTION_BATTERY_CHANGED).apply {
            putExtra(BatteryManager.EXTRA_LEVEL, level)
            putExtra(BatteryManager.EXTRA_SCALE, scale)
            putExtra(BatteryManager.EXTRA_STATUS, status)
            putExtra(BatteryManager.EXTRA_PLUGGED, plugged)
        }
}
