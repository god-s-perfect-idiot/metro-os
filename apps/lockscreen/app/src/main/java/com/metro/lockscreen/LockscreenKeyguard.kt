package com.metro.lockscreen

import android.app.KeyguardManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.PowerManager
import android.view.Display

/**
 * Keyguard + display-awake helpers for the Metro lock surface.
 *
 * The fill may render over an awake lock screen. It must not present when the display is
 * off or in Always-On Display / doze (same gate as volume HUD).
 *
 * Do not use [Context.getDisplay] — Service / AccessibilityService contexts throw on that API.
 */
object LockscreenKeyguard {
    fun isLocked(context: Context): Boolean {
        val keyguard = context.getSystemService(KeyguardManager::class.java) ?: return false
        return keyguard.isKeyguardLocked
    }

    /**
     * True only when the default display is fully awake ([Display.STATE_ON]) and the device
     * is interactive. False for screen-off, AOD, and doze.
     */
    fun isDisplayAwake(context: Context): Boolean {
        return try {
            val power = context.getSystemService(PowerManager::class.java)
            val interactive = power?.isInteractive ?: false
            LockscreenLogic.isDisplayAwake(defaultDisplayState(context), interactive)
        } catch (_: Throwable) {
            false
        }
    }

    /** True when the default display is in AOD / doze (not off, not fully awake). */
    fun isDisplayAod(context: Context): Boolean {
        return try {
            LockscreenLogic.isDisplayAod(defaultDisplayState(context))
        } catch (_: Throwable) {
            false
        }
    }

    /** True when Android battery saver (low power mode) is active. */
    fun isBatterySaverOn(context: Context): Boolean {
        return try {
            context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
        } catch (_: Throwable) {
            false
        }
    }

    fun defaultDisplayState(context: Context): Int {
        val display = defaultDisplay(context) ?: return Display.STATE_OFF
        return display.state
    }

    private fun defaultDisplay(context: Context): Display? {
        val manager = context.getSystemService(DisplayManager::class.java) ?: return null
        return manager.getDisplay(Display.DEFAULT_DISPLAY)
    }
}
