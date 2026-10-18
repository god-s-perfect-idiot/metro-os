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
     * tile brand color; [StatusBarBackgroundMode.ShowAccentColor] always uses the system accent;
     * [StatusBarBackgroundMode.Transparent] paints no tray fill (glyphs float over content).
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

    /**
     * Per-glyph visibility from the setup **icons** tab. Defaults all on so new installs
     * match the full preview row until the user turns individual icons off.
     */
    var iconFlags: TrayIconFlags
        get() = TrayIconFlags(
            network = prefs.getBoolean(KEY_ICON_NETWORK, true),
            wifi = prefs.getBoolean(KEY_ICON_WIFI, true),
            mute = prefs.getBoolean(KEY_ICON_MUTE, true),
            notifications = prefs.getBoolean(KEY_ICON_NOTIFICATIONS, true),
            hotspot = prefs.getBoolean(KEY_ICON_HOTSPOT, true),
            bluetoothAudio = prefs.getBoolean(KEY_ICON_BLUETOOTH_AUDIO, true),
            battery = prefs.getBoolean(KEY_ICON_BATTERY, true),
        )
        set(value) = prefs.edit()
            .putBoolean(KEY_ICON_NETWORK, value.network)
            .putBoolean(KEY_ICON_WIFI, value.wifi)
            .putBoolean(KEY_ICON_MUTE, value.mute)
            .putBoolean(KEY_ICON_NOTIFICATIONS, value.notifications)
            .putBoolean(KEY_ICON_HOTSPOT, value.hotspot)
            .putBoolean(KEY_ICON_BLUETOOTH_AUDIO, value.bluetoothAudio)
            .putBoolean(KEY_ICON_BATTERY, value.battery)
            .apply()

    /**
     * Ordered tray slots from the **configure** page (icons + optional spacers).
     * Defaults to WP L→R icon order with no spacers.
     */
    var layout: List<TrayLayoutSlot>
        get() = TrayLayout.parse(prefs.getString(KEY_LAYOUT, null))
        set(value) = prefs.edit().putString(KEY_LAYOUT, TrayLayout.serialize(value)).apply()

    /**
     * When true, SystemUI status-bar icons are cleared via Shizuku (`cmd statusbar
     * send-disable-flag`) so only the Metro tray shows. Requires Shizuku running + permission.
     * Turning this off (or stopping the Metro tray) must restore SystemUI icons.
     */
    var hideSystemStatusBar: Boolean
        get() = prefs.getBoolean(KEY_HIDE_SYSTEM_STATUS_BAR, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_SYSTEM_STATUS_BAR, value).apply()

    companion object {
        private const val PREFS_NAME = "metro_statusbar"
        private const val KEY_ENABLED = "status_tray_enabled"
        private const val KEY_ICON_HIDE_TIMEOUT_MS = "icon_hide_timeout_ms"
        private const val KEY_NOTCH_POSITION = "notch_position"
        private const val KEY_BACKGROUND_MODE = "statusbar_background_mode"
        /** Legacy boolean key — read for migration, removed on write. */
        private const val KEY_MATCH_APP_BACKGROUND = "match_app_background"
        private const val KEY_ICON_NETWORK = "icon_network"
        private const val KEY_ICON_WIFI = "icon_wifi"
        private const val KEY_ICON_MUTE = "icon_mute"
        private const val KEY_ICON_NOTIFICATIONS = "icon_notifications"
        private const val KEY_ICON_HOTSPOT = "icon_hotspot"
        private const val KEY_ICON_BLUETOOTH_AUDIO = "icon_bluetooth_audio"
        private const val KEY_ICON_BATTERY = "icon_battery"
        private const val KEY_LAYOUT = "tray_layout"
        private const val KEY_HIDE_SYSTEM_STATUS_BAR = "hide_system_status_bar"

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
            StatusBarBackgroundMode.Transparent,
        )

        fun coerceIconHideTimeoutMs(ms: Long): Long =
            if (ms in ICON_HIDE_TIMEOUT_OPTIONS_MS) ms else DEFAULT_ICON_HIDE_TIMEOUT_MS
    }
}
