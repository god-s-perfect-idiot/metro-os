package com.metro.launcher.ui

import com.metro.launcher.data.DisplayTile
import com.metro.launcher.data.PinnedTileEntry
import com.metro.launcher.data.PinnedTileSize
import com.metro.launcher.data.PinnedTileStore
import com.metro.launcher.data.TileSizeCycle
import com.metro.launcher.data.applyTileResize
import com.metro.launcher.data.compactEmptyRows
import com.metro.launcher.data.ensureGridPositions
import com.metro.launcher.data.tileOverlapsRegion
import com.metro.launcher.ui.compactEmptyRowPlacements
import com.metro.system.MetroTileContract
import com.metro.ui.MetroColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TileGridTest {
    @Test
    fun grid_hasFourColumns() {
        assertEquals(4, TILE_GRID_COLUMNS)
    }

    @Test
    fun tileSizeCycle_followsBlueprint() {
        assertEquals(PinnedTileSize.TwoByTwo, TileSizeCycle.nextSize(PinnedTileSize.OneByOne))
        assertEquals(PinnedTileSize.FourByTwo, TileSizeCycle.nextSize(PinnedTileSize.TwoByTwo))
        assertEquals(PinnedTileSize.OneByOne, TileSizeCycle.nextSize(PinnedTileSize.FourByTwo))
    }

    @Test
    fun defaultPins_includeShippedMetroApps() {
        val pins = PinnedTileStore.defaultPins()
        assertEquals(6, pins.size)
        assertEquals("com.metro.people", pins.first().packageName)
        assertEquals("com.metro.calculator", pins.last().packageName)
        assertTrue(pins.none { it.packageName in setOf(
            "com.metro.browser",
            "com.metro.notes",
            "com.metro.music",
            "com.metro.settings",
            "com.metro.store",
        ) })
    }

    @Test
    fun wideTile_spansFullGridWidth() {
        val tile = displayTile("com.metro.photos", PinnedTileSize.FourByTwo, col = 0, row = 0)
        val placed = layoutTilesOnGrid(listOf(tile))
        assertEquals(1, placed.size)
        assertEquals(0, placed.first().col)
        assertEquals(4, placed.first().tile.entry.size.colSpan)
    }

    @Test
    fun layout_preservesColumnGapsButCompactsEmptyRows() {
        val tiles = listOf(
            displayTile("a", PinnedTileSize.OneByOne, col = 0, row = 0),
            displayTile("b", PinnedTileSize.OneByOne, col = 2, row = 0),
            displayTile("c", PinnedTileSize.OneByOne, col = 0, row = 2),
        )
        val placed = layoutTilesOnGrid(tiles)
        assertEquals(3, placed.size)
        assertEquals(1, placed.first { it.tile.entry.packageName == "c" }.row)
        assertTrue(placed.none { it.col == 1 && it.row == 0 })
    }

    @Test
    fun compactEmptyRows_shiftsDownTilesWithoutChangingColumns() {
        val entries = listOf(
            PinnedTileEntry("a", size = PinnedTileSize.OneByOne, gridCol = 0, gridRow = 0),
            PinnedTileEntry("b", size = PinnedTileSize.OneByOne, gridCol = 2, gridRow = 3),
        )
        val compacted = compactEmptyRows(entries)
        assertEquals(0, compacted[0].gridRow)
        assertEquals(0, compacted[0].gridCol)
        assertEquals(1, compacted[1].gridRow)
        assertEquals(2, compacted[1].gridCol)
    }

    @Test
    fun resizeGlyph_matchesSizeCycle() {
        assertEquals(TileResizeGlyph.DiagonalDownRight, resizeGlyphForTileSize(PinnedTileSize.OneByOne))
        assertEquals(TileResizeGlyph.Right, resizeGlyphForTileSize(PinnedTileSize.TwoByTwo))
        assertEquals(TileResizeGlyph.DiagonalUpLeft, resizeGlyphForTileSize(PinnedTileSize.FourByTwo))
    }

    @Test
    fun idleFloatParams_differBySeed() {
        val a = TileIdleFloatParams.fromSeed(1)
        val b = TileIdleFloatParams.fromSeed(99)
        assertNotEquals(a, b)
        assertTrue(a.ampXDp in 3.5f..8.5f)
        assertTrue(a.ampYDp in 3f..8f)
        assertTrue(a.durationXMs in 2200..4000)
    }

    @Test
    fun ensureGridPositions_assignsFirstFitForMissingSlots() {
        val entries = listOf(
            PinnedTileEntry("a", size = PinnedTileSize.TwoByTwo),
            PinnedTileEntry("b", size = PinnedTileSize.TwoByTwo),
        )
        val positioned = ensureGridPositions(entries)
        assertEquals(0, positioned[0].gridCol)
        assertEquals(0, positioned[0].gridRow)
        assertEquals(2, positioned[1].gridCol)
        assertEquals(0, positioned[1].gridRow)
    }

    @Test
    fun applyTileResize_clampsColumnWhenExpandingPastRightEdge() {
        val entries = listOf(
            PinnedTileEntry("a", size = PinnedTileSize.TwoByTwo, gridCol = 2, gridRow = 0),
            PinnedTileEntry("b", size = PinnedTileSize.OneByOne, gridCol = 0, gridRow = 0),
        )
        val resized = applyTileResize(entries, "a", "primary", PinnedTileSize.FourByTwo)
        val a = resized.first { it.packageName == "a" }
        assertEquals(PinnedTileSize.FourByTwo, a.size)
        assertEquals(0, a.gridCol)
        assertEquals(0, a.gridRow)
        assertTrue(a.gridCol!! + a.size.colSpan <= TILE_GRID_COLUMNS)
        val b = resized.first { it.packageName == "b" }
        assertFalse(
            tileOverlapsRegion(
                b.gridCol!!,
                b.gridRow!!,
                b.size.colSpan,
                b.size.rowSpan,
                a.gridCol!!,
                a.gridRow!!,
                a.size.colSpan,
                a.size.rowSpan,
            ),
        )
    }

    @Test
    fun applyTileResize_shiftsLeftWhenMediumWouldOverflow() {
        val entries = listOf(
            PinnedTileEntry("a", size = PinnedTileSize.OneByOne, gridCol = 3, gridRow = 0),
        )
        val resized = applyTileResize(entries, "a", "primary", PinnedTileSize.TwoByTwo)
        val a = resized.first()
        assertEquals(2, a.gridCol)
        assertEquals(PinnedTileSize.TwoByTwo, a.size)
        assertTrue(a.gridCol!! + a.size.colSpan <= TILE_GRID_COLUMNS)
    }

    @Test
    fun applyTileResize_pushesOverlappingNeighbors() {
        val entries = listOf(
            PinnedTileEntry("a", size = PinnedTileSize.OneByOne, gridCol = 0, gridRow = 0),
            PinnedTileEntry("b", size = PinnedTileSize.OneByOne, gridCol = 1, gridRow = 0),
            PinnedTileEntry("c", size = PinnedTileSize.OneByOne, gridCol = 0, gridRow = 1),
            PinnedTileEntry("d", size = PinnedTileSize.OneByOne, gridCol = 1, gridRow = 1),
        )
        val resized = applyTileResize(entries, "a", "primary", PinnedTileSize.TwoByTwo)
        val a = resized.first { it.packageName == "a" }
        assertEquals(0, a.gridCol)
        assertEquals(0, a.gridRow)
        assertEquals(PinnedTileSize.TwoByTwo, a.size)
        for (other in resized.filter { it.packageName != "a" }) {
            assertFalse(
                tileOverlapsRegion(
                    other.gridCol!!,
                    other.gridRow!!,
                    other.size.colSpan,
                    other.size.rowSpan,
                    a.gridCol!!,
                    a.gridRow!!,
                    a.size.colSpan,
                    a.size.rowSpan,
                ),
            )
        }
    }

    @Test
    fun dragLayout_flowsNeighborsAroundHole() {
        val a = displayTile("a", PinnedTileSize.TwoByTwo, col = 0, row = 0)
        val b = displayTile("b", PinnedTileSize.TwoByTwo, col = 2, row = 0)
        val c = displayTile("c", PinnedTileSize.OneByOne, col = 0, row = 2)
        val d = displayTile("d", PinnedTileSize.OneByOne, col = 1, row = 2)
        val e = displayTile("e", PinnedTileSize.OneByOne, col = 2, row = 2)
        val f = displayTile("f", PinnedTileSize.OneByOne, col = 3, row = 2)
        val tiles = listOf(a, b, c, d, e, f)
        val baseline = tiles.associate { it.tileKey() to (it.entry.gridCol!! to it.entry.gridRow!!) }
        // Hole at (1,2) for f — d slides past the hole on the same row.
        val placed = layoutTilesForDrag(tiles, f, slotCol = 1, slotRow = 2, baseline)
        assertEquals(1, placed.first { it.tile.entry.packageName == "f" }.col)
        assertEquals(2, placed.first { it.tile.entry.packageName == "f" }.row)
        assertEquals(0, placed.first { it.tile.entry.packageName == "c" }.col)
        assertEquals(2, placed.first { it.tile.entry.packageName == "c" }.row)
        assertEquals(2, placed.first { it.tile.entry.packageName == "d" }.col)
        assertEquals(2, placed.first { it.tile.entry.packageName == "d" }.row)
        assertEquals(3, placed.first { it.tile.entry.packageName == "e" }.col)
        assertEquals(2, placed.first { it.tile.entry.packageName == "e" }.row)
        assertEquals(0, placed.first { it.tile.entry.packageName == "a" }.col)
        assertEquals(2, placed.first { it.tile.entry.packageName == "b" }.col)
    }

    @Test
    fun dragLayout_wrapsWhenTileCannotStayOnRow() {
        val a = displayTile("a", PinnedTileSize.FourByTwo, col = 0, row = 0)
        val b = displayTile("b", PinnedTileSize.OneByOne, col = 0, row = 2)
        val c = displayTile("c", PinnedTileSize.OneByOne, col = 1, row = 2)
        val d = displayTile("d", PinnedTileSize.OneByOne, col = 2, row = 2)
        val e = displayTile("e", PinnedTileSize.OneByOne, col = 3, row = 2)
        val dragged = displayTile("x", PinnedTileSize.OneByOne, col = 3, row = 3)
        val tiles = listOf(a, b, c, d, e, dragged)
        val baseline = tiles.associate { it.tileKey() to (it.entry.gridCol!! to it.entry.gridRow!!) }
        // Hole at (2,2) — d stays on the row past the hole; e wraps to the next row.
        val placed = layoutTilesForDrag(tiles, dragged, slotCol = 2, slotRow = 2, baseline)
        assertEquals(2, placed.first { it.tile.entry.packageName == "x" }.col)
        assertEquals(2, placed.first { it.tile.entry.packageName == "x" }.row)
        assertEquals(3, placed.first { it.tile.entry.packageName == "d" }.col)
        assertEquals(2, placed.first { it.tile.entry.packageName == "d" }.row)
        assertEquals(0, placed.first { it.tile.entry.packageName == "e" }.col)
        assertEquals(3, placed.first { it.tile.entry.packageName == "e" }.row)
    }

    @Test
    fun dragLayout_fillsEarlierGapsWhileKeepingHole() {
        val a = displayTile("a", PinnedTileSize.TwoByTwo, col = 0, row = 0)
        val b = displayTile("b", PinnedTileSize.OneByOne, col = 3, row = 4)
        val dragged = displayTile("c", PinnedTileSize.OneByOne, col = 0, row = 2)
        val tiles = listOf(a, b, dragged)
        val baseline = tiles.associate { it.tileKey() to (it.entry.gridCol!! to it.entry.gridRow!!) }
        val placed = layoutTilesForDrag(tiles, dragged, slotCol = 1, slotRow = 2, baseline)
        assertEquals(1, placed.first { it.tile.entry.packageName == "c" }.col)
        assertEquals(2, placed.first { it.tile.entry.packageName == "c" }.row)
        // b fills the first open cell after a (gap collapses via flow pack).
        assertEquals(2, placed.first { it.tile.entry.packageName == "b" }.col)
        assertEquals(0, placed.first { it.tile.entry.packageName == "b" }.row)
    }

    @Test
    fun packTilesInReadingOrder_wrapsOversizedRemainder() {
        val tiles = listOf(
            displayTile("a", PinnedTileSize.TwoByTwo),
            displayTile("b", PinnedTileSize.TwoByTwo),
            displayTile("c", PinnedTileSize.OneByOne),
            displayTile("d", PinnedTileSize.FourByTwo),
        )
        val placed = packTilesInReadingOrder(tiles)
        assertEquals(0, placed[0].col)
        assertEquals(0, placed[0].row)
        assertEquals(2, placed[1].col)
        assertEquals(0, placed[1].row)
        assertEquals(0, placed[2].col)
        assertEquals(2, placed[2].row)
        // Wide tile cannot share row 2 with c → falls to row 3.
        assertEquals(0, placed[3].col)
        assertEquals(3, placed[3].row)
    }

    @Test
    fun snapDragSlotWithHysteresis_holdsNearBoundary() {
        // pointerCol is tile center; ideal top-left = center - colSpan/2.
        // ideal=1.6 → raw=2, but still short of 1.78 boundary with hysteresis.
        val (col, row) = snapDragSlotWithHysteresis(
            pointerCol = 2.1f,
            pointerRow = 0.5f,
            colSpan = 1,
            rowSpan = 1,
            currentCol = 1,
            currentRow = 0,
            hysteresis = 0.28f,
        )
        assertEquals(1, col)
        assertEquals(0, row)
    }

    @Test
    fun snapDragSlotWithHysteresis_commitsAfterCrossing() {
        // ideal=1.8 → past 1.78 boundary → commit to column 2.
        val (col, row) = snapDragSlotWithHysteresis(
            pointerCol = 2.3f,
            pointerRow = 0.5f,
            colSpan = 1,
            rowSpan = 1,
            currentCol = 1,
            currentRow = 0,
            hysteresis = 0.28f,
        )
        assertEquals(2, col)
        assertEquals(0, row)
    }

    @Test
    fun photoGridDimensions_matchesTileSizes() {
        assertEquals(3 to 3, MetroTileContract.photoGridDimensions(2, 2))
        assertEquals(6 to 3, MetroTileContract.photoGridDimensions(4, 2))
    }

    private fun displayTile(
        packageName: String,
        size: PinnedTileSize,
        col: Int? = null,
        row: Int? = null,
    ) = DisplayTile(
        entry = PinnedTileEntry(packageName, size = size, gridCol = col, gridRow = row),
        title = packageName,
        backgroundColor = MetroColors.AccentBlue,
        counter = null,
        deepLinkUri = null,
        hasFlipFace = false,
    )
}
