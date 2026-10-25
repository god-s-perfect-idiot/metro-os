package com.metro.system

object MetroPreferenceKeys {
    const val PREFS_NAME = "metro_system"
    const val THEME_MODE = "theme_mode"
    const val ACCENT_COLOR = "accent_color"
    const val FONT_SCALE = "font_scale"
    /** Suite chrome typeface — see [MetroTypeface]. */
    const val FONT_FAMILY = "font_family"
    const val NAV_BAR_COLOR = "nav_bar_color"
    const val NAV_BAR_ENABLED = "nav_bar_enabled"
    /** When true, Start uses a 6-column grid (3 medium tiles across) instead of 4 (2 across). */
    const val SHOW_MORE_COLUMNS = "show_more_columns"

    /**
     * When true (and [SHOW_MORE_COLUMNS] is false), Start uses a compact layout: tiles leave a
     * sticky right column for the app-list arrow (no trailing arrow under the grid).
     */
    const val USE_COMPACT_LAYOUT = "use_compact_layout"

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
     * Comma-separated MediaStore [android.provider.MediaStore.Images.ImageColumns.BUCKET_ID]
     * values whose photos may appear on gallery live tiles.
     * Null or blank → none; otherwise only the listed buckets
     * (Settings → connected apps → Gallery apps → Albums).
     */
    const val CONNECTED_GALLERY_ALBUMS = "connected_gallery_albums"

    /**
     * Comma-separated packages whose pinned Start tiles use Xbox Music–style now-playing faces.
     * Null (never written) → [MetroConnectedApps.DEFAULT_MUSIC_PACKAGES]; blank → none.
     */
    const val CONNECTED_MUSIC_APPS = "connected_music_apps"

    /**
     * Default now-playing Start face for connected music apps
     * ([MetroMusicLiveTileStyle] storage value). Null → [MetroMusicLiveTileStyle.DEFAULT].
     * Per-tile override lives on the launcher pin (tile customize → widgets).
     */
    const val MUSIC_LIVE_TILE_STYLE = "music_live_tile_style"

    /**
     * Comma-separated packages Conversations may surface from the notification shade.
     * Null (never written) → [MetroConnectedApps.DEFAULT_CONVERSATION_PACKAGES]; blank → none.
     */
    const val CONNECTED_CONVERSATION_APPS = "connected_conversation_apps"

    /**
     * Active Android icon-pack package for Start / app list glyphs.
     * Null or blank → system / suite icons ([MetroIconPacks]).
     */
    const val ICON_PACK_PACKAGE = "icon_pack_package"
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
