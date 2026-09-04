package com.metro.launcher.data

/** Default Start grid — 2 medium tiles across (WP8.1 phone default). */
const val TILE_GRID_COLUMN_COUNT = 4

/** Expanded Start grid when Settings → show more columns is on — 3 medium tiles across. */
const val TILE_GRID_COLUMN_COUNT_EXPANDED = 6

fun tileGridColumnCount(showMoreColumns: Boolean): Int =
    if (showMoreColumns) TILE_GRID_COLUMN_COUNT_EXPANDED else TILE_GRID_COLUMN_COUNT

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
            val (col, row) = findFirstOpenAlignedSlot(
                occupied,
                entry.size.colSpan,
                entry.size.rowSpan,
                columns,
                startRow = 0,
            ) ?: findFirstOpenSlot(occupied, entry.size.colSpan, entry.size.rowSpan, columns)
            markTileCells(occupied, col, row, entry.size.colSpan, entry.size.rowSpan)
            entry.copy(gridCol = col, gridRow = row)
        }
    }
}

/**
 * Keeps tiles whose footprints still fit [columns]; first-fit packs only the rest
 * (and any unpositioned tiles) so shrinking from 6→4 never leaves overflow cells.
 * Still-valid coordinates are preserved (horizontal gaps survive).
 */
fun adaptTilesToColumnCount(
    entries: List<PinnedTileEntry>,
    columns: Int,
): List<PinnedTileEntry> {
    val readingOrder = entries.sortedWith(
        compareBy(
            { it.gridRow ?: Int.MAX_VALUE },
            { it.gridCol ?: Int.MAX_VALUE },
            { it.packageName },
            { it.tileId },
        ),
    )
    val occupied = mutableSetOf<Pair<Int, Int>>()
    val kept = mutableListOf<PinnedTileEntry>()
    val displaced = mutableListOf<PinnedTileEntry>()

    for (entry in readingOrder) {
        if (!entry.hasGridPosition()) {
            displaced += entry
            continue
        }
        val col = alignTileColumn(entry.gridCol!!, entry.size.colSpan, columns)
        val row = entry.gridRow!!
        if (canPlaceAt(occupied, col, row, entry.size.colSpan, entry.size.rowSpan, columns)) {
            markTileCells(occupied, col, row, entry.size.colSpan, entry.size.rowSpan)
            kept += if (col == entry.gridCol) entry else entry.copy(gridCol = col)
        } else {
            displaced += entry
        }
    }

    val relocated = displaced.map { entry ->
        val (col, row) = findFirstOpenAlignedSlot(
            occupied,
            entry.size.colSpan,
            entry.size.rowSpan,
            columns,
            startRow = 0,
        ) ?: findFirstOpenSlot(occupied, entry.size.colSpan, entry.size.rowSpan, columns)
        markTileCells(occupied, col, row, entry.size.colSpan, entry.size.rowSpan)
        entry.copy(gridCol = col, gridRow = row)
    }

    return compactEmptyRows(kept + relocated)
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
 * Applies a size change for one tile: clamps/aligns its column so the footprint stays
 * inside the grid, then displaces only tiles that overlap the new footprint.
 */
fun applyTileResize(
    entries: List<PinnedTileEntry>,
    packageName: String,
    tileId: String,
    newSize: PinnedTileSize,
    columns: Int = TILE_GRID_COLUMN_COUNT,
): List<PinnedTileEntry> {
    val positioned = ensureGridPositions(entries, columns)
    val target = positioned.firstOrNull { it.packageName == packageName && it.tileId == tileId }
        ?: return entries
    val newCol = alignTileColumn(target.gridCol!!, newSize.colSpan, columns)
    val newRow = target.gridRow!!
    val resizedKey = TilePlacementKey(packageName, tileId)

    val baseline = positioned.associate { entry ->
        val key = TilePlacementKey(entry.packageName, entry.tileId)
        key to GridPlacement(
            key = key,
            col = entry.gridCol!!,
            row = entry.gridRow!!,
            colSpan = if (key == resizedKey) newSize.colSpan else entry.size.colSpan,
            rowSpan = if (key == resizedKey) newSize.rowSpan else entry.size.rowSpan,
        )
    }
    // Seat resized tile at aligned col/row, displace overlaps (same engine as drag).
    val placed = placeTileAt(
        baseline = baseline,
        draggedKey = resizedKey,
        slotCol = newCol,
        slotRow = newRow,
        colSpan = newSize.colSpan,
        rowSpan = newSize.rowSpan,
        columns = columns,
    )
    val compacted = compactPlacementRows(placed)

    return positioned.map { entry ->
        val key = TilePlacementKey(entry.packageName, entry.tileId)
        val p = compacted[key] ?: return@map entry
        entry.copy(
            size = if (key == resizedKey) newSize else entry.size,
            gridCol = p.col,
            gridRow = p.row,
        )
    }.let { compactEmptyRows(it) }
}
