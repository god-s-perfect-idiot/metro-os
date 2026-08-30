package com.metro.volume

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.PowerManager
import android.view.Display

/**
 * Display-awake gating for the Metro volume HUD.
 *
 * The HUD may render over an awake lock screen. It must not present when the display is
 * off or in Always-On Display / doze (low-power) states.
 *
 * Do not use [Context.getDisplay] here — Service / AccessibilityService contexts throw
 * [UnsupportedOperationException] on that API.
 */
object VolumeDisplayGate {
    /**
     * Whether the charcoal volume panel may be shown.
     *
     * [displayState] is an [Display] state constant; [interactive] mirrors
     * [PowerManager.isInteractive].
     */
    fun shouldPresentHud(displayState: Int, interactive: Boolean): Boolean {
        if (!interactive) return false
        return displayState == Display.STATE_ON
    }

    fun shouldPresentHud(context: Context): Boolean {
        return try {
            val power = context.getSystemService(PowerManager::class.java)
            val interactive = power?.isInteractive ?: true
            shouldPresentHud(defaultDisplayState(context), interactive)
        } catch (_: Throwable) {
            // Never crash the a11y key path — prefer showing when state is unknown.
            true
        }
    }

    fun defaultDisplayState(context: Context): Int {
        val display = defaultDisplay(context) ?: return Display.STATE_ON
        return display.state
    }

    private fun defaultDisplay(context: Context): Display? {
        val manager = context.getSystemService(DisplayManager::class.java) ?: return null
        return manager.getDisplay(Display.DEFAULT_DISPLAY)
    }
}
