package com.metro.navbar

import android.content.Context

/**
 * Local setup preferences for the Metro navigation bar. Owned by this app (not
 * [com.metro.system.MetroPreferences]) — other apps use [com.metro.system.MetroNavBar] for
 * per-app visibility while the overlay is active.
 */
class NavbarPreferences(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Bar fill mode. [NavbarBackgroundMode.DefaultBlackBackground] is a solid black band
     * (default); [NavbarBackgroundMode.MatchAppBackground] uses the foreground app’s icon /
     * tile brand color; [NavbarBackgroundMode.ShowAccentColor] always uses the system accent.
     */
    var backgroundMode: NavbarBackgroundMode
        get() = NavbarBackgroundMode.fromStorage(prefs.getString(KEY_BACKGROUND_MODE, null))
        set(value) = prefs.edit().putString(KEY_BACKGROUND_MODE, value.toStorage()).apply()

    companion object {
        private const val PREFS_NAME = "metro_navbar"
        private const val KEY_BACKGROUND_MODE = "navbar_background_mode"

        val BACKGROUND_MODE_OPTIONS = listOf(
            NavbarBackgroundMode.DefaultBlackBackground,
            NavbarBackgroundMode.MatchAppBackground,
            NavbarBackgroundMode.ShowAccentColor,
        )
    }
}

/** Setup ListPicker: how the navbar fill is chosen for the foreground app. */
enum class NavbarBackgroundMode {
    /** Solid black bar fill (default). */
    DefaultBlackBackground,
    /** Fill from the foreground app’s tile brand color (Metro suite stays black). */
    MatchAppBackground,
    /** Always use the system accent color. */
    ShowAccentColor,
    ;

    fun toStorage(): String = when (this) {
        DefaultBlackBackground -> STORAGE_DEFAULT_BLACK
        MatchAppBackground -> STORAGE_MATCH_APP
        ShowAccentColor -> STORAGE_SHOW_ACCENT
    }

    companion object {
        const val STORAGE_DEFAULT_BLACK = "default_black"
        const val STORAGE_MATCH_APP = "match_app"
        const val STORAGE_SHOW_ACCENT = "show_accent"

        fun fromStorage(value: String?): NavbarBackgroundMode = when (value) {
            STORAGE_MATCH_APP -> MatchAppBackground
            STORAGE_SHOW_ACCENT -> ShowAccentColor
            STORAGE_DEFAULT_BLACK -> DefaultBlackBackground
            else -> DefaultBlackBackground
        }
    }
}
