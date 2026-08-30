package com.metro.lockscreen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockscreenTrayTelemetryTest {
    @Test
    fun battery_fromLevel_setsFractionAndCharging() {
        val status = LockscreenBatteryStatus.fromLevel(80, 100, charging = true)
        assertEquals(0.8f, status.fraction, 0.01f)
        assertTrue(status.charging)
        assertFalse(status.isLow)
    }

    @Test
    fun battery_lowThresholdAtTwentyPercent() {
        val low = LockscreenBatteryStatus.fromLevel(20, 100, charging = false)
        assertTrue(low.isLow)
        val ok = LockscreenBatteryStatus.fromLevel(21, 100, charging = false)
        assertFalse(ok.isLow)
    }

    @Test
    fun signal_clampsBarCounts() {
        val status = LockscreenSignalStatus.fromLevels(cellularLevel = 9, wifiBands = 5)
        assertEquals(LockscreenSignalStatus.CELLULAR_BAR_COUNT, status.cellularBars)
        assertEquals(LockscreenSignalStatus.WIFI_BAND_COUNT, status.wifiBands)
    }
}
