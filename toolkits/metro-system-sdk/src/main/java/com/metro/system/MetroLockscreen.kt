package com.metro.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

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

    /** Accessibility service component that hosts the overlay and can lock the display. */
    const val ACCESSIBILITY_SERVICE_CLASS = "com.metro.lockscreen.LockscreenAccessibilityService"

    /** Hide or restore the Metro lock fill for a critical overlay (incoming call, alarm, …). */
    const val ACTION_SET_SUPPRESSED = "com.metro.lockscreen.action.SET_SUPPRESSED"

    /** Boolean extra for [ACTION_SET_SUPPRESSED] — `true` tears down the overlay until cleared. */
    const val EXTRA_SUPPRESSED = "suppressed"

    /**
     * Ask the lockscreen accessibility service to lock the device now
     * (`GLOBAL_ACTION_LOCK_SCREEN` on API 28+).
     */
    const val ACTION_LOCK_NOW = "com.metro.lockscreen.action.LOCK_NOW"

    /** Ask the lock host to hide (`true`) or resume normal presentation (`false`). */
    fun requestSuppress(context: Context, suppressed: Boolean) {
        val appContext = context.applicationContext
        appContext.sendBroadcast(
            request(ACTION_SET_SUPPRESSED).putExtra(EXTRA_SUPPRESSED, suppressed),
        )
    }

    /** Ask lockscreen to lock the display (requires its accessibility service enabled). */
    fun requestLock(context: Context) {
        context.applicationContext.sendBroadcast(request(ACTION_LOCK_NOW))
    }

    /** True when the Metro lockscreen accessibility service is listed as enabled. */
    fun isAccessibilityEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val expected = ComponentName(PACKAGE, ACCESSIBILITY_SERVICE_CLASS).flattenToString()
        val expectedShort = "$PACKAGE/$ACCESSIBILITY_SERVICE_CLASS"
        return enabled.split(':').any { entry ->
            entry.equals(expected, ignoreCase = true) ||
                entry.equals(expectedShort, ignoreCase = true)
        }
    }

    /** Opens system Accessibility settings so the user can enable the lockscreen service. */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.applicationContext.startActivity(intent)
    }

    private fun request(action: String): Intent =
        Intent(action).setPackage(PACKAGE)
}
