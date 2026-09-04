package com.metro.launcher.data

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * WP8.1 Start placement: tiles live at explicit (col, row). Horizontal gaps are
 * allowed; only fully empty rows compact. Drag seats a tile at a snapped slot and
 * displaces overlaps — same-row gaps first, then one batched pack into a new band
 * immediately below the seated tile (so a medium/wide can break a small-tile group
 * without flinging tiles to distant slots or fragmenting row inserts).
 *
 * @see <a href="https://www.eightforums.com/threads/windows-phone-8-move-a-tile-on-start-screen.36423/">WP8 Move a Tile</a>
 */

data class TilePlacementKey(val packageName: String, val tileId: String)

data class GridPlacement(
    val key: TilePlacementKey,
    val col: Int,
    val row: Int,
    val colSpan: Int,
    val rowSpan: Int,
)

/** Clamps a top-left column so [colSpan] fits on a [columns]-wide grid.
 *
 * Mediums may start on any column (including odd): WP8.1 allows
 * `1×1 | 2×2 | 1×1` with the medium centered between stacked smalls.
 */
fun alignTileColumn(col: Int, colSpan: Int, columns: Int): Int {
    val maxCol = (columns - colSpan).coerceAtLeast(0)
    return if (colSpan >= columns) 0 else col.coerceIn(0, maxCol)
}

/**
 * Snap pointer (tile center in continuous grid units) to a valid top-left slot.
 */
fun snapAlignedSlot(
    pointerCol: Float,
    pointerRow: Float,
    colSpan: Int,
    rowSpan: Int,
    columns: Int,
): Pair<Int, Int> {
    val idealCol = pointerCol - colSpan / 2f
    val idealRow = pointerRow - rowSpan / 2f
    val rawCol = nearestAlignedColumn(idealCol, colSpan, columns)
    val rawRow = idealRow.roundToInt().coerceAtLeast(0)
    return rawCol to rawRow
}

/** Nearest valid top-left column for [colSpan], from a continuous ideal top-left. */
fun nearestAlignedColumn(idealCol: Float, colSpan: Int, columns: Int): Int {
    val maxCol = (columns - colSpan).coerceAtLeast(0)
    return idealCol.roundToInt().coerceIn(0, maxCol)
}

/**
 * Seats [draggedKey] at [slotCol]/[slotRow], displacing overlapping tiles from [baseline].
 * Non-overlapping tiles keep their baseline coordinates (gaps preserved).
 */
