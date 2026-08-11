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
        val current = runCatching {
            Settings.Global.getInt(cr, HEADS_UP_NOTIFICATIONS_ENABLED, 1)
        }.getOrDefault(1)
        prefs.previousHeadsUpEnabled = current
        val ok = runCatching {
            Settings.Global.putInt(cr, HEADS_UP_NOTIFICATIONS_ENABLED, 0)
        }.getOrDefault(false)
        if (!ok) {
            Log.w(TAG, "Could not set heads_up_notifications_enabled=0 (need WRITE_SECURE_SETTINGS)")
        }
    }

    fun restoreStockHeadsUp(context: Context) {
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
}
