package com.metro.lockscreen

/**
 * Pure helpers for lock-surface gesture and keyguard presentation decisions.
 *
 * Swipe session phases (UI):
 * - [SwipePhase.Idle] — resting at offset 0
 * - [SwipePhase.Dragging] — finger down, tracking offset
 * - [SwipePhase.SettlingBack] — released below threshold; spring bounce home (translate only)
 * - [SwipePhase.Committing] — released at/above threshold; animate fully off-screen
 * - [SwipePhase.HandedOff] — off-screen; host owns SystemUI bouncer; do not restore
 */
object LockscreenLogic {
    /** Minimum upward travel (dp) to commit unlock after release. */
    const val SWIPE_UP_THRESHOLD_DP = 180f

    /** Fraction of screen height that also counts as commit (whichever is larger vs dp). */
    const val SWIPE_UP_THRESHOLD_FRACTION = 0.28f

    enum class SwipePhase {
        Idle,
        Dragging,
        SettlingBack,
        Committing,
        HandedOff,
    }

    /** Outcome of a finger-up while [SwipePhase.Dragging]. */
    enum class ReleaseAction {
        /** Offset below threshold — animate back with spring overshoot (pure translate). */
        SnapBack,

        /** Offset at/above threshold — animate fully off-screen, then hand off. */
        Commit,
    }

    /** Only allow dragging the lock surface upward (negative Y in Compose). */
    fun clampDragOffsetY(offsetY: Float): Float = offsetY.coerceAtMost(0f)

    /**
     * Visual Y for the lock fill during drag / snap-back.
     *
     * Upward offsets (≤ 0) pass through. Spring overshoot past rest is positive in the
     * animatable; mirror it to negative so the bounce lifts the fill off the **top**
     * (revealing beneath at the bottom) instead of sinking under the status bar.
     */
    fun bounceTranslationY(rawOffsetY: Float): Float =
        if (rawOffsetY > 0f) -rawOffsetY else rawOffsetY

    fun unlockThresholdPx(screenHeightPx: Float, density: Float): Float {
        val fromDp = SWIPE_UP_THRESHOLD_DP * density
        val fromFraction = screenHeightPx * SWIPE_UP_THRESHOLD_FRACTION
        return maxOf(fromDp, fromFraction)
    }

    fun isSwipeUp(totalDragY: Float, thresholdPx: Float): Boolean =
        totalDragY <= -thresholdPx

    fun decideRelease(offsetY: Float, thresholdPx: Float): ReleaseAction =
        if (isSwipeUp(offsetY, thresholdPx)) ReleaseAction.Commit else ReleaseAction.SnapBack

    /**
     * Whether the default display is fully awake (not off / AOD / doze).
     * [displayState] is a [android.view.Display] state; [interactive] mirrors
     * [android.os.PowerManager.isInteractive].
     */
    fun isDisplayAwake(displayState: Int, interactive: Boolean): Boolean {
        if (!interactive) return false
        return displayState == android.view.Display.STATE_ON
    }

    /**
     * Whether the Metro lock fill should be attached.
     * [handedOff] stays true after a committed swipe until the next screen-off / re-lock.
     * [displayAwake] must be true — never over AOD or screen-off.
     */
    fun shouldPresentLock(
        enabled: Boolean,
        keyguardLocked: Boolean,
        displayAwake: Boolean,
        handedOff: Boolean = false,
        criticalOverlaySuppressed: Boolean = false,
    ): Boolean =
        enabled &&
            keyguardLocked &&
            displayAwake &&
            !handedOff &&
            !criticalOverlaySuppressed

    /** Hide the Metro fill while the radio reports an active or ringing call. */
    fun shouldSuppressForPhoneState(state: String?): Boolean =
        state == PHONE_STATE_RINGING || state == PHONE_STATE_OFFHOOK

    /** [android.telephony.TelephonyManager.EXTRA_STATE] values used by [shouldSuppressForPhoneState]. */
    internal const val PHONE_STATE_RINGING = "RINGING"
    internal const val PHONE_STATE_OFFHOOK = "OFFHOOK"

    /**
     * Drag is allowed at rest, while already dragging, or while the snap-back spring
     * is running — a new finger must be able to interrupt the bounce without waiting
     * for Idle (otherwise the first re-swipe after release is silently dropped).
     */
    fun phaseAllowsDrag(phase: SwipePhase): Boolean =
        phase == SwipePhase.Idle ||
            phase == SwipePhase.Dragging ||
            phase == SwipePhase.SettlingBack
}
