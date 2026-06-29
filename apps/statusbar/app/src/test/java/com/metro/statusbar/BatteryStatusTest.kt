package com.metro.statusbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryStatusTest {
    @Test
    fun fromLevel_computesFraction() {
        val status = BatteryStatus.fromLevel(level = 37, scale = 100, charging = false)
        assertEquals(0.37f, status.fraction, 0.0001f)
        assertEquals(37, status.percent)
        assertFalse(status.charging)
    }

    @Test
    fun fromLevel_clampsOverflow() {
        val status = BatteryStatus.fromLevel(level = 150, scale = 100, charging = true)
        assertEquals(1f, status.fraction, 0.0001f)
        assertEquals(100, status.percent)
        assertTrue(status.charging)
    }

    @Test
    fun fromLevel_invalidScale_fallsBackToUnknown() {
        val status = BatteryStatus.fromLevel(level = 50, scale = 0, charging = true)
        assertEquals(BatteryStatus.Unknown.fraction, status.fraction, 0.0001f)
        assertTrue(status.charging)
    }

    @Test
    fun percent_clampsNegativeFraction() {
        assertEquals(0, BatteryStatus(fraction = -0.5f, charging = false).percent)
    }
}
