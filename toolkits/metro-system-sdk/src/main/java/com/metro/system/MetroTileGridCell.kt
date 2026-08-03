package com.metro.system

/**
 * One cell in a live-tile photo grid (People hub tile, etc.).
 *
 * [label] is an optional single-letter (or short) overlay used when [imageUri] has no
 * bitmap — e.g. People contacts without a photo still read as distinct mosaic faces.
 */
data class MetroTileGridCell(
    val colorHex: String? = null,
    val imageUri: String? = null,
    val label: String? = null,
)
