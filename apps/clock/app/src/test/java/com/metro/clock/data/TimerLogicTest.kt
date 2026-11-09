package com.metro.clock.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerLogicTest {
    @Test
    fun totalSeconds_andPartsRoundTrip() {
        assertEquals(3661, TimerLogic.totalSeconds(1, 1, 1))
        assertEquals(Triple(1, 1, 1), TimerLogic.partsFromTotalSeconds(3661))
        assertEquals(Triple(0, 0, 0), TimerLogic.partsFromTotalSeconds(-5))
    }

    @Test
    fun formatDuration_omitsHoursWhenZero() {
        assertEquals("1:05", TimerLogic.formatDuration(65))
        assertEquals("0:00", TimerLogic.formatDuration(0))
        assertEquals("1:01:05", TimerLogic.formatDuration(3665))
    }

    @Test
    fun formatStopwatch_centiseconds() {
        assertEquals("0:00.00", TimerLogic.formatStopwatch(0))
        assertEquals("0:01.50", TimerLogic.formatStopwatch(1500))
        assertEquals("1:00.00", TimerLogic.formatStopwatch(60_000))
    }
}
