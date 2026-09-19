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
     * One of [TIMEOUT_3S_MS], [TIMEOUT_5S_MS], [TIMEOUT_10S_MS], or [TIMEOUT_NEVER_MS]
     * (always keep indicators expanded). Defaults to the WP8.1 5s hold.
     */
    var iconHideTimeoutMs: Long
        get() = coerceIconHideTimeoutMs(
            prefs.getLong(KEY_ICON_HIDE_TIMEOUT_MS, DEFAULT_ICON_HIDE_TIMEOUT_MS),
        )
        set(value) = prefs.edit()
            .putLong(KEY_ICON_HIDE_TIMEOUT_MS, coerceIconHideTimeoutMs(value))
            .apply()

    /** True when indicators should stay expanded and never auto-collapse. */
    val neverHidesIcons: Boolean
        get() = iconHideTimeoutMs == TIMEOUT_NEVER_MS

    /**
     * Display cutout / punch-hole side. [NotchPosition.Center] keeps the default tray layout;
     * left/right add side clearance so icons clear a corner notch.
     */
    var notchPosition: NotchPosition
        get() = NotchPosition.fromStorage(prefs.getString(KEY_NOTCH_POSITION, null))
        set(value) = prefs.edit().putString(KEY_NOTCH_POSITION, value.toStorage()).apply()

    /**
     * Tray fill mode. [StatusBarBackgroundMode.DefaultBlackBackground] is a solid black band
     * (default); [StatusBarBackgroundMode.MatchAppBackground] uses the foreground app’s icon /
     * tile brand color; [StatusBarBackgroundMode.ShowAccentColor] always uses the system accent.
     * Migrates the legacy boolean match toggle when present.
     */
    var backgroundMode: StatusBarBackgroundMode
        get() {
            prefs.getString(KEY_BACKGROUND_MODE, null)?.let {
                return StatusBarBackgroundMode.fromStorage(it)
            }
            // Legacy: match_app_background boolean (true → match, false → default black).
            return if (prefs.contains(KEY_MATCH_APP_BACKGROUND) &&
                prefs.getBoolean(KEY_MATCH_APP_BACKGROUND, false)
            ) {
                StatusBarBackgroundMode.MatchAppBackground
            } else {
                StatusBarBackgroundMode.DefaultBlackBackground
            }
        }
        set(value) = prefs.edit()
            .putString(KEY_BACKGROUND_MODE, value.toStorage())
            .remove(KEY_MATCH_APP_BACKGROUND)
            .apply()

    companion object {
        private const val PREFS_NAME = "metro_statusbar"
        private const val KEY_ENABLED = "status_tray_enabled"
        private const val KEY_ICON_HIDE_TIMEOUT_MS = "icon_hide_timeout_ms"
        private const val KEY_NOTCH_POSITION = "notch_position"
        private const val KEY_BACKGROUND_MODE = "statusbar_background_mode"
        /** Legacy boolean key — read for migration, removed on write. */
        private const val KEY_MATCH_APP_BACKGROUND = "match_app_background"

        const val TIMEOUT_3S_MS = 3_000L
        const val TIMEOUT_5S_MS = MetroStatusBar.AUTO_COLLAPSE_MS
        const val TIMEOUT_10S_MS = 10_000L
        /** Sentinel: never auto-hide expanded indicators. */
        const val TIMEOUT_NEVER_MS = -1L
        const val DEFAULT_ICON_HIDE_TIMEOUT_MS = TIMEOUT_5S_MS

        val ICON_HIDE_TIMEOUT_OPTIONS_MS = listOf(
            TIMEOUT_3S_MS,
            TIMEOUT_5S_MS,
            TIMEOUT_10S_MS,
            TIMEOUT_NEVER_MS,
        )
        val NOTCH_POSITION_OPTIONS = listOf(
            NotchPosition.Center,
            NotchPosition.Left,
            NotchPosition.Right,
        )
        val BACKGROUND_MODE_OPTIONS = listOf(
            StatusBarBackgroundMode.DefaultBlackBackground,
            StatusBarBackgroundMode.MatchAppBackground,
            StatusBarBackgroundMode.ShowAccentColor,
        )

        fun coerceIconHideTimeoutMs(ms: Long): Long =
            if (ms in ICON_HIDE_TIMEOUT_OPTIONS_MS) ms else DEFAULT_ICON_HIDE_TIMEOUT_MS
    }
}
