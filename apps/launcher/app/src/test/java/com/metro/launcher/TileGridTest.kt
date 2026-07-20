package com.metro.launcher.ui

import com.metro.launcher.data.DisplayTile
import com.metro.launcher.data.PinnedTileEntry
import com.metro.launcher.data.PinnedTileSize
import com.metro.launcher.data.PinnedTileStore
import com.metro.launcher.data.TileSizeCycle
import com.metro.launcher.data.compactEmptyRows
import com.metro.launcher.data.ensureGridPositions
import com.metro.launcher.ui.compactEmptyRowPlacements
import com.metro.system.MetroTileContract
import com.metro.ui.MetroColors
import org.junit.Assert.assertEquals
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
    fun dragLayout_reservesSlotWithSideGaps() {
        val a = displayTile("a", PinnedTileSize.TwoByTwo, col = 0, row = 0)
        val b = displayTile("b", PinnedTileSize.TwoByTwo, col = 2, row = 0)
        val c = displayTile("c", PinnedTileSize.OneByOne, col = 0, row = 2)
        val d = displayTile("d", PinnedTileSize.OneByOne, col = 1, row = 2)
        val e = displayTile("e", PinnedTileSize.OneByOne, col = 2, row = 2)
        val f = displayTile("f", PinnedTileSize.OneByOne, col = 3, row = 2)
        val tiles = listOf(a, b, c, d, e, f)
        val baseline = tiles.associate { it.tileKey() to (it.entry.gridCol!! to it.entry.gridRow!!) }
        val placed = layoutTilesForDrag(tiles, f, slotCol = 1, slotRow = 2, baseline)
        val fPlacement = placed.first { it.tile.entry.packageName == "f" }
        assertEquals(1, fPlacement.col)
        assertEquals(2, fPlacement.row)
        val cPlacement = placed.first { it.tile.entry.packageName == "c" }
        assertEquals(0, cPlacement.col)
        assertEquals(2, cPlacement.row)
        val dPlacement = placed.first { it.tile.entry.packageName == "d" }
        assertEquals(3, dPlacement.col)
        assertEquals(2, dPlacement.row)
        assertEquals(0, placed.first { it.tile.entry.packageName == "a" }.col)
        assertEquals(2, placed.first { it.tile.entry.packageName == "b" }.col)
        assertEquals(2, placed.first { it.tile.entry.packageName == "e" }.col)
    }

    @Test
    fun dragLayout_onlyPushesOverlappingTiles() {
        val a = displayTile("a", PinnedTileSize.FourByTwo, col = 0, row = 0)
        val b = displayTile("b", PinnedTileSize.OneByOne, col = 0, row = 2)
        val c = displayTile("c", PinnedTileSize.OneByOne, col = 1, row = 2)
        val d = displayTile("d", PinnedTileSize.OneByOne, col = 2, row = 2)
        val e = displayTile("e", PinnedTileSize.OneByOne, col = 3, row = 2)
        val dragged = displayTile("x", PinnedTileSize.OneByOne, col = 3, row = 3)
        val tiles = listOf(a, b, c, d, e, dragged)
        val baseline = tiles.associate { it.tileKey() to (it.entry.gridCol!! to it.entry.gridRow!!) }
        val placed = layoutTilesForDrag(tiles, dragged, slotCol = 2, slotRow = 2, baseline)
        assertEquals(0, placed.first { it.tile.entry.packageName == "d" }.col)
        assertEquals(3, placed.first { it.tile.entry.packageName == "d" }.row)
        assertEquals(3, placed.first { it.tile.entry.packageName == "e" }.col)
        assertEquals(2, placed.first { it.tile.entry.packageName == "e" }.row)
        assertEquals(0, placed.first { it.tile.entry.packageName == "b" }.col)
        assertEquals(2, placed.first { it.tile.entry.packageName == "b" }.row)
    }

    @Test
    fun dragLayout_doesNotCollapseDistantGaps() {
        val a = displayTile("a", PinnedTileSize.TwoByTwo, col = 0, row = 0)
        val b = displayTile("b", PinnedTileSize.OneByOne, col = 3, row = 4)
        val dragged = displayTile("c", PinnedTileSize.OneByOne, col = 0, row = 2)
        val tiles = listOf(a, b, dragged)
        val baseline = tiles.associate { it.tileKey() to (it.entry.gridCol!! to it.entry.gridRow!!) }
        val placed = layoutTilesForDrag(tiles, dragged, slotCol = 1, slotRow = 2, baseline)
        val bPlacement = placed.first { it.tile.entry.packageName == "b" }
        assertEquals(3, bPlacement.col)
        assertEquals(4, bPlacement.row)
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
