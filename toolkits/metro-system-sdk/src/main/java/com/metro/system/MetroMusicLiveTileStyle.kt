package com.metro.system

/**
 * Xbox Music–style now-playing Start face layout.
 * Settings → connected apps → Music apps sets the suite default; each pin may override
 * via tile customize → widgets.
 */
enum class MetroMusicLiveTileStyle(val storageValue: String) {
    /** Large uppercase title + artist panning in opposite directions on the accent fill. */
    Presentation("presentation"),

    /** Current layout: 2×2 bottom meta+transport; 4×2 art half + controls half. */
    CompactControls("compact"),

    /** Full-bleed album art with title + transport laid out across the whole tile. */
    SpaciousControls("spacious"),
    ;

    companion object {
        val DEFAULT: MetroMusicLiveTileStyle = CompactControls

        fun fromStorage(value: String?): MetroMusicLiveTileStyle =
            entries.firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}
