package com.metro.statusbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrayCollapseSchedulerTest {
    @Test
    fun autoCollapse_afterEnterPlusHold() {
        val icons = 5
        val enterMs = TraySpec.staggerSequenceMs(icons)
        assertTrue(
            TrayCollapseScheduler.shouldAutoCollapse(
                expanded = true,
                lastExpandedAtMs = 0L,
                nowMs = enterMs + TraySpec.AUTO_COLLAPSE_MS,
                animatingIconCount = icons,
            ),
        )
    }

    @Test
    fun noAutoCollapse_beforeHoldCompletes() {
        val icons = 5
        val enterMs = TraySpec.staggerSequenceMs(icons)
        assertFalse(
            TrayCollapseScheduler.shouldAutoCollapse(
                expanded = true,
                lastExpandedAtMs = 0L,
                nowMs = enterMs + TraySpec.AUTO_COLLAPSE_MS - 1,
                animatingIconCount = icons,
            ),
        )
    }

    @Test
    fun holdDuration_defaultsToFiveSeconds() {
        assertEquals(5000L, TraySpec.AUTO_COLLAPSE_MS)
    }

    @Test
    fun autoCollapse_honorsCustomHold() {
        val icons = 5
        val enterMs = TraySpec.staggerSequenceMs(icons)
        val holdMs = StatusTrayPreferences.TIMEOUT_3S_MS
        assertFalse(
            TrayCollapseScheduler.shouldAutoCollapse(
                expanded = true,
                lastExpandedAtMs = 0L,
                nowMs = enterMs + holdMs - 1,
                animatingIconCount = icons,
                holdMs = holdMs,
            ),
        )
        assertTrue(
            TrayCollapseScheduler.shouldAutoCollapse(
                expanded = true,
                lastExpandedAtMs = 0L,
                nowMs = enterMs + holdMs,
                animatingIconCount = icons,
                holdMs = holdMs,
            ),
        )
    }

    @Test
    fun staggerSequence_scalesWithIconCount() {
        assertEquals(0L, TraySpec.staggerSequenceMs(0))
        assertEquals(TraySpec.EXPAND_ANIMATION_MS, TraySpec.staggerSequenceMs(1))
        assertEquals(
            TraySpec.EXPAND_ANIMATION_MS + TraySpec.ICON_STAGGER_MS,
            TraySpec.staggerSequenceMs(2),
        )
    }

    @Test
    fun expandedIndicatorOrder_isNetworkWifiOnly() {
        assertEquals(
            listOf(
                TrayIndicator.Cellular,
                TrayIndicator.DataConnection,
                TrayIndicator.Wifi,
            ),
            TrayIndicatorOrder.expanded,
        )
    }

    @Test
    fun collapsedTray_showsClockOnly_noLeftIndicators() {
        assertEquals(emptyList<TrayIndicator>(), TrayIndicatorOrder.collapsed)
    }

    @Test
    fun battery_isNotInTheLeftIndicatorRow() {
        // Battery is drawn on the right next to the clock, not in either left row.
        assertFalse(TrayIndicatorOrder.expanded.contains(TrayIndicator.Battery))
        assertFalse(TrayIndicatorOrder.collapsed.contains(TrayIndicator.Battery))
    }
}
