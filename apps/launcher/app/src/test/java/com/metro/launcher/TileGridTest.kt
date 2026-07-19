package com.metro.launcher

import com.metro.launcher.data.PinnedTileSize
import com.metro.launcher.data.PinnedTileStore
import com.metro.launcher.data.TileSizeCycle
import com.metro.launcher.ui.TILE_GRID_COLUMNS
import com.metro.launcher.ui.TileIdleFloatParams
import com.metro.launcher.ui.TileResizeGlyph
import com.metro.launcher.ui.layoutTilesOnGrid
import com.metro.launcher.ui.resizeGlyphForTileSize
import com.metro.launcher.data.DisplayTile
import com.metro.launcher.data.PinnedTileEntry
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
        val tile = displayTile("com.metro.photos", PinnedTileSize.FourByTwo)
        val placed = layoutTilesOnGrid(listOf(tile))
        assertEquals(1, placed.size)
        assertEquals(0, placed.first().col)
        assertEquals(4, placed.first().tile.entry.size.colSpan)
    }

    @Test
    fun layout_doesNotOverlap() {
        val tiles = listOf(
            displayTile("a", PinnedTileSize.TwoByTwo),
            displayTile("b", PinnedTileSize.TwoByTwo),
            displayTile("c", PinnedTileSize.OneByOne),
        )
        val placed = layoutTilesOnGrid(tiles)
        assertEquals(3, placed.size)
        val positions = placed.map { it.col to it.row }.toSet()
        assertEquals(3, positions.size)
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
    fun photoGridDimensions_matchesTileSizes() {
        assertEquals(3 to 3, MetroTileContract.photoGridDimensions(2, 2))
        assertEquals(6 to 3, MetroTileContract.photoGridDimensions(4, 2))
    }

    private fun displayTile(packageName: String, size: PinnedTileSize) = DisplayTile(
        entry = PinnedTileEntry(packageName, size = size),
        title = packageName,
        backgroundColor = MetroColors.AccentBlue,
        counter = null,
        deepLinkUri = null,
        hasFlipFace = false,
    )
}
