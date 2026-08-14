package com.metro.ui

import android.app.Activity

/**
 * Activity-level helpers for Metro page transitions.
 *
 * Suppresses the platform open/close animation so Compose pivot motion
 * ([MetroPagePivotLoad], [MetroAppPivotShell]) owns the visible transition.
 */
object MetroActivities {
    fun applyLaunchTransition(activity: Activity) {
        activity.overridePendingTransition(0, 0)
    }

    fun applyExitTransition(activity: Activity) {
        activity.overridePendingTransition(0, 0)
    }
}
