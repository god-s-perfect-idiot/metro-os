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
    fun expandedIndicatorOrder_matchesWp81Breakdown() {
        assertEquals(
            listOf(
                TrayIndicator.Cellular,
                TrayIndicator.DataConnection,
                TrayIndicator.CallForwarding,
                TrayIndicator.Roaming,
                TrayIndicator.Wifi,
                TrayIndicator.Bluetooth,
                TrayIndicator.QuietHours,
                TrayIndicator.DrivingMode,
                TrayIndicator.Ringer,
                TrayIndicator.Location,
            ),
            TrayIndicatorOrder.expanded,
        )
    }

    @Test
    fun collapsedTray_showsBaseConnectionIndicators() {
        assertEquals(
            listOf(TrayIndicator.Cellular, TrayIndicator.Wifi),
            TrayIndicatorOrder.collapsed,
        )
    }

    @Test
    fun battery_isNotInTheLeftIndicatorRow() {
        // Battery is drawn on the right next to the clock, not in either left row.
        assertFalse(TrayIndicatorOrder.expanded.contains(TrayIndicator.Battery))
        assertFalse(TrayIndicatorOrder.collapsed.contains(TrayIndicator.Battery))
    }
}
