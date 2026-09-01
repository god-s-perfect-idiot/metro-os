package com.metro.system

import android.content.Context
import android.content.Intent

/**
 * Shared spec for the Metro lock screen overlay (`com.metro.lockscreen`).
 *
 * The lock surface is drawn as a [android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY]
 * above the system keyguard and foreground activities. Critical full-screen alerts (incoming call,
 * alarm, etc.) must temporarily suppress it so their UI is visible while the keyguard stays locked.
 *
 * Requests are delivered as broadcasts targeted at [PACKAGE], mirroring [MetroStatusBar].
 */
object MetroLockscreen {
    /** Package that owns and renders the lock screen overlay. */
    const val PACKAGE = "com.metro.lockscreen"

    /** Hide or restore the Metro lock fill for a critical overlay (incoming call, alarm, …). */
    const val ACTION_SET_SUPPRESSED = "com.metro.lockscreen.action.SET_SUPPRESSED"

    /** Boolean extra for [ACTION_SET_SUPPRESSED] — `true` tears down the overlay until cleared. */
    const val EXTRA_SUPPRESSED = "suppressed"

    /** Ask the lock host to hide (`true`) or resume normal presentation (`false`). */
    fun requestSuppress(context: Context, suppressed: Boolean) {
        val appContext = context.applicationContext
        appContext.sendBroadcast(
            request(ACTION_SET_SUPPRESSED).putExtra(EXTRA_SUPPRESSED, suppressed),
        )
    }

    private fun request(action: String): Intent =
        Intent(action).setPackage(PACKAGE)
}
