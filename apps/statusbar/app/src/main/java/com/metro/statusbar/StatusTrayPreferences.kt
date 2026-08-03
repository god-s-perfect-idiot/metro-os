package com.metro.statusbar

import android.content.Context

/**
 * Local setup preference for the Metro status tray master switch. Owned by this app (not
 * [com.metro.system.MetroPreferences]) — other apps use [com.metro.system.MetroStatusBar] for
 * per-app tray styling while the overlay is active.
 */
class StatusTrayPreferences(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Whether the user wants the status tray overlay running. Defaults to off until first toggle. */
    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    companion object {
        private const val PREFS_NAME = "metro_statusbar"
        private const val KEY_ENABLED = "status_tray_enabled"
    }
}
