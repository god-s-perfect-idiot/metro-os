package com.metro.launcher.ui

import com.metro.launcher.data.DisplayTile
import com.metro.launcher.data.alignTileColumn
import com.metro.launcher.data.nearestAlignedColumn
import com.metro.launcher.data.snapAlignedSlot
import kotlin.math.roundToInt

/** Identity for a pinned Start tile (package + tile id). */
data class TileKey(val packageName: String, val tileId: String)

/** Disco Launcher edit hold — 500ms pointerdown before edit mode arms. */
const val TILE_DRAG_HOLD_MS = TILE_EDIT_HOLD_MS

fun DisplayTile.tileKey(): TileKey = TileKey(entry.packageName, entry.tileId)

fun sameTile(a: DisplayTile, b: DisplayTile): Boolean =
    a.entry.packageName == b.entry.packageName && a.entry.tileId == b.entry.tileId

/** Snap pointer (grid units) to a size-aligned top-left cell for a dragged tile. */
fun snapDragSlot(
    pointerCol: Float,
    pointerRow: Float,
    colSpan: Int,
    rowSpan: Int,
    columns: Int = TILE_GRID_COLUMNS,
): Pair<Int, Int> = snapAlignedSlot(pointerCol, pointerRow, colSpan, rowSpan, columns)

/**
 * Like [snapDragSlot], but keeps [currentCol]/[currentRow] until the pointer crosses
 * halfway to the next cell by [hysteresis] — prevents magnet flicker.
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
    val rawCol = nearestAlignedColumn(idealCol, colSpan, columns)
    val rawRow = idealRow.roundToInt().coerceAtLeast(0)
    val col = holdSlotUntilPastMidpoint(idealCol, rawCol, currentCol, step = 1, hysteresis)
        .let { alignTileColumn(it, colSpan, columns) }
    val row = holdSlotUntilPastMidpoint(idealRow, rawRow, currentRow, step = 1, hysteresis)
        .coerceAtLeast(0)
    return col to row
}

private fun holdSlotUntilPastMidpoint(
    continuous: Float,
    raw: Int,
    current: Int,
    step: Int,
    hysteresis: Float,
): Int {
    if (raw == current) return current
    val half = step / 2f
    val boundary = if (raw > current) {
        current + half + hysteresis
    } else {
        current - half - hysteresis
    }
    return when {
        raw > current && continuous >= boundary -> raw
        raw < current && continuous <= boundary -> raw
        else -> current
    }
}
