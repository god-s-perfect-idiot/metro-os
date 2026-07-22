package com.metro.launcher.data

const val TILE_GRID_COLUMN_COUNT = 4

/**
 * Assigns [gridCol]/[gridRow] to entries that lack them using first-fit packing.
 * Existing positions are preserved so intentional gaps survive.
 */
fun ensureGridPositions(
    entries: List<PinnedTileEntry>,
    columns: Int = TILE_GRID_COLUMN_COUNT,
): List<PinnedTileEntry> {
    val occupied = mutableSetOf<Pair<Int, Int>>()
    entries.filter { it.hasGridPosition() }.forEach { entry ->
        markTileCells(occupied, entry.gridCol!!, entry.gridRow!!, entry.size.colSpan, entry.size.rowSpan)
    }
    return entries.map { entry ->
        if (entry.hasGridPosition()) {
            entry
        } else {
            val (col, row) = findFirstOpenSlot(occupied, entry.size.colSpan, entry.size.rowSpan, columns)
            markTileCells(occupied, col, row, entry.size.colSpan, entry.size.rowSpan)
            entry.copy(gridCol = col, gridRow = row)
        }
    }
}

internal fun markTileCells(
    occupied: MutableSet<Pair<Int, Int>>,
    col: Int,
    row: Int,
    colSpan: Int,
    rowSpan: Int,
) {
    for (r in row until row + rowSpan) {
        for (c in col until col + colSpan) {
            occupied += c to r
        }
    }
}

internal fun findFirstOpenSlot(
    occupied: Set<Pair<Int, Int>>,
    colSpan: Int,
    rowSpan: Int,
    columns: Int,
    startRow: Int = 0,
): Pair<Int, Int> {
    var row = startRow
    while (true) {
        var col = 0
        while (col <= columns - colSpan) {
            if (canPlaceAt(occupied, col, row, colSpan, rowSpan, columns)) {
                return col to row
            }
            col++
        }
        row++
    }
}

internal fun canPlaceAt(
    occupied: Set<Pair<Int, Int>>,
    col: Int,
    row: Int,
    colSpan: Int,
    rowSpan: Int,
    columns: Int,
): Boolean {
    if (col + colSpan > columns) return false
    for (r in row until row + rowSpan) {
        for (c in col until col + colSpan) {
            if ((c to r) in occupied) return false
        }
    }
    return true
}

internal fun tileOverlapsRegion(
    col: Int,
    row: Int,
    colSpan: Int,
    rowSpan: Int,
    regionCol: Int,
    regionRow: Int,
    regionColSpan: Int,
    regionRowSpan: Int,
): Boolean {
    val colOverlap = col < regionCol + regionColSpan && col + colSpan > regionCol
    val rowOverlap = row < regionRow + regionRowSpan && row + rowSpan > regionRow
    return colOverlap && rowOverlap
}

/** Maps each occupied row index to a compact index with no fully empty rows between them. */
internal fun rowCompactionMap(rowSpans: Iterable<Pair<Int, Int>>): Map<Int, Int> {
    val occupiedRows = mutableSetOf<Int>()
    for ((row, span) in rowSpans) {
        for (r in row until row + span) {
            occupiedRows += r
        }
    }
    return occupiedRows.sorted().withIndex().associate { (index, oldRow) -> oldRow to index }
}

/**
 * Shifts tiles up to remove fully empty rows. Horizontal black gaps (empty columns) are kept.
 */
fun compactEmptyRows(entries: List<PinnedTileEntry>): List<PinnedTileEntry> {
    val positioned = entries.filter { it.hasGridPosition() }
    if (positioned.isEmpty()) return entries
    val rowMap = rowCompactionMap(positioned.map { it.gridRow!! to it.size.rowSpan })
    return entries.map { entry ->
        if (!entry.hasGridPosition()) {
            entry
        } else {
            entry.copy(gridRow = rowMap[entry.gridRow] ?: entry.gridRow)
        }
    }
}

/**
 * Applies a size change for one tile: clamps its column so the footprint stays inside the
 * grid, then pushes only tiles that overlap the new footprint (WP8.1 resize reflow).
 */
fun applyTileResize(
    entries: List<PinnedTileEntry>,
    packageName: String,
    tileId: String,
    newSize: PinnedTileSize,
    columns: Int = TILE_GRID_COLUMN_COUNT,
): List<PinnedTileEntry> {
    val positioned = ensureGridPositions(entries)
    val target = positioned.firstOrNull { it.packageName == packageName && it.tileId == tileId }
        ?: return entries
    val newCol = target.gridCol!!.coerceIn(0, columns - newSize.colSpan)
    val newRow = target.gridRow!!
    val resized = target.copy(size = newSize, gridCol = newCol, gridRow = newRow)

    val occupied = mutableSetOf<Pair<Int, Int>>()
    markTileCells(occupied, newCol, newRow, newSize.colSpan, newSize.rowSpan)

    val stable = mutableListOf<PinnedTileEntry>()
    val displaced = mutableListOf<PinnedTileEntry>()
    for (entry in positioned) {
        if (entry.packageName == packageName && entry.tileId == tileId) continue
        if (tileOverlapsRegion(
                entry.gridCol!!,
                entry.gridRow!!,
                entry.size.colSpan,
                entry.size.rowSpan,
                newCol,
                newRow,
                newSize.colSpan,
                newSize.rowSpan,
            )
        ) {
            displaced += entry
        } else {
            stable += entry
            markTileCells(
                occupied,
                entry.gridCol!!,
                entry.gridRow!!,
                entry.size.colSpan,
                entry.size.rowSpan,
            )
        }
    }

    val repositioned = displaced
        .sortedWith(compareBy({ it.gridRow!! }, { it.gridCol!! }))
        .map { entry ->
            val (col, row) = findFirstOpenSlot(
                occupied,
                entry.size.colSpan,
                entry.size.rowSpan,
                columns,
                startRow = newRow,
            )
            markTileCells(occupied, col, row, entry.size.colSpan, entry.size.rowSpan)
            entry.copy(gridCol = col, gridRow = row)
        }

    return compactEmptyRows(stable + repositioned + resized)
}
