package com.metro.statusbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrayClockSchedulerTest {
    @Test
    fun millisUntilNextMinute_waitsForBoundary() {
        assertEquals(60_000L, TrayClockScheduler.millisUntilNextMinute(0L))
        assertEquals(1L, TrayClockScheduler.millisUntilNextMinute(59_999L))
        assertTrue(TrayClockScheduler.millisUntilNextMinute(1_000L) in 1L..60_000L)
    }

    @Test
    fun millisUntilNextMinute_midMinute() {
        assertEquals(30_000L, TrayClockScheduler.millisUntilNextMinute(30_000L))
        assertEquals(1_000L, TrayClockScheduler.millisUntilNextMinute(59_000L))
    }
}