fun placeTileAt(
    baseline: Map<TilePlacementKey, GridPlacement>,
    draggedKey: TilePlacementKey,
    slotCol: Int,
    slotRow: Int,
    colSpan: Int,
    rowSpan: Int,
    columns: Int,
): Map<TilePlacementKey, GridPlacement> {
    val draggedSpanC = colSpan
    val draggedSpanR = rowSpan
    val seatCol = alignTileColumn(slotCol, draggedSpanC, columns)
    val seatRow = slotRow.coerceAtLeast(0)
    val pushBelowRow = seatRow + draggedSpanR

    val others = baseline.filterKeys { it != draggedKey }.toMutableMap()
    val draggedSeat = GridPlacement(
        key = draggedKey,
        col = seatCol,
        row = seatRow,
        colSpan = draggedSpanC,
        rowSpan = draggedSpanR,
    )

    val displaced = others.values
        .filter {
            tileOverlapsRegion(
                it.col, it.row, it.colSpan, it.rowSpan,
                seatCol, seatRow, draggedSpanC, draggedSpanR,
            )
        }
        .sortedWith(compareBy({ it.row }, { it.col }, { it.key.packageName }, { it.key.tileId }))

    for (victim in displaced) {
        others.remove(victim.key)
    }

    val settled = LinkedHashMap<TilePlacementKey, GridPlacement>()
    settled[draggedKey] = draggedSeat

    fun occupiedSet(): MutableSet<Pair<Int, Int>> {
        val occupied = mutableSetOf<Pair<Int, Int>>()
        for (p in settled.values) {
            markTileCells(occupied, p.col, p.row, p.colSpan, p.rowSpan)
        }
        for (p in others.values) {
            markTileCells(occupied, p.col, p.row, p.colSpan, p.rowSpan)
        }
        return occupied
    }

    fun shiftRowsFrom(fromRow: Int, amount: Int) {
        if (amount <= 0) return
        fun bump(map: MutableMap<TilePlacementKey, GridPlacement>) {
            for (key in map.keys.toList()) {
                val p = map[key] ?: continue
                if (p.row >= fromRow) {
                    map[key] = p.copy(row = p.row + amount)
                }
            }
        }
        bump(others)
        for (key in settled.keys.filter { it != draggedKey }.toList()) {
            val p = settled[key] ?: continue
            if (p.row >= fromRow) {
                settled[key] = p.copy(row = p.row + amount)
            }
        }
    }

    // Phase 1: slide into a free same-row gap when one exists (local, low-impact).
    val mustPush = mutableListOf<GridPlacement>()
    for (victim in displaced) {
        val sameRow = findSameRowGap(
            occupied = occupiedSet(),
            colSpan = victim.colSpan,
            rowSpan = victim.rowSpan,
            columns = columns,
            preferCol = victim.col,
            preferRow = victim.row,
        )
        if (sameRow != null) {
            settled[victim.key] = victim.copy(col = sameRow.first, row = sameRow.second)
        } else {
            mustPush += victim
        }
    }

    // Phase 2: one insert + pack for everyone that couldn't slide — keeps small groups
    // contiguous under the seated tile instead of N fragmented 1-row inserts.
    if (mustPush.isNotEmpty()) {
        // Clear the seated footprint, and don't start mid-band under a taller victim
        // (e.g. medium pushed by a small should land at row 2, not row 1).
        val bandStart = maxOf(
            pushBelowRow,
            mustPush.maxOf { it.row + it.rowSpan },
        )
        val groupMinRow = mustPush.minOf { it.row }
        val desired = mustPush.map { victim ->
            val col = alignTileColumn(victim.col, victim.colSpan, columns)
            val row = bandStart + (victim.row - groupMinRow)
            victim to (col to row)
        }
        fun desiredFits(occupied: Set<Pair<Int, Int>>): Boolean {
            val trial = occupied.toMutableSet()
            for ((victim, pos) in desired) {
                val (col, row) = pos
                if (!canPlaceAt(trial, col, row, victim.colSpan, victim.rowSpan, columns)) {
                    return false
                }
                markTileCells(trial, col, row, victim.colSpan, victim.rowSpan)
            }
            return true
        }
        if (!desiredFits(occupiedSet())) {
            val stripHeight = measurePreservedGroupHeight(mustPush, groupMinRow)
            shiftRowsFrom(bandStart, stripHeight)
        }
        val occupied = occupiedSet()
        for ((victim, pos) in desired) {
            val (targetCol, targetRow) = pos
            val (col, row) = when {
                canPlaceAt(occupied, targetCol, targetRow, victim.colSpan, victim.rowSpan, columns) ->
                    targetCol to targetRow
                else -> findSameRowGap(
                    occupied, victim.colSpan, victim.rowSpan, columns,
                    preferCol = victim.col, preferRow = targetRow,
                ) ?: findFirstOpenAlignedSlot(
                    occupied, victim.colSpan, victim.rowSpan, columns, startRow = bandStart,
                ) ?: (targetCol to targetRow)
            }
            markTileCells(occupied, col, row, victim.colSpan, victim.rowSpan)
            settled[victim.key] = victim.copy(col = col, row = row)
        }
    }

    // Cascade: others overlapped after a shift (rare) get the same treatment.
    var guard = 0
    while (guard++ < baseline.size) {
        val conflict = others.values.firstOrNull { other ->
            settled.values.any { s ->
                tileOverlapsRegion(
                    other.col, other.row, other.colSpan, other.rowSpan,
                    s.col, s.row, s.colSpan, s.rowSpan,
                )
            }
        } ?: break
        others.remove(conflict.key)
        val sameRow = findSameRowGap(
            occupied = occupiedSet(),
            colSpan = conflict.colSpan,
            rowSpan = conflict.rowSpan,
            columns = columns,
            preferCol = conflict.col,
            preferRow = conflict.row,
        )
        if (sameRow != null) {
            settled[conflict.key] = conflict.copy(col = sameRow.first, row = sameRow.second)
            continue
        }
        val stripHeight = measureFirstFitHeight(listOf(conflict), columns)
        val pushFrom = maxOf(pushBelowRow, conflict.row + conflict.rowSpan)
        shiftRowsFrom(pushFrom, stripHeight)
        val occupied = occupiedSet()
        val (col, row) = findFirstOpenAlignedSlot(
            occupied,
            conflict.colSpan,
            conflict.rowSpan,
            columns,
            startRow = pushFrom,
        ) ?: (alignTileColumn(conflict.col, conflict.colSpan, columns) to pushFrom)
        settled[conflict.key] = conflict.copy(col = col, row = row)
    }

    settled.putAll(others)
    return settled
}

