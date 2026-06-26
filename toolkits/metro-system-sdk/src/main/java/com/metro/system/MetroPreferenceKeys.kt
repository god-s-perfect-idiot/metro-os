package com.metro.system

object MetroPreferenceKeys {
    const val PREFS_NAME = "metro_system"
    const val THEME_MODE = "theme_mode"
    const val ACCENT_COLOR = "accent_color"
    const val FONT_SCALE = "font_scale"
    const val NAV_BAR_COLOR = "nav_bar_color"
    const val NAV_BAR_ENABLED = "nav_bar_enabled"
}

enum class MetroThemeMode(val storageValue: String) {
    Dark("dark"),
    Light("light"),
    ;

    companion object {
        fun fromStorage(value: String?): MetroThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: Dark
    }
}
