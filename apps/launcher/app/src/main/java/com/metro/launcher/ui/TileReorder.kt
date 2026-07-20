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
