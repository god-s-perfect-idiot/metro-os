package com.metro.system

/**
 * Photo-grid live tile payload. The launcher chooses column/row layout from the pinned
 * tile size (e.g. 3×3 on 2×2, 6×3 on 4×2) and renders the first [columns × rows] cells.
 */
data class MetroTilePhotoGrid(
    val cells: List<MetroTileGridCell>,
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
