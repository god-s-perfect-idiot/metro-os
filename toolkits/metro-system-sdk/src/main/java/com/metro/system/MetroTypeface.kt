package com.metro.system

/**
 * Suite-wide chrome typeface selected in Settings → start+theme.
 * Bundled faces live in `metro-ui-android`; storage values are stable preference keys.
 */
enum class MetroTypeface(
    val storageValue: String,
    val displayName: String,
) {
    /** Noto Sans (Segoe WP stand-in) — default. */
    MetroNoto("metro_noto", "Metro Noto"),

    /** Adobe Source Sans 3. */
    SourceSans3("source_sans_3", "Source Sans 3"),

    /** Huerta Tipográfica Alegreya Sans. */
    AlegreyaSans("alegreya_sans", "Alegreya Sans"),
    ;

    companion object {
        val DEFAULT: MetroTypeface = MetroNoto

        fun fromStorage(value: String?): MetroTypeface =
            entries.firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}
