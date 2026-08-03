package com.metro.volume

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeHudLogicTest {
    @Test
    fun androidToWp_mapsProportionally() {
        assertEquals(0, VolumeHudLogic.androidToWp(0, 7, 10))
        assertEquals(10, VolumeHudLogic.androidToWp(7, 7, 10))
        assertEquals(6, VolumeHudLogic.androidToWp(4, 7, 10))
        assertEquals(16, VolumeHudLogic.androidToWp(8, 15, 30))
        assertEquals(30, VolumeHudLogic.androidToWp(15, 15, 30))
    }

    @Test
    fun androidToWp_handlesZeroMax() {
        assertEquals(0, VolumeHudLogic.androidToWp(3, 0, 10))
        assertEquals(0, VolumeHudLogic.androidToWp(3, 7, 0))
    }

    @Test
    fun wpToAndroid_mapsProportionally() {
        assertEquals(0, VolumeHudLogic.wpToAndroid(0, 7, 10))
        assertEquals(7, VolumeHudLogic.wpToAndroid(10, 7, 10))
        assertEquals(15, VolumeHudLogic.wpToAndroid(30, 15, 30))
    }

    @Test
    fun androidToWpConsistent_keepsPreferredWhenMappedEqual() {
        // androidMax=5: WP 8 and 7 both map to android 4; keep the HUD tick.
        assertEquals(8, VolumeHudLogic.androidToWpConsistent(4, 5, 10, preferredWp = 8))
        assertEquals(7, VolumeHudLogic.androidToWpConsistent(4, 5, 10, preferredWp = 7))
        // External change away from preferred → remap.
        assertEquals(10, VolumeHudLogic.androidToWpConsistent(5, 5, 10, preferredWp = 8))
        assertEquals(0, VolumeHudLogic.androidToWpConsistent(0, 5, 10, preferredWp = 8))
    }

    @Test
    fun stepWpAcrossAndroid_coarseRingScaleDoesNotFloorOrJump() {
        // Regression: max=5 used to floor at 8/10 and jump to 10 on a single up.
        var level = 10
        for (expected in 9 downTo 0) {
            level = VolumeHudLogic.stepWpAcrossAndroid(level, -1, androidMax = 5, wpMax = 10)
            assertEquals(expected, level)
        }
        for (expected in 1..10) {
            level = VolumeHudLogic.stepWpAcrossAndroid(level, 1, androidMax = 5, wpMax = 10)
            assertEquals(expected, level)
        }
    }

    @Test
    fun stepWpAcrossAndroid_typicalRingMaxSeven() {
        var level = 10
        for (expected in 9 downTo 0) {
            level = VolumeHudLogic.stepWpAcrossAndroid(level, -1, androidMax = 7, wpMax = 10)
            assertEquals(expected, level)
        }
    }

    @Test
    fun selectDefaultStream_priority() {
        assertEquals(
            VolumeStreamKind.Call,
            VolumeHudLogic.selectDefaultStream(inCall = true, musicActive = true),
        )
        assertEquals(
            VolumeStreamKind.Media,
            VolumeHudLogic.selectDefaultStream(inCall = false, musicActive = true),
        )
        assertEquals(
            VolumeStreamKind.Ringer,
            VolumeHudLogic.selectDefaultStream(inCall = false, musicActive = false),
        )
    }

    @Test
    fun formatLevel_zeroPads() {
        assertEquals("00", VolumeHudLogic.formatLevelDigits(0))
        assertEquals("07", VolumeHudLogic.formatLevelDigits(7))
        assertEquals("/10", VolumeHudLogic.formatMaxSuffix(10))
        assertEquals("/30", VolumeHudLogic.formatMaxSuffix(30))
    }

    @Test
    fun stepLevel_clamps() {
        assertEquals(0, VolumeHudLogic.stepLevel(0, -1, 10))
        assertEquals(10, VolumeHudLogic.stepLevel(10, 1, 10))
        assertEquals(6, VolumeHudLogic.stepLevel(5, 1, 10))
    }

    @Test
    fun toggleMute_restoresPrevious() {
        assertEquals(0 to 7, VolumeHudLogic.toggleMute(7, 7))
        assertEquals(7 to 7, VolumeHudLogic.toggleMute(0, 7))
        assertEquals(1 to 1, VolumeHudLogic.toggleMute(0, 0))
    }

    @Test
    fun shouldDismiss_afterTimeout() {
        assertFalse(
            VolumeHudLogic.shouldDismiss(
                visible = true,
                lastInteractionMs = 0L,
                nowMs = VolumeHudSpec.DISMISS_MS - 1,
            ),
        )
        assertTrue(
            VolumeHudLogic.shouldDismiss(
                visible = true,
                lastInteractionMs = 0L,
                nowMs = VolumeHudSpec.DISMISS_MS,
            ),
        )
        assertFalse(
            VolumeHudLogic.shouldDismiss(
                visible = false,
                lastInteractionMs = 0L,
                nowMs = VolumeHudSpec.DISMISS_MS,
            ),
        )
    }

    @Test
    fun streamLabelsAndMax_matchWp81() {
        assertEquals(10, VolumeStreamKind.Ringer.wpMax)
        assertEquals(30, VolumeStreamKind.Media.wpMax)
        assertEquals(10, VolumeStreamKind.Call.wpMax)
        assertEquals("Ringer + Notifications", VolumeStreamKind.Ringer.label)
        assertEquals("Media + Apps", VolumeStreamKind.Media.label)
        assertEquals("Call volume", VolumeStreamKind.Call.label)
    }

    @Test
    fun dismissMs_isTwoPointFiveSeconds() {
        assertEquals(2500L, VolumeHudSpec.DISMISS_MS)
    }

    @Test
    fun showHideAndExpandTiming_areSnappyEaseOutFamily() {
        assertEquals(200, VolumeHudSpec.SHOW_HIDE_MS)
        assertEquals(200, VolumeHudSpec.EXPAND_COLLAPSE_MS)
    }

    @Test
    fun keyRepeatTiming_matchesHoldToStepSpec() {
        assertEquals(400L, VolumeHudSpec.KEY_REPEAT_INITIAL_MS)
        assertEquals(100L, VolumeHudSpec.KEY_REPEAT_INTERVAL_MS)
    }
}