/** Valid top-left columns for a footprint of [colSpan] on a [columns]-wide grid. */
internal fun alignedColumns(colSpan: Int, columns: Int): IntProgression {
    val maxCol = (columns - colSpan).coerceAtLeast(0)
    return 0..maxCol
}

/** Nearest open footprint on [preferRow], or null if the row has no gap. */
internal fun findSameRowGap(
    occupied: Set<Pair<Int, Int>>,
    colSpan: Int,
    rowSpan: Int,
    columns: Int,
    preferCol: Int,
    preferRow: Int,
): Pair<Int, Int>? {
    val alignedPrefer = alignTileColumn(preferCol, colSpan, columns)
    if (canPlaceAt(occupied, alignedPrefer, preferRow, colSpan, rowSpan, columns)) {
        return alignedPrefer to preferRow
    }
    var bestCol: Int? = null
    var bestDist = Int.MAX_VALUE
    for (col in alignedColumns(colSpan, columns)) {
        if (canPlaceAt(occupied, col, preferRow, colSpan, rowSpan, columns)) {
            val dist = abs(col - alignedPrefer)
            if (dist < bestDist) {
                bestDist = dist
                bestCol = col
            }
        }
    }
    return bestCol?.let { it to preferRow }
}

/** Rows needed to re-seat [tiles] preserving their relative (col, row) offsets. */
internal fun measurePreservedGroupHeight(
    tiles: List<GridPlacement>,
    groupMinRow: Int,
): Int {
    if (tiles.isEmpty()) return 0
    return tiles.maxOf { (it.row - groupMinRow) + it.rowSpan }
}

/** How many rows a first-fit pack of [tiles] needs on an empty [columns]-wide strip. */
internal fun measureFirstFitHeight(
    tiles: List<GridPlacement>,
    columns: Int,
): Int {
    if (tiles.isEmpty()) return 0
    val occupied = mutableSetOf<Pair<Int, Int>>()
    var maxBottom = 0
    for (tile in tiles) {
        val (col, row) = findFirstOpenAlignedSlot(
            occupied, tile.colSpan, tile.rowSpan, columns, startRow = 0,
        ) ?: (0 to maxBottom)
        markTileCells(occupied, col, row, tile.colSpan, tile.rowSpan)
        maxBottom = maxOf(maxBottom, row + tile.rowSpan)
    }
    return maxBottom
}

internal fun findFirstOpenAlignedSlot(
    occupied: Set<Pair<Int, Int>>,
    colSpan: Int,
    rowSpan: Int,
    columns: Int,
    startRow: Int,
    maxRows: Int = 256,
): Pair<Int, Int>? {
    for (row in startRow until startRow + maxRows) {
        for (col in alignedColumns(colSpan, columns)) {
            if (canPlaceAt(occupied, col, row, colSpan, rowSpan, columns)) {
                return col to row
            }
        }
    }
    return null
}

/**
 * Compacts fully empty rows out of a placement map (horizontal gaps untouched).
 */
fun compactPlacementRows(
    placements: Map<TilePlacementKey, GridPlacement>,
): Map<TilePlacementKey, GridPlacement> {
    if (placements.isEmpty()) return placements
    val rowMap = rowCompactionMap(placements.values.map { it.row to it.rowSpan })
    return placements.mapValues { (_, p) ->
        p.copy(row = rowMap[p.row] ?: p.row)
    }
}
