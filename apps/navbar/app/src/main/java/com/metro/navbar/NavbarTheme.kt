package com.metro.navbar

import androidx.compose.ui.graphics.Color
import com.metro.system.MetroNavBar
import com.metro.system.MetroPreferences

object NavbarSpec {
    const val BAR_HEIGHT_DP = MetroNavBar.HEIGHT_DP
    const val SYSTEM_NAV_BAR_FALLBACK_HEIGHT_DP = MetroNavBar.HEIGHT_DP
    const val REVEAL_STRIP_HEIGHT_DP = MetroNavBar.REVEAL_STRIP_HEIGHT_DP
    const val SOFT_KEY_ICON_SIZE_DP = 40
    const val START_KEY_ICON_SIZE_DP = 26
    const val LAUNCHER_PACKAGE = "com.metro.launcher"
    const val GOOGLE_SEARCH_PACKAGE = "com.google.android.googlequicksearchbox"
    const val GEMINI_PACKAGE = "com.google.android.apps.bard"
    const val GEMINI_ENTRY_ACTIVITY =
        "com.google.android.apps.bard.shellapp.BardEntryPointActivity"
}

data class NavbarThemeSnapshot(
    val barColor: Color,
    val iconColor: Color,
    val darkTheme: Boolean,
)

object NavbarThemeResolver {
    private val barColor = Color.Black

    fun resolve(preferences: MetroPreferences): NavbarThemeSnapshot {
        return NavbarThemeSnapshot(
            barColor = barColor,
            iconColor = Color.White,
            darkTheme = preferences.isDark,
        )
    }
}
