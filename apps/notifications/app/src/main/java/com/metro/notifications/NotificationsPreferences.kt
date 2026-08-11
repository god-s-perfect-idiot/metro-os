package com.metro.notifications

import android.content.Context

/**
 * Local setup preferences for the Metro notifications overlay. Owned by this app
 * (not [com.metro.system.MetroPreferences]).
 */
class NotificationsPreferences(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var previousHeadsUpEnabled: Int
        get() = prefs.getInt(KEY_PREV_HEADS_UP, 1)
        set(value) = prefs.edit().putInt(KEY_PREV_HEADS_UP, value).apply()

    /** Auto-dismiss duration for toast banners. One of 3s / 5s / 10s; defaults to 5s. */
    var toastDurationMs: Long
        get() = ToastSpec.coerceDurationMs(
            prefs.getLong(KEY_TOAST_DURATION_MS, ToastSpec.DURATION_MS),
        )
        set(value) = prefs.edit()
            .putLong(KEY_TOAST_DURATION_MS, ToastSpec.coerceDurationMs(value))
            .apply()

    companion object {
        private const val PREFS_NAME = "metro_notifications"
        private const val KEY_ENABLED = "notifications_enabled"
        private const val KEY_PREV_HEADS_UP = "previous_heads_up_enabled"
        private const val KEY_TOAST_DURATION_MS = "toast_duration_ms"
    }
}
