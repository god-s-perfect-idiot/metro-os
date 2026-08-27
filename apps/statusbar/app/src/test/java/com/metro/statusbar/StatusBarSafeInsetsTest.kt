package com.metro.statusbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusBarSafeInsetsTest {

    @Test
    fun topLeftCorner_atTopEdge_requiresFullRadius() {
        val radius = 80
        val inset = StatusBarSafeInsets.topRoundedCornerInsetPx(
            radius = radius,
            centerX = radius,
            centerY = radius,
            contentTopY = 0f,
            contentBottomY = 0f,
            windowLeft = 0,
            windowRight = 1080,
            isLeftCorner = true,
        )
        assertEquals(radius.toFloat(), inset, 0.01f)
    }

    @Test
    fun topRightCorner_atTopEdge_requiresFullRadius() {
        val radius = 80
        val width = 1080
        val inset = StatusBarSafeInsets.topRoundedCornerInsetPx(
            radius = radius,
            centerX = width - radius,
            centerY = radius,
            contentTopY = 0f,
            contentBottomY = 0f,
            windowLeft = 0,
            windowRight = width,
            isLeftCorner = false,
        )
        assertEquals(radius.toFloat(), inset, 0.01f)
    }

    @Test
    fun topLeftCorner_belowRadius_needsNoInset() {
        val radius = 80
        val inset = StatusBarSafeInsets.topRoundedCornerInsetPx(
            radius = radius,
            centerX = radius,
            centerY = radius,
            contentTopY = radius.toFloat(),
            contentBottomY = radius + 10f,
            windowLeft = 0,
            windowRight = 1080,
            isLeftCorner = true,
        )
        assertEquals(0f, inset, 0.01f)
    }

    @Test
    fun topLeftCorner_midBand_isBetweenZeroAndRadius() {
        val radius = 100
        // Status-bar-like band around y ≈ 20–80 inside a 100px corner.
        val inset = StatusBarSafeInsets.topRoundedCornerInsetPx(
            radius = radius,
            centerX = radius,
            centerY = radius,
            contentTopY = 20f,
            contentBottomY = 80f,
            windowLeft = 0,
            windowRight = 1080,
            isLeftCorner = true,
        )
        assertTrue("expected partial inset, got $inset", inset > 0f && inset < radius)
        // Worst sample is contentTopY=20 → dy=80 → halfChord=60 → safeLeft=40.
        assertEquals(40f, inset, 0.5f)
    }

    @Test
    fun zeroRadius_isZero() {
        val inset = StatusBarSafeInsets.topRoundedCornerInsetPx(
            radius = 0,
            centerX = 0,
            centerY = 0,
            contentTopY = 0f,
            contentBottomY = 32f,
            windowLeft = 0,
            windowRight = 1080,
            isLeftCorner = true,
        )
        assertEquals(0f, inset, 0f)
    }

    @Test
    fun pxToDpCeil_roundsUp() {
        assertEquals(11, StatusBarSafeInsets.pxToDpCeil(31.1f, 3f))
        assertEquals(10, StatusBarSafeInsets.pxToDpCeil(30f, 3f))
        assertEquals(0, StatusBarSafeInsets.pxToDpCeil(0f, 2.75f))
        assertEquals(12, StatusBarSafeInsets.pxToDpCeil(32.1f, 2.75f))
    }

    @Test
    fun privacyDots_onRight_nudgeClock() {
        assertTrue(
            StatusBarSafeInsets.privacyDotsNearClock(
                boundsLeft = 1000f,
                boundsRight = 1060f,
                boundsTop = 4f,
                boundsBottom = 28f,
                windowWidth = 1080f,
            ),
        )
    }

    @Test
    fun privacyDots_onLeft_doNotNudgeClock() {
        assertFalse(
            StatusBarSafeInsets.privacyDotsNearClock(
                boundsLeft = 20f,
                boundsRight = 80f,
                boundsTop = 4f,
                boundsBottom = 28f,
                windowWidth = 1080f,
            ),
        )
    }

    @Test
    fun privacyDots_emptyBounds_doNotNudgeClock() {
        assertFalse(
            StatusBarSafeInsets.privacyDotsNearClock(
                boundsLeft = 0f,
                boundsRight = 0f,
                boundsTop = 0f,
                boundsBottom = 0f,
                windowWidth = 1080f,
            ),
        )
    }
}
