package com.metro.navbar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.metro.system.MetroNavBar
import com.metro.system.MetroPreferences

object NavbarSpec {
    const val BAR_HEIGHT_DP = MetroNavBar.HEIGHT_DP
    const val SYSTEM_NAV_BAR_FALLBACK_HEIGHT_DP = MetroNavBar.HEIGHT_DP
    const val REVEAL_STRIP_HEIGHT_DP = MetroNavBar.REVEAL_STRIP_HEIGHT_DP
    const val SOFT_KEY_ICON_SIZE_DP = 40
    /** Enlarged vs soft keys — launcher glyph is adaptive-icon padded (0.55). */
    const val START_KEY_ICON_SIZE_DP = 72
    const val LAUNCHER_PACKAGE = "com.metro.launcher"
    const val GOOGLE_SEARCH_PACKAGE = "com.google.android.googlequicksearchbox"
    const val GEMINI_PACKAGE = "com.google.android.apps.bard"
    const val GEMINI_ENTRY_ACTIVITY =
        "com.google.android.apps.bard.shellapp.BardEntryPointActivity"

    /** Luminance above this → dark (black) glyphs for contrast on light fills. */
    const val LIGHT_BACKGROUND_LUMINANCE = 0.5f
}

data class NavbarThemeSnapshot(
    val barColor: Color,
    val iconColor: Color,
    /** Suite accent — used for MetroTheme chrome, not necessarily the bar fill. */
    val accentColor: Color,
    val darkTheme: Boolean,
)

enum class NavbarVisibilityMode {
    Opaque,
    Hidden,
    ;

    companion object {
        fun fromContract(mode: String?): NavbarVisibilityMode = when (mode) {
            MetroNavBar.MODE_HIDDEN -> Hidden
            else -> Opaque
        }
    }
}

object NavbarThemeResolver {
    fun resolve(
        preferences: MetroPreferences,
        backgroundMode: NavbarBackgroundMode = NavbarBackgroundMode.DefaultBlackBackground,
        appBackgroundColor: Color? = null,
    ): NavbarThemeSnapshot {
        // Background mode from the setup ListPicker is the source of truth. Do not let a stale
        // MetroPreferences.nav_bar_color override win — that key is unused by Settings UI and
        // previously forced a permanent accent fill regardless of the picker.
        val accentColor = preferences.accentColor
        val barColor = when (backgroundMode) {
            NavbarBackgroundMode.DefaultBlackBackground -> Color.Black
            NavbarBackgroundMode.ShowAccentColor -> accentColor
            NavbarBackgroundMode.MatchAppBackground -> appBackgroundColor ?: Color.Black
        }
        val iconColor = foregroundForBackground(barColor)
        return NavbarThemeSnapshot(
            barColor = barColor,
            iconColor = iconColor,
            accentColor = accentColor,
            darkTheme = preferences.isDark,
        )
    }

    fun foregroundForBackground(background: Color): Color =
        if (background.luminance() > NavbarSpec.LIGHT_BACKGROUND_LUMINANCE) {
            Color.Black
        } else {
            Color.White
        }
}
