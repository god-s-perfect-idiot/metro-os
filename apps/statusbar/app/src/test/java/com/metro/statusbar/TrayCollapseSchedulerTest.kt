package com.metro.statusbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrayCollapseSchedulerTest {
    @Test
    fun autoCollapse_afterEightSeconds() {
        assertTrue(
            TrayCollapseScheduler.shouldAutoCollapse(
                expanded = true,
                lastExpandedAtMs = 0L,
                nowMs = TraySpec.AUTO_COLLAPSE_MS,
            ),
        )
    }

    @Test
    fun noAutoCollapse_beforeTimeout() {
        assertFalse(
            TrayCollapseScheduler.shouldAutoCollapse(
                expanded = true,
                lastExpandedAtMs = 0L,
                nowMs = TraySpec.AUTO_COLLAPSE_MS - 1,
            ),
        )
    }

    @Test
    fun indicatorOrder_matchesWp81Spec() {
        assertEquals(
            listOf(
                TrayIndicator.Cellular,
                TrayIndicator.Wifi,
                TrayIndicator.Bluetooth,
                TrayIndicator.Alarm,
                TrayIndicator.Location,
                TrayIndicator.Battery,
            ),
            TrayIndicatorOrder.default,
        )
    }
}
