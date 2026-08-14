package com.metro.ui

import android.app.Activity

/**
 * Activity-level helpers for Metro page transitions.
 *
 * Suppresses the platform open/close animation so Compose pivot motion
 * ([MetroPagePivotLoad], [MetroAppPivotShell]) owns the visible transition.
 *
 * Call [applyLaunchTransition] from `onCreate` before `setContent`, and
 * [finishWithExitTransition] from [MetroAppPivotShell]'s `onExit` (after the
 * Compose flip-out completes).
 */
object MetroActivities {
    fun applyLaunchTransition(activity: Activity) {
        activity.overridePendingTransition(0, 0)
    }

    fun applyExitTransition(activity: Activity) {
        activity.overridePendingTransition(0, 0)
    }

    /** Finish the activity with no platform close animation. */
    fun finishWithExitTransition(activity: Activity) {
        activity.finish()
        applyExitTransition(activity)
    }
}
