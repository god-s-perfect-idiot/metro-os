package com.metro.system

/**
 * Live tile payload exported by pin-capable Metro apps via [MetroTileContract].
 */
data class MetroTileData(
    val title: String,
    val backgroundColorHex: String,
    val counter: Int? = null,
    val iconUri: String? = null,
    val imageUri: String? = null,
    val backFaceTitle: String? = null,
    val backFaceImageUri: String? = null,
    val deepLinkUri: String? = null,
    val photoGrid: MetroTilePhotoGrid? = null,
    val agenda: MetroTileAgenda? = null,
    /**
     * Custom Start widget face (clock / battery / glyph / peek cycle). When set, the launcher
     * renders the face and does not launch the app on tap unless no [tapAction] is needed
     * for display-only.
     */
    val widgetFace: MetroTileWidgetFace? = null,
    /**
     * Explicit broadcast action for Start taps (targeted at the tile's package).
     * Prefer a single per-app action that switches on [MetroTileContract] tile id.
     */
    val tapAction: String? = null,
    /**
     * Peek faces for [MetroTileWidgetFaceKind.PEEK_CYCLE] — launcher cycles these with no
     * host-app icon front (Notifier catalog tile behavior).
     */
    val peeks: List<MetroTilePeek>? = null,
) {
    val hasFlipFace: Boolean
        get() = !backFaceTitle.isNullOrBlank() || !backFaceImageUri.isNullOrBlank()

    val hasPhotoGrid: Boolean
        get() = photoGrid?.hasContent == true

    val hasAgenda: Boolean
        get() = agenda?.hasContent == true

    val hasWidgetFace: Boolean
        get() = widgetFace?.hasContent == true

    val hasPeekCycle: Boolean
        get() = widgetFace?.kind == MetroTileWidgetFaceKind.PEEK_CYCLE

    /** Start should not open the app activity when the tile owns its tap or face. */
    val handlesStartTap: Boolean
        get() = !tapAction.isNullOrBlank() || hasWidgetFace
}
