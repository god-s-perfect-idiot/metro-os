package com.metro.system

/**
 * Photo-grid live tile payload.
 *
 * When [cycle] is false the launcher renders a static mosaic: it chooses a column/row
 * layout from the pinned tile size (e.g. 3×3 on 2×2, 6×3 on 4×2) and draws the first
 * [columns × rows] cells (People hub style).
 *
 * When [cycle] is true the launcher renders a single photo that slowly pans up, then slides
 * up as the next image enters from below (WP8.1 Photos tile style), regardless of tile size.
 */
data class MetroTilePhotoGrid(
    val cells: List<MetroTileGridCell>,
    val cycle: Boolean = false,
) {
    val hasContent: Boolean
        get() = cells.isNotEmpty()

    fun cellsFor(columns: Int, rows: Int): List<MetroTileGridCell> {
        val count = columns * rows
        if (count <= 0) return emptyList()
        if (cells.size >= count) return cells.take(count)
        return cells + List(count - cells.size) { MetroTileGridCell() }
    }
}
