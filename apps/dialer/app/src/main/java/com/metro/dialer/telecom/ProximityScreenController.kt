package com.metro.dialer.telecom

import android.content.Context
import android.os.PowerManager
import android.util.Log

/**
 * Turns the screen off while the device is held to the ear during an earpiece call.
 *
 * Uses [PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK]: near → screen off, far → screen on.
 * No-ops when the device lacks a proximity sensor or the wake-lock level.
 */
class ProximityScreenController(context: Context) {
    private val appContext = context.applicationContext
    private val powerManager =
        appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    @Suppress("DEPRECATION")
    fun setEnabled(enabled: Boolean) {
        if (enabled) acquire() else release()
    }

    @Suppress("DEPRECATION")
    private fun acquire() {
        val pm = powerManager ?: return
        if (!pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            return
        }
        val lock = wakeLock ?: pm.newWakeLock(
            PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
            WAKE_LOCK_TAG,
        ).also {
            it.setReferenceCounted(false)
            wakeLock = it
        }
        if (!lock.isHeld) {
            try {
                lock.acquire(MAX_HOLD_MS)
            } catch (e: RuntimeException) {
                Log.w(TAG, "Failed to acquire proximity wake lock", e)
            }
        }
    }

    fun release() {
        val lock = wakeLock ?: return
        if (lock.isHeld) {
            try {
                lock.release()
            } catch (e: RuntimeException) {
                Log.w(TAG, "Failed to release proximity wake lock", e)
            }
        }
    }

    companion object {
        private const val TAG = "ProximityScreen"
        private const val WAKE_LOCK_TAG = "metro-dialer:proximity"
        /** Safety cap; call UI releases earlier on pause / route change / end. */
        private const val MAX_HOLD_MS = 4 * 60 * 60 * 1000L
    }
}
