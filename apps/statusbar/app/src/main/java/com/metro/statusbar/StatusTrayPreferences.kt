package com.metro.statusbar

import android.content.Context
import com.metro.system.MetroStatusBar

/**
 * Local setup preferences for the Metro status tray. Owned by this app (not
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

    /**
     * How long expanded indicators stay fully visible after the staggered enter finishes.
     * One of [TIMEOUT_3S_MS], [TIMEOUT_5S_MS], [TIMEOUT_10S_MS]. Defaults to the WP8.1 5s hold.
     */
    var iconHideTimeoutMs: Long
        get() = coerceIconHideTimeoutMs(
            prefs.getLong(KEY_ICON_HIDE_TIMEOUT_MS, DEFAULT_ICON_HIDE_TIMEOUT_MS),
        )
        set(value) = prefs.edit()
            .putLong(KEY_ICON_HIDE_TIMEOUT_MS, coerceIconHideTimeoutMs(value))
            .apply()

    /**
     * Display cutout / punch-hole side. [NotchPosition.Center] keeps the default tray layout;
     * left/right add side clearance so icons clear a corner notch.
     */
    var notchPosition: NotchPosition
        get() = NotchPosition.fromStorage(prefs.getString(KEY_NOTCH_POSITION, null))
        set(value) = prefs.edit().putString(KEY_NOTCH_POSITION, value.toStorage()).apply()

    /**
     * When true, non-Metro apps use their published status-bar / primary theme color for the tray
     * (glyphs invert for contrast). Metro suite apps always keep the Metro page fill. When false,
     * the tray uses the system theme for everyone.
     */
    var matchAppBackground: Boolean
        get() = prefs.getBoolean(KEY_MATCH_APP_BACKGROUND, false)
        set(value) = prefs.edit().putBoolean(KEY_MATCH_APP_BACKGROUND, value).apply()

    companion object {
        private const val PREFS_NAME = "metro_statusbar"
        private const val KEY_ENABLED = "status_tray_enabled"
        private const val KEY_ICON_HIDE_TIMEOUT_MS = "icon_hide_timeout_ms"
        private const val KEY_NOTCH_POSITION = "notch_position"
        private const val KEY_MATCH_APP_BACKGROUND = "match_app_background"

        const val TIMEOUT_3S_MS = 3_000L
        const val TIMEOUT_5S_MS = MetroStatusBar.AUTO_COLLAPSE_MS
        const val TIMEOUT_10S_MS = 10_000L
        const val DEFAULT_ICON_HIDE_TIMEOUT_MS = TIMEOUT_5S_MS

        val ICON_HIDE_TIMEOUT_OPTIONS_MS = listOf(TIMEOUT_3S_MS, TIMEOUT_5S_MS, TIMEOUT_10S_MS)
        val NOTCH_POSITION_OPTIONS = listOf(
            NotchPosition.Center,
            NotchPosition.Left,
            NotchPosition.Right,
        )

        fun coerceIconHideTimeoutMs(ms: Long): Long =
            if (ms in ICON_HIDE_TIMEOUT_OPTIONS_MS) ms else DEFAULT_ICON_HIDE_TIMEOUT_MS
    }
}
