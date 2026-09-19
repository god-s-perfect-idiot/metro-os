package com.metro.notifications

import android.content.Context
import android.provider.Settings
import android.util.Log

/**
 * Disables AOSP SystemUI heads-up while Metro toasts are responsible for peek UI.
 *
 * Requires `WRITE_SECURE_SETTINGS` (adb grant). Failure is non-fatal — toasts still render,
 * but the stock banner may flash on OEM skins that ignore the global setting.
 *
 * Critical interrupts (calls / alarms / full-screen intents) temporarily restore stock
 * heads-up so WhatsApp calls and similar peeks are not swallowed by the global suppress.
 */
object HeadsUpController {
    private const val TAG = "HeadsUpController"

    /** AOSP Settings.Global key; the SDK constant is @hide. */
    const val HEADS_UP_NOTIFICATIONS_ENABLED = "heads_up_notifications_enabled"

    private val lock = Any()
    private val criticalKeys = mutableSetOf<String>()

    fun disableStockHeadsUp(context: Context) {
        synchronized(lock) {
            if (criticalKeys.isNotEmpty()) {
                // A call/alarm peek is live — keep stock heads-up until it ends.
                return
            }
            disableStockHeadsUpLocked(context)
        }
    }

    /**
     * Restores the pre-Metro heads-up setting. No-ops while the master toggle is still on so an
     * overlay FGS restart does not re-enable stock peeks (critical peeks hold stock HU open
     * via [beginCriticalStockPeek] until they end).
     */
    fun restoreStockHeadsUp(context: Context) {
        synchronized(lock) {
            if (NotificationsPreferences(context).enabled) return
            restoreStockHeadsUpLocked(context)
        }
    }

    /**
     * Allow SystemUI to show the stock heads-up / call UI for this notification key.
     * Also clears listener effect-suppression hints via [ActionNotificationListenerService].
     */
    fun beginCriticalStockPeek(context: Context, key: String) {
        synchronized(lock) {
            val first = criticalKeys.isEmpty()
            criticalKeys += key
            if (!first) return
            val prefs = NotificationsPreferences(context)
            val cr = context.contentResolver
            val current = readHeadsUpEnabled(cr)
            if (current != 0) {
                prefs.previousHeadsUpEnabled = current
            }
            val restoreTo = prefs.previousHeadsUpEnabled.coerceAtLeast(1)
            val ok = runCatching {
                Settings.Global.putInt(cr, HEADS_UP_NOTIFICATIONS_ENABLED, restoreTo)
            }.getOrDefault(false)
            if (!ok) {
                Log.w(TAG, "Could not re-enable heads_up_notifications_enabled for critical peek")
            }
            ActionNotificationListenerService.clearHeadsUpSuppression()
        }
    }

    /** Drop a critical key; re-suppress stock heads-up when none remain and Metro is on. */
    fun endCriticalStockPeek(context: Context, key: String) {
        synchronized(lock) {
            if (!criticalKeys.remove(key)) return
            if (criticalKeys.isNotEmpty()) return
            if (!NotificationsPreferences(context).enabled) {
                restoreStockHeadsUpLocked(context)
                return
            }
            disableStockHeadsUpLocked(context)
            ActionNotificationListenerService.requestHeadsUpSuppression()
        }
    }

    fun isCriticalStockPeekActive(): Boolean = synchronized(lock) { criticalKeys.isNotEmpty() }

    fun isStockHeadsUpDisabled(context: Context): Boolean =
        readHeadsUpEnabled(context.contentResolver) == 0

    private fun disableStockHeadsUpLocked(context: Context) {
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

    private fun restoreStockHeadsUpLocked(context: Context) {
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

    private fun readHeadsUpEnabled(cr: android.content.ContentResolver): Int =
        runCatching {
            Settings.Global.getInt(cr, HEADS_UP_NOTIFICATIONS_ENABLED, 1)
        }.getOrDefault(1)
}
