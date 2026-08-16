package com.metro.notifications

import android.content.Context
import android.provider.Settings
import android.util.Log

/**
 * Disables AOSP SystemUI heads-up while Metro toasts are responsible for peek UI.
 *
 * Requires `WRITE_SECURE_SETTINGS` (adb grant). Failure is non-fatal — toasts still render,
 * but the stock banner may flash on OEM skins that ignore the global setting.
 */
object HeadsUpController {
    private const val TAG = "HeadsUpController"

    /** AOSP Settings.Global key; the SDK constant is @hide. */
    const val HEADS_UP_NOTIFICATIONS_ENABLED = "heads_up_notifications_enabled"

    fun disableStockHeadsUp(context: Context) {
        val prefs = NotificationsPreferences(context)
        val cr = context.contentResolver
        val current = readHeadsUpEnabled(cr)
        // Keep the pre-Metro value across re-entrant disables (FGS restart, listener connect).
        if (current != 0) {
            prefs.previousHeadsUpEnabled = current
        }
        val ok = runCatching {
            Settings.Global.putInt(cr, HEADS_UP_NOTIFICATIONS_ENABLED, 0)
        }.getOrDefault(false)
        if (!ok || readHeadsUpEnabled(cr) != 0) {
            Log.w(TAG, "Could not set heads_up_notifications_enabled=0 (need WRITE_SECURE_SETTINGS)")
        }
    }

    /**
     * Restores the pre-Metro heads-up setting. No-ops while the master toggle is still on so an
     * overlay FGS restart does not re-enable stock peeks.
     */
    fun restoreStockHeadsUp(context: Context) {
        if (NotificationsPreferences(context).enabled) return
        val prefs = NotificationsPreferences(context)
        val ok = runCatching {
            Settings.Global.putInt(
                context.contentResolver,
                HEADS_UP_NOTIFICATIONS_ENABLED,
                prefs.previousHeadsUpEnabled,
            )
        }.getOrDefault(false)
        if (!ok) {
            Log.w(TAG, "Could not restore heads_up_notifications_enabled")
        }
    }

    fun isStockHeadsUpDisabled(context: Context): Boolean =
        readHeadsUpEnabled(context.contentResolver) == 0

    private fun readHeadsUpEnabled(cr: android.content.ContentResolver): Int =
        runCatching {
            Settings.Global.getInt(cr, HEADS_UP_NOTIFICATIONS_ENABLED, 1)
        }.getOrDefault(1)
}
