package com.metro.launcher.ui

import com.metro.launcher.data.DisplayTile
import kotlin.math.abs
import kotlin.math.roundToInt

/** Identity for a pinned Start tile (package + tile id). */
data class TileKey(val packageName: String, val tileId: String)

/** Shorter than [android.view.ViewConfiguration.getLongPressTimeout] (~400ms) for tile edit. */
const val TILE_DRAG_HOLD_MS = 250L

fun DisplayTile.tileKey(): TileKey = TileKey(entry.packageName, entry.tileId)

fun sameTile(a: DisplayTile, b: DisplayTile): Boolean =
    a.entry.packageName == b.entry.packageName && a.entry.tileId == b.entry.tileId

/** Snap pointer (grid units) to the top-left cell for a dragged tile footprint. */
fun snapDragSlot(
    pointerCol: Float,
    pointerRow: Float,
    colSpan: Int,
    rowSpan: Int,
    columns: Int = TILE_GRID_COLUMNS,
): Pair<Int, Int> {
    val col = (pointerCol - colSpan / 2f).roundToInt().coerceIn(0, columns - colSpan)
    val row = (pointerRow - rowSpan / 2f).roundToInt().coerceAtLeast(0)
    return col to row
}

/**
 * Like [snapDragSlot], but keeps [currentCol]/[currentRow] until the pointer crosses the cell
 * midpoint by [hysteresis] grid units — prevents magnet reflow flicker at boundaries.
 */
fun snapDragSlotWithHysteresis(
    pointerCol: Float,
    pointerRow: Float,
    colSpan: Int,
    rowSpan: Int,
    currentCol: Int,
    currentRow: Int,
    columns: Int = TILE_GRID_COLUMNS,
    hysteresis: Float = 0.28f,
): Pair<Int, Int> {
    val idealCol = pointerCol - colSpan / 2f
    val idealRow = pointerRow - rowSpan / 2f
    val rawCol = idealCol.roundToInt().coerceIn(0, columns - colSpan)
    val rawRow = idealRow.roundToInt().coerceAtLeast(0)
    val col = holdSlotUntilPastMidpoint(idealCol, rawCol, currentCol, hysteresis)
        .coerceIn(0, columns - colSpan)
    val row = holdSlotUntilPastMidpoint(idealRow, rawRow, currentRow, hysteresis)
        .coerceAtLeast(0)
    return col to row
}

private fun holdSlotUntilPastMidpoint(
    continuous: Float,
    raw: Int,
    current: Int,
    hysteresis: Float,
): Int {
    if (raw == current) return current
    val boundary = if (raw > current) {
        current + 0.5f + hysteresis
    } else {
        current - 0.5f - hysteresis
    }
    return when {
        raw > current && continuous >= boundary -> raw
        raw < current && continuous <= boundary -> raw
        else -> current
    }
}
