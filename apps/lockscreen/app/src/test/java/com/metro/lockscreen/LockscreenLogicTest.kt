package com.metro.lockscreen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockscreenLogicTest {
    @Test
    fun swipeUp_requiresEnoughTravel() {
        assertTrue(LockscreenLogic.isSwipeUp(-100f, 80f))
        assertFalse(LockscreenLogic.isSwipeUp(-40f, 80f))
        assertFalse(LockscreenLogic.isSwipeUp(100f, 80f))
    }

    @Test
    fun decideRelease_commitsOnlyPastThreshold() {
        assertEquals(
            LockscreenLogic.ReleaseAction.Commit,
            LockscreenLogic.decideRelease(-100f, 80f),
        )
        assertEquals(
            LockscreenLogic.ReleaseAction.SnapBack,
            LockscreenLogic.decideRelease(-40f, 80f),
        )
        assertEquals(
            LockscreenLogic.ReleaseAction.SnapBack,
            LockscreenLogic.decideRelease(0f, 80f),
        )
    }

    @Test
    fun decideRelease_commitsOnUpwardFlingWithPartialTravel() {
        val threshold = 100f
        val fling = 800f
        // Past 20% of threshold + fast upward velocity → commit.
        assertEquals(
            LockscreenLogic.ReleaseAction.Commit,
            LockscreenLogic.decideRelease(
                offsetY = -25f,
                thresholdPx = threshold,
                velocityY = -900f,
                flingVelocityPx = fling,
            ),
        )
        // Fast fling but almost no travel → snap back (avoid tap unlock).
        assertEquals(
            LockscreenLogic.ReleaseAction.SnapBack,
            LockscreenLogic.decideRelease(
                offsetY = -5f,
                thresholdPx = threshold,
                velocityY = -2000f,
                flingVelocityPx = fling,
            ),
        )
        // Enough travel but slow → snap back.
        assertEquals(
            LockscreenLogic.ReleaseAction.SnapBack,
            LockscreenLogic.decideRelease(
                offsetY = -40f,
                thresholdPx = threshold,
                velocityY = -100f,
                flingVelocityPx = fling,
            ),
        )
    }

    @Test
    fun clampDrag_onlyAllowsUpward() {
        assertEquals(0f, LockscreenLogic.clampDragOffsetY(40f), 0.01f)
        assertEquals(-50f, LockscreenLogic.clampDragOffsetY(-50f), 0.01f)
    }

    @Test
    fun bounceTranslation_mirrorsOvershootUpward() {
        assertEquals(0f, LockscreenLogic.bounceTranslationY(0f), 0.01f)
        assertEquals(-80f, LockscreenLogic.bounceTranslationY(-80f), 0.01f)
        // Positive spring overshoot → lift off the top, not sink under the tray.
        assertEquals(-40f, LockscreenLogic.bounceTranslationY(40f), 0.01f)
    }

    @Test
    fun unlockThreshold_usesLargerOfDpAndFraction() {
        val density = 2f
        // max(120dp * 2, 2000 * 0.22) = max(240, 440) = 440
        val tall = LockscreenLogic.unlockThresholdPx(screenHeightPx = 2000f, density = density)
        assertEquals(440f, tall, 0.01f)

        // max(120dp * 2, 800 * 0.22) = max(240, 176) = 240
        val short = LockscreenLogic.unlockThresholdPx(screenHeightPx = 800f, density = density)
        assertEquals(240f, short, 0.01f)
    }

    @Test
    fun flingVelocity_scalesWithDensity() {
        assertEquals(1800f, LockscreenLogic.flingVelocityPx(2f), 0.01f)
    }

    @Test
    fun shouldPresentLock_onlyWhenEnabledLockedAwakeAndNotHandedOff() {
        assertTrue(LockscreenLogic.shouldPresentLock(true, true, true, handedOff = false))
        assertFalse(LockscreenLogic.shouldPresentLock(true, true, true, handedOff = true))
        assertFalse(LockscreenLogic.shouldPresentLock(false, true, true))
        assertFalse(LockscreenLogic.shouldPresentLock(true, false, true))
        assertFalse(LockscreenLogic.shouldPresentLock(true, true, false))
    }

    @Test
    fun shouldPresentLock_suppressedForCriticalOverlay() {
        assertFalse(
            LockscreenLogic.shouldPresentLock(
                enabled = true,
                keyguardLocked = true,
                displayAwake = true,
                criticalOverlaySuppressed = true,
            ),
        )
        assertTrue(
            LockscreenLogic.shouldPresentLock(
                enabled = true,
                keyguardLocked = true,
                displayAwake = true,
                criticalOverlaySuppressed = false,
            ),
        )
    }

    @Test
    fun shouldSuppressForPhoneState_ringingAndOffHook() {
        assertTrue(LockscreenLogic.shouldSuppressForPhoneState(LockscreenLogic.PHONE_STATE_RINGING))
        assertTrue(LockscreenLogic.shouldSuppressForPhoneState(LockscreenLogic.PHONE_STATE_OFFHOOK))
        assertFalse(LockscreenLogic.shouldSuppressForPhoneState("IDLE"))
    }

    @Test
    fun isDisplayAwake_requiresStateOnAndInteractive() {
        assertTrue(
            LockscreenLogic.isDisplayAwake(
                displayState = android.view.Display.STATE_ON,
                interactive = true,
            ),
        )
        assertFalse(
            LockscreenLogic.isDisplayAwake(
                displayState = android.view.Display.STATE_OFF,
                interactive = false,
            ),
        )
        assertFalse(
            LockscreenLogic.isDisplayAwake(
                displayState = android.view.Display.STATE_DOZE,
                interactive = true,
            ),
        )
        assertFalse(
            LockscreenLogic.isDisplayAwake(
                displayState = android.view.Display.STATE_DOZE_SUSPEND,
                interactive = false,
            ),
        )
        assertFalse(
            LockscreenLogic.isDisplayAwake(
                displayState = android.view.Display.STATE_ON,
                interactive = false,
            ),
        )
    }

    @Test
    fun isDisplayAod_dozeStatesOnly() {
        assertTrue(LockscreenLogic.isDisplayAod(android.view.Display.STATE_DOZE))
        assertTrue(LockscreenLogic.isDisplayAod(android.view.Display.STATE_DOZE_SUSPEND))
        assertFalse(LockscreenLogic.isDisplayAod(android.view.Display.STATE_ON))
        assertFalse(LockscreenLogic.isDisplayAod(android.view.Display.STATE_OFF))
    }

    @Test
    fun shouldPresentGlance_onlyWhenEnabledLockedAndNotAwake() {
        assertTrue(
            LockscreenLogic.shouldPresentGlance(
                enabled = true,
                glanceEnabled = true,
                keyguardLocked = true,
                displayAwake = false,
            ),
        )
        assertFalse(
            LockscreenLogic.shouldPresentGlance(
                enabled = true,
                glanceEnabled = false,
                keyguardLocked = true,
                displayAwake = false,
            ),
        )
        assertFalse(
            LockscreenLogic.shouldPresentGlance(
                enabled = false,
                glanceEnabled = true,
                keyguardLocked = true,
                displayAwake = false,
            ),
        )
        assertFalse(
            LockscreenLogic.shouldPresentGlance(
                enabled = true,
                glanceEnabled = true,
                keyguardLocked = false,
                displayAwake = false,
            ),
        )
        assertFalse(
            LockscreenLogic.shouldPresentGlance(
                enabled = true,
                glanceEnabled = true,
                keyguardLocked = true,
                displayAwake = true,
            ),
        )
        assertFalse(
            LockscreenLogic.shouldPresentGlance(
                enabled = true,
                glanceEnabled = true,
                keyguardLocked = true,
                displayAwake = false,
                batterySaverOn = true,
            ),
        )
        assertFalse(
            LockscreenLogic.shouldPresentGlance(
                enabled = true,
                glanceEnabled = true,
                keyguardLocked = true,
                displayAwake = false,
                criticalOverlaySuppressed = true,
            ),
        )
    }

    @Test
    fun phaseAllowsDrag_idleDraggingOrSettlingBack() {
        assertTrue(LockscreenLogic.phaseAllowsDrag(LockscreenLogic.SwipePhase.Idle))
        assertTrue(LockscreenLogic.phaseAllowsDrag(LockscreenLogic.SwipePhase.Dragging))
        assertTrue(LockscreenLogic.phaseAllowsDrag(LockscreenLogic.SwipePhase.SettlingBack))
        assertFalse(LockscreenLogic.phaseAllowsDrag(LockscreenLogic.SwipePhase.Committing))
        assertFalse(LockscreenLogic.phaseAllowsDrag(LockscreenLogic.SwipePhase.HandedOff))
    }
}
