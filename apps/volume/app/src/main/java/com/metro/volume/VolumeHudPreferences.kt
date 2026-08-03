package com.metro.volume

import android.content.Context

/**
 * Local setup preference for the Metro volume HUD master switch. Owned by this app (not
 * [com.metro.system.MetroPreferences]).
 */
class VolumeHudPreferences(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Whether the user wants the volume HUD overlay running. Defaults to off until first toggle. */
    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    companion object {
        private const val PREFS_NAME = "metro_volume"
        private const val KEY_ENABLED = "volume_hud_enabled"
    }
}
