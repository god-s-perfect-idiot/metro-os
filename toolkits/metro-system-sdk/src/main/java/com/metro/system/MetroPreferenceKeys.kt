package com.metro.system

object MetroPreferenceKeys {
    const val PREFS_NAME = "metro_system"
    const val THEME_MODE = "theme_mode"
    const val ACCENT_COLOR = "accent_color"
    const val FONT_SCALE = "font_scale"
    const val NAV_BAR_COLOR = "nav_bar_color"
    const val NAV_BAR_ENABLED = "nav_bar_enabled"
    /** When true, Start uses a 6-column grid (3 medium tiles across) instead of 4 (2 across). */
    const val SHOW_MORE_COLUMNS = "show_more_columns"

    /**
     * When true, Start shows a user photo behind accent/transparent tiles (WP8.1 Start background).
     * Image bytes live at [MetroStartBackground.CONTENT_URI]; this flag is the cross-app signal.
     */
    const val START_BACKGROUND_ENABLED = "start_background_enabled"

    /**
     * Comma-separated packages whose pinned Start tiles use Photos-style gallery live faces.
     * Null (never written) → [MetroConnectedApps.DEFAULT_GALLERY_PACKAGES]; blank → none.
     */
    const val CONNECTED_GALLERY_APPS = "connected_gallery_apps"

    /**
     * Comma-separated packages whose pinned Start tiles use Xbox Music–style now-playing faces.
     * Null (never written) → [MetroConnectedApps.DEFAULT_MUSIC_PACKAGES]; blank → none.
     */
    const val CONNECTED_MUSIC_APPS = "connected_music_apps"
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
