package com.metro.launcher.ui

import com.metro.launcher.data.DisplayTile
import com.metro.launcher.data.PinnedTileEntry
import com.metro.launcher.data.PinnedTileSize
import com.metro.launcher.data.PinnedTileStore
import com.metro.launcher.data.TileSizeCycle
import com.metro.launcher.data.adaptTilesToColumnCount
import com.metro.launcher.data.applyTileResize
import com.metro.launcher.data.compactEmptyRows
import com.metro.launcher.data.ensureGridPositions
import com.metro.launcher.data.tileGridColumnCount
import com.metro.launcher.data.tileOverlapsRegion
import com.metro.launcher.data.TILE_GRID_COLUMN_COUNT_EXPANDED
import com.metro.system.MetroTileContract
import com.metro.ui.MetroColors
import com.metro.ui.MetroSystemIconType
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TileGridTest {
    @Test
    fun grid_hasFourColumnsByDefault() {
        assertEquals(4, TILE_GRID_COLUMNS)
        assertEquals(4, tileGridColumnCount(showMoreColumns = false))
    }

    @Test
    fun grid_hasSixColumnsWhenShowMoreColumns() {
        assertEquals(6, TILE_GRID_COLUMNS_EXPANDED)
        assertEquals(6, TILE_GRID_COLUMN_COUNT_EXPANDED)
        assertEquals(6, tileGridColumnCount(showMoreColumns = true))
    }

    @Test
    fun tileChrome_standardMatchesLegacyFourColumnMetrics() {
        val chrome = TileChrome.Standard
        assertEquals(12f, chrome.horizontalPadding.value)
        assertEquals(8f, chrome.contentInset.value)
        assertEquals(10f, chrome.smallIconInset.value)
        assertEquals(0.55f, chrome.mediumIconFraction)
        assertEquals(0.42f, chrome.wideIconFraction)
        assertEquals(16f, chrome.titleSp)
        assertEquals(TileChrome.Standard, TileChrome.forColumns(4))
    }

    @Test
    fun tileChrome_denseExtendsIconsAndShrinksTitles() {
        val chrome = TileChrome.Dense
        assertTrue(chrome.horizontalPadding < TileChrome.Standard.horizontalPadding)
        assertTrue(chrome.contentInset < TileChrome.Standard.contentInset)
        assertTrue(chrome.smallIconInset < TileChrome.Standard.smallIconInset)
        assertTrue(chrome.mediumIconFraction > TileChrome.Standard.mediumIconFraction)
        assertTrue(chrome.wideIconFraction > TileChrome.Standard.wideIconFraction)
        assertTrue(chrome.titleSp < TileChrome.Standard.titleSp)
        assertTrue(chrome.titleSp >= 13f)
        assertEquals(TileChrome.Dense, TileChrome.forColumns(6))
    }

    @Test
    fun tileNotificationDisplayCount_capsAtNinetyNineWithoutPlus() {
        assertEquals("1", tileNotificationDisplayCount(1))
        assertEquals("99", tileNotificationDisplayCount(99))
        assertEquals("99", tileNotificationDisplayCount(100))
        assertEquals("99", tileNotificationDisplayCount(999))
    }

    @Test
    fun tileChrome_denseSmallIconFillsMoreOfCell() {
        val tile = 50.dp
        val standard = TileChrome.Standard.iconSize(tile, tile, PinnedTileSize.OneByOne)
        val dense = TileChrome.Dense.iconSize(tile, tile, PinnedTileSize.OneByOne)
        assertTrue(dense > standard)
    }

    @Test
    fun tileChrome_denseMediumIconFillsMoreOfCell() {
        val tile = 110.dp
        val standard = TileChrome.Standard.iconSize(tile, tile, PinnedTileSize.TwoByTwo)
        val dense = TileChrome.Dense.iconSize(tile, tile, PinnedTileSize.TwoByTwo)
        assertTrue(dense > standard)
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
        // Horizontal gap at (1,0) is intentional and preserved.
        assertTrue(placed.none { it.col == 1 && it.row == 0 })
        assertEquals(2, placed.first { it.tile.entry.packageName == "b" }.col)
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
    fun gap_rightMediumAlonePreserved() {
        val entries = listOf(
            PinnedTileEntry("m", size = PinnedTileSize.TwoByTwo, gridCol = 2, gridRow = 0),
        )
        val kept = ensureGridPositions(entries)
        assertEquals(2, kept[0].gridCol)
        assertEquals(0, kept[0].gridRow)
    }

    @Test
    fun resizeIcon_matchesSizeCycle() {
        assertEquals(MetroSystemIconType.Resize, resizeIconForTileSize(PinnedTileSize.OneByOne))
        assertEquals(MetroSystemIconType.Forward, resizeIconForTileSize(PinnedTileSize.TwoByTwo))
        assertEquals(MetroSystemIconType.ResizeShrink, resizeIconForTileSize(PinnedTileSize.FourByTwo))
    }

    @Test
    fun resizeGlyphScale_forwardLargerThanDiagonals() {
        assertEquals(ResizeGlyphCanvasFraction, resizeGlyphScaleForTileSize(PinnedTileSize.OneByOne))
        assertEquals(ResizeForwardGlyphCanvasFraction, resizeGlyphScaleForTileSize(PinnedTileSize.TwoByTwo))
        assertEquals(ResizeGlyphCanvasFraction, resizeGlyphScaleForTileSize(PinnedTileSize.FourByTwo))
        assertTrue(ResizeForwardGlyphCanvasFraction > ResizeGlyphCanvasFraction)
    }

    @Test
    fun tileEditShake_movesWithTime() {
        val a = tileEditShakeAt(seed = 42, timeSec = 0.25f)
        val b = tileEditShakeAt(seed = 42, timeSec = 1.25f)
        assertTrue(a.offsetXDp != 0f || a.offsetYDp != 0f)
        assertNotEquals(a, b)
    }

    @Test
    fun tileEditShake_stillWhenTimeZero() {
        val still = tileEditShakeAt(seed = 42, timeSec = 0f)
        assertEquals(0f, still.offsetXDp)
        assertEquals(0f, still.offsetYDp)
    }

    @Test
    fun tileEditActiveFocusBouncePeak_matchesIntroPopRatio() {
        assertEquals(
            TILE_EDIT_INTRO_ACTIVE_SCALE / TILE_EDIT_ACTIVE_SCALE,
            TILE_EDIT_ACTIVE_FOCUS_BOUNCE_PEAK,
            0.0001f,
        )
    }

    @Test
    fun tileEditFocusScale_matchesEnterAndActiveEndpoints() {
        assertEquals(1f, tileEditFocusScale(editProgress = 0f, activeBlend = 0f))
        assertEquals(
            TILE_EDIT_ACTIVE_SCALE,
            tileEditFocusScale(editProgress = 1f, activeBlend = 1f),
            0.0001f,
        )
        assertEquals(
            TILE_EDIT_INACTIVE_SCALE,
            tileEditFocusScale(editProgress = 1f, activeBlend = 0f),
            0.0001f,
        )
    }

    @Test
    fun tileEditFocusScale_interpolatesActiveBlend() {
        val inactiveAtFull = TILE_EDIT_INACTIVE_SCALE
        val activeAtFull = TILE_EDIT_ACTIVE_SCALE
        val mid = tileEditFocusScale(editProgress = 1f, activeBlend = 0.5f)
        assertEquals((inactiveAtFull + activeAtFull) / 2f, mid, 0.0001f)
    }

    @Test
    fun tileEditFocusAlpha_matchesEnterAndActiveEndpoints() {
        assertEquals(1f, tileEditFocusAlpha(editProgress = 0f, activeBlend = 0f))
        assertEquals(1f, tileEditFocusAlpha(editProgress = 1f, activeBlend = 1f))
        assertEquals(
            TILE_EDIT_INACTIVE_ALPHA,
            tileEditFocusAlpha(editProgress = 1f, activeBlend = 0f),
            0.0001f,
        )
    }

    @Test
    fun tileEditPageScale_matchesPerspectiveRecess() {
        assertEquals(1f, tileEditPageScale(0f), 0.0001f)
        assertEquals(
            TILE_EDIT_PERSPECTIVE_DP / (TILE_EDIT_PERSPECTIVE_DP - TILE_EDIT_PAGE_Z_STEADY_DP),
            tileEditPageScale(1f),
            0.0001f,
        )
    }

    @Test
    fun tileResizeOvershoot_matchesSizeCycle() {
        assertEquals(
            0.10f,
            tileResizeOvershoot(PinnedTileSize.OneByOne, PinnedTileSize.TwoByTwo).translationFractionX,
        )
        assertEquals(
            0.10f,
            tileResizeOvershoot(PinnedTileSize.TwoByTwo, PinnedTileSize.FourByTwo).translationFractionY,
        )
        assertEquals(
            0.63f,
            tileResizeOvershoot(PinnedTileSize.TwoByTwo, PinnedTileSize.OneByOne).scaleMultiplier,
            0.0001f,
        )
    }

    @Test
    fun tileEnterDiagonal_startsAtBottomRight() {
        // 2×2 of 1×1 cells: bottom-right (1,1) is wave 0; top-left (0,0) is wave 2.
        assertEquals(
            0,
            tileEnterDiagonalIndex(col = 1, row = 1, colSpan = 1, rowSpan = 1, maxRight = 1, maxBottom = 1),
        )
        assertEquals(
            1,
            tileEnterDiagonalIndex(col = 0, row = 1, colSpan = 1, rowSpan = 1, maxRight = 1, maxBottom = 1),
        )
        assertEquals(
            1,
            tileEnterDiagonalIndex(col = 1, row = 0, colSpan = 1, rowSpan = 1, maxRight = 1, maxBottom = 1),
        )
        assertEquals(
            2,
            tileEnterDiagonalIndex(col = 0, row = 0, colSpan = 1, rowSpan = 1, maxRight = 1, maxBottom = 1),
        )
    }

    @Test
    fun tileEnterDiagonal_usesTileBottomRightCorner() {
        // Wide tile at (0,0) spanning 4×2: its bottom-right is (3,1) → same wave as a 1×1 there.
        assertEquals(
            0,
            tileEnterDiagonalIndex(col = 0, row = 0, colSpan = 4, rowSpan = 2, maxRight = 3, maxBottom = 1),
        )
        assertEquals(
            0,
            tileEnterDiagonalIndex(col = 3, row = 1, colSpan = 1, rowSpan = 1, maxRight = 3, maxBottom = 1),
        )
    }

    @Test
    fun tileEnterGridExtents_tracksMaxCorners() {
        val placed = listOf(
            PlacedTile(displayTile("a", PinnedTileSize.TwoByTwo, col = 0, row = 0), 0, 0),
            PlacedTile(displayTile("b", PinnedTileSize.OneByOne, col = 3, row = 2), 3, 2),
        )
        assertEquals(3 to 2, tileEnterGridExtents(placed))
    }

    @Test
    fun tileEnterWaveDuration_includesLastDiagonalAndSwing() {
        val placed = listOf(
            PlacedTile(displayTile("br", PinnedTileSize.OneByOne, col = 1, row = 1), 1, 1),
            PlacedTile(displayTile("tl", PinnedTileSize.OneByOne, col = 0, row = 0), 0, 0),
        )
        // Bottom-right diagonal 0; top-left diagonal 2 → 2×55 + 200
        assertEquals(2L * 55L + 200L, tileEnterWaveDurationMs(placed))
    }

    @Test
    fun tileExitDiagonal_matchesEnterWaveOrder() {
        assertEquals(
            0,
            tileExitDiagonalIndex(
                col = 1, row = 1, colSpan = 1, rowSpan = 1,
                maxRight = 1, maxBottom = 1,
            ),
        )
        assertEquals(
            2,
            tileExitDiagonalIndex(
                col = 0, row = 0, colSpan = 1, rowSpan = 1,
                maxRight = 1, maxBottom = 1,
            ),
        )
    }

    @Test
    fun tileExitWaveDuration_includesTappedTileAfterLastDiagonal() {
        val placed = listOf(
            PlacedTile(displayTile("br", PinnedTileSize.OneByOne, col = 1, row = 1), 1, 1),
            PlacedTile(displayTile("tl", PinnedTileSize.OneByOne, col = 0, row = 0), 0, 0),
        )
        // maxEnterDiagonal 2 → tapped step 3 → 3×40 + 280
        assertEquals(3L * 40L + 280L, tileExitWaveDurationMs(placed))
    }

    @Test
    fun tileExitStaggerDelay_scalesWithStepIndex() {
        assertEquals(0L, tileExitStaggerDelayMs(0))
        assertEquals(21L * 40L, tileExitStaggerDelayMs(21))
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
    fun ensureGridPositions_packsThreeMediumsAcrossWhenExpanded() {
        val entries = listOf(
            PinnedTileEntry("a", size = PinnedTileSize.TwoByTwo),
            PinnedTileEntry("b", size = PinnedTileSize.TwoByTwo),
            PinnedTileEntry("c", size = PinnedTileSize.TwoByTwo),
        )
        val positioned = ensureGridPositions(entries, columns = 6)
        assertEquals(0, positioned[0].gridCol)
        assertEquals(2, positioned[1].gridCol)
        assertEquals(4, positioned[2].gridCol)
        assertTrue(positioned.all { it.gridRow == 0 })
    }

    @Test
    fun adaptTilesToColumnCount_reflowsOverflowWhenShrinkingToFour() {
        val entries = listOf(
            PinnedTileEntry("a", size = PinnedTileSize.TwoByTwo, gridCol = 0, gridRow = 0),
            PinnedTileEntry("b", size = PinnedTileSize.TwoByTwo, gridCol = 2, gridRow = 0),
            PinnedTileEntry("c", size = PinnedTileSize.TwoByTwo, gridCol = 4, gridRow = 0),
        )
        val adapted = adaptTilesToColumnCount(entries, columns = 4)
        adapted.forEach { entry ->
            assertTrue(entry.gridCol!! + entry.size.colSpan <= 4)
        }
        assertEquals(3, adapted.size)
        assertEquals(
            setOf("a", "b", "c"),
            adapted.map { it.packageName }.toSet(),
        )
        // a and b keep; c was at col 4 and relocates.
        assertEquals(0, adapted.first { it.packageName == "a" }.gridCol)
        assertEquals(2, adapted.first { it.packageName == "b" }.gridCol)
    }

    @Test
    fun adaptTilesToColumnCount_keepsValidPositionsWhenExpanding() {
        val entries = listOf(
            PinnedTileEntry("a", size = PinnedTileSize.TwoByTwo, gridCol = 0, gridRow = 0),
            PinnedTileEntry("b", size = PinnedTileSize.TwoByTwo, gridCol = 2, gridRow = 0),
        )
        val adapted = adaptTilesToColumnCount(entries, columns = 6)
        assertEquals(0, adapted.first { it.packageName == "a" }.gridCol)
        assertEquals(2, adapted.first { it.packageName == "b" }.gridCol)
        assertTrue(adapted.all { it.gridRow == 0 })
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
    fun applyTileResize_alignsWhenGrowingSmallAtEdge() {
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
    fun applyTileResize_displacesOverlappingNeighbors() {
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
    fun applyTileResize_preservesUnrelatedGaps() {
        val entries = listOf(
            PinnedTileEntry("gap_anchor", size = PinnedTileSize.TwoByTwo, gridCol = 2, gridRow = 2),
            PinnedTileEntry("a", size = PinnedTileSize.OneByOne, gridCol = 0, gridRow = 0),
            PinnedTileEntry("b", size = PinnedTileSize.OneByOne, gridCol = 1, gridRow = 0),
        )
        val resized = applyTileResize(entries, "a", "primary", PinnedTileSize.TwoByTwo)
        val anchor = resized.first { it.packageName == "gap_anchor" }
        assertEquals(2, anchor.gridCol)
        assertEquals(2, anchor.gridRow)
    }

    @Test
    fun dragLayout_dropSmallOntoRightMediumDisplaces() {
        val left = displayTile("left", PinnedTileSize.TwoByTwo, col = 0, row = 0)
        val right = displayTile("right", PinnedTileSize.TwoByTwo, col = 2, row = 0)
        val below = displayTile("below", PinnedTileSize.TwoByTwo, col = 0, row = 2)
        val small = displayTile("small", PinnedTileSize.OneByOne, col = 2, row = 2)
        val tiles = listOf(left, right, below, small)
        val baseline = tiles.associate { it.tileKey() to (it.entry.gridCol!! to it.entry.gridRow!!) }
        // Drop small onto the right medium at (2,0).
        val placed = layoutTilesForDrag(tiles, small, slotCol = 2, slotRow = 0, baseline)
        val s = placed.first { it.tile.entry.packageName == "small" }
        assertEquals(2, s.col)
        assertEquals(0, s.row)
        val r = placed.first { it.tile.entry.packageName == "right" }
        // Left occupied → keep right column and push to the next band (not fling away).
        assertEquals(2, r.col)
        assertEquals(2, r.row)
        // below was already at row 2 on the left — shares the band with pushed right.
        assertEquals(0, placed.first { it.tile.entry.packageName == "below" }.col)
        assertEquals(2, placed.first { it.tile.entry.packageName == "below" }.row)
        assertEquals(0, placed.first { it.tile.entry.packageName == "left" }.col)
        assertEquals(0, placed.first { it.tile.entry.packageName == "left" }.row)
    }

    @Test
    fun dragLayout_rightEdgeInsertsRowWhenBelowBlocked() {
        val left = displayTile("left", PinnedTileSize.TwoByTwo, col = 0, row = 0)
        val right = displayTile("right", PinnedTileSize.TwoByTwo, col = 2, row = 0)
        // Occupy the natural landing band under the right medium.
        val block = displayTile("block", PinnedTileSize.TwoByTwo, col = 2, row = 2)
        val lower = displayTile("lower", PinnedTileSize.TwoByTwo, col = 0, row = 4)
        val small = displayTile("small", PinnedTileSize.OneByOne, col = 0, row = 2)
        val tiles = listOf(left, right, block, lower, small)
        val baseline = tiles.associate { it.tileKey() to (it.entry.gridCol!! to it.entry.gridRow!!) }
        val placed = layoutTilesForDrag(tiles, small, slotCol = 2, slotRow = 0, baseline)
        val r = placed.first { it.tile.entry.packageName == "right" }
        assertEquals(2, r.col)
        assertEquals(2, r.row)
        // block was at (2,2) → shifted down by inserted band.
        assertEquals(2, placed.first { it.tile.entry.packageName == "block" }.col)
        assertEquals(4, placed.first { it.tile.entry.packageName == "block" }.row)
        assertEquals(6, placed.first { it.tile.entry.packageName == "lower" }.row)
    }

    @Test
    fun dragLayout_rightEdgePrefersSameRowGapWhenFree() {
        val right = displayTile("right", PinnedTileSize.TwoByTwo, col = 2, row = 0)
        val small = displayTile("small", PinnedTileSize.OneByOne, col = 0, row = 2)
        val tiles = listOf(right, small)
        val baseline = tiles.associate { it.tileKey() to (it.entry.gridCol!! to it.entry.gridRow!!) }
        val placed = layoutTilesForDrag(tiles, small, slotCol = 2, slotRow = 0, baseline)
        // Left of the band is empty → medium slides horizontally, no row insert.
        assertEquals(0, placed.first { it.tile.entry.packageName == "right" }.col)
        assertEquals(0, placed.first { it.tile.entry.packageName == "right" }.row)
        assertEquals(2, placed.first { it.tile.entry.packageName == "small" }.col)
        assertEquals(0, placed.first { it.tile.entry.packageName == "small" }.row)
    }

    @Test
    fun dragLayout_moveIntoEmptyRightLeavesSourceGap() {
        val a = displayTile("a", PinnedTileSize.TwoByTwo, col = 0, row = 0)
        val b = displayTile("b", PinnedTileSize.TwoByTwo, col = 0, row = 2)
        val tiles = listOf(a, b)
        val baseline = tiles.associate { it.tileKey() to (it.entry.gridCol!! to it.entry.gridRow!!) }
        val placed = layoutTilesForDrag(tiles, a, slotCol = 2, slotRow = 0, baseline)
        assertEquals(2, placed.first { it.tile.entry.packageName == "a" }.col)
        assertEquals(0, placed.first { it.tile.entry.packageName == "a" }.row)
        // b stays; left of row 0 is now empty (intentional gap).
        assertEquals(0, placed.first { it.tile.entry.packageName == "b" }.col)
        assertEquals(2, placed.first { it.tile.entry.packageName == "b" }.row)
        assertTrue(placed.none { it.col == 0 && it.row == 0 })
    }

    @Test
    fun dragLayout_wideOntoTwoMediumsDisplacesBoth() {
        val a = displayTile("a", PinnedTileSize.TwoByTwo, col = 0, row = 0)
        val b = displayTile("b", PinnedTileSize.TwoByTwo, col = 2, row = 0)
        val wide = displayTile("w", PinnedTileSize.FourByTwo, col = 0, row = 2)
        val tiles = listOf(a, b, wide)
        val baseline = tiles.associate { it.tileKey() to (it.entry.gridCol!! to it.entry.gridRow!!) }
        val placed = layoutTilesForDrag(tiles, wide, slotCol = 0, slotRow = 0, baseline)
        val w = placed.first { it.tile.entry.packageName == "w" }
        assertEquals(0, w.col)
        assertEquals(0, w.row)
        for (other in placed.filter { it.tile.entry.packageName != "w" }) {
            assertFalse(
                tileOverlapsRegion(
                    other.col, other.row, other.tile.entry.size.colSpan, other.tile.entry.size.rowSpan,
                    w.col, w.row, 4, 2,
                ),
            )
        }
    }

    @Test
    fun packTilesInReadingOrder_wrapsWidePastOccupiedBand() {
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
        // Wide cannot share row 2 with c → falls below.
        assertEquals(0, placed[3].col)
        assertEquals(3, placed[3].row)
    }

    @Test
    fun dragLayout_mediumBreaksSmallGroupWhenRightBlocked() {
        val left = displayTile("left", PinnedTileSize.TwoByTwo, col = 0, row = 0)
        val s1 = displayTile("s1", PinnedTileSize.OneByOne, col = 2, row = 0)
        val s2 = displayTile("s2", PinnedTileSize.OneByOne, col = 3, row = 0)
        val s3 = displayTile("s3", PinnedTileSize.OneByOne, col = 2, row = 1)
        val s4 = displayTile("s4", PinnedTileSize.OneByOne, col = 3, row = 1)
        val medium = displayTile("med", PinnedTileSize.TwoByTwo, col = 0, row = 2)
        val tiles = listOf(left, s1, s2, s3, s4, medium)
        val baseline = tiles.associate { it.tileKey() to (it.entry.gridCol!! to it.entry.gridRow!!) }
        val placed = layoutTilesForDrag(tiles, medium, slotCol = 2, slotRow = 0, baseline)
        val m = placed.first { it.tile.entry.packageName == "med" }
        assertEquals(2, m.col)
        assertEquals(0, m.row)
        // Left stays; all four smalls move below as one packed group (not flung away).
        assertEquals(0, placed.first { it.tile.entry.packageName == "left" }.col)
        assertEquals(0, placed.first { it.tile.entry.packageName == "left" }.row)
        val smalls = placed.filter { it.tile.entry.packageName.startsWith("s") }
        assertEquals(4, smalls.size)
        assertTrue(smalls.all { it.row >= 2 })
        for (s in smalls) {
            assertFalse(
                tileOverlapsRegion(
                    s.col, s.row, 1, 1,
                    m.col, m.row, 2, 2,
                ),
            )
        }
        // Packed contiguously under the medium, keeping the group's right-side columns.
        val smallCells = smalls.map { it.col to it.row }.toSet()
        assertEquals(
            setOf(2 to 2, 3 to 2, 2 to 3, 3 to 3),
            smallCells,
        )
    }

    @Test
    fun dragLayout_mediumBreaksLeftSmallGroupSlidingRight() {
        val s1 = displayTile("s1", PinnedTileSize.OneByOne, col = 0, row = 0)
        val s2 = displayTile("s2", PinnedTileSize.OneByOne, col = 1, row = 0)
        val s3 = displayTile("s3", PinnedTileSize.OneByOne, col = 0, row = 1)
        val s4 = displayTile("s4", PinnedTileSize.OneByOne, col = 1, row = 1)
        val medium = displayTile("med", PinnedTileSize.TwoByTwo, col = 0, row = 2)
        val tiles = listOf(s1, s2, s3, s4, medium)
        val baseline = tiles.associate { it.tileKey() to (it.entry.gridCol!! to it.entry.gridRow!!) }
        val placed = layoutTilesForDrag(tiles, medium, slotCol = 0, slotRow = 0, baseline)
        assertEquals(0, placed.first { it.tile.entry.packageName == "med" }.col)
        assertEquals(0, placed.first { it.tile.entry.packageName == "med" }.row)
        // Right half of the band was empty → smalls slide horizontally onto cols 2–3.
        val smalls = placed.filter { it.tile.entry.packageName.startsWith("s") }
        assertTrue(smalls.all { it.col >= 2 && it.row <= 1 })
    }

    @Test
    fun snapDragSlotWithHysteresis_mediumUsesHalfStepOfTwo() {
        // Mediums may sit on odd columns; hysteresis still uses 1-cell steps.
        val (held, _) = snapDragSlotWithHysteresis(
            pointerCol = 1.6f, // ideal top-left = 0.6 → raw 1, still short of 0.78 boundary
            pointerRow = 1f,
            colSpan = 2,
            rowSpan = 2,
            currentCol = 0,
            currentRow = 0,
            hysteresis = 0.28f,
        )
        assertEquals(0, held)
        val (committed, _) = snapDragSlotWithHysteresis(
            pointerCol = 1.9f, // ideal = 0.9 → past 0.78 → column 1
            pointerRow = 1f,
            colSpan = 2,
            rowSpan = 2,
            currentCol = 0,
            currentRow = 0,
            hysteresis = 0.28f,
        )
        assertEquals(1, committed)
    }

    @Test
    fun snapDragSlot_allowsMediumOnOddColumn() {
        // Center over cols 1–2 → medium top-left at column 1 (middle of 4-col Start).
        val (col, row) = snapDragSlot(
            pointerCol = 2.0f,
            pointerRow = 1f,
            colSpan = 2,
            rowSpan = 2,
            columns = 4,
        )
        assertEquals(1, col)
        assertEquals(0, row)
    }

    @Test
    fun layout_allowsMediumBetweenStackedSmallColumns() {
        // Valid WP8.1 config: 1×1 | 2×2 | 1×1 (smalls stacked on each side).
        val tiles = listOf(
            displayTile("tl", PinnedTileSize.OneByOne, col = 0, row = 0),
            displayTile("bl", PinnedTileSize.OneByOne, col = 0, row = 1),
            displayTile("mid", PinnedTileSize.TwoByTwo, col = 1, row = 0),
            displayTile("tr", PinnedTileSize.OneByOne, col = 3, row = 0),
            displayTile("br", PinnedTileSize.OneByOne, col = 3, row = 1),
        )
        val placed = layoutTilesOnGrid(tiles)
        assertEquals(1, placed.first { it.tile.entry.packageName == "mid" }.col)
        assertEquals(0, placed.first { it.tile.entry.packageName == "tl" }.col)
        assertEquals(3, placed.first { it.tile.entry.packageName == "tr" }.col)
        // Resize/ensure must not shove the medium back to an even column.
        val entries = tiles.map { it.entry }
        val kept = ensureGridPositions(entries)
        assertEquals(1, kept.first { it.packageName == "mid" }.gridCol)
    }

    @Test
    fun dragLayout_canSeatMediumInMiddleBetweenSmalls() {
        val tl = displayTile("tl", PinnedTileSize.OneByOne, col = 0, row = 0)
        val bl = displayTile("bl", PinnedTileSize.OneByOne, col = 0, row = 1)
        val tr = displayTile("tr", PinnedTileSize.OneByOne, col = 3, row = 0)
        val br = displayTile("br", PinnedTileSize.OneByOne, col = 3, row = 1)
        val medium = displayTile("mid", PinnedTileSize.TwoByTwo, col = 0, row = 2)
        val tiles = listOf(tl, bl, tr, br, medium)
        val baseline = tiles.associate { it.tileKey() to (it.entry.gridCol!! to it.entry.gridRow!!) }
        val placed = layoutTilesForDrag(tiles, medium, slotCol = 1, slotRow = 0, baseline)
        val mid = placed.first { it.tile.entry.packageName == "mid" }
        assertEquals(1, mid.col)
        assertEquals(0, mid.row)
        // Side smalls stay put — medium fits the hole between them.
        assertEquals(0, placed.first { it.tile.entry.packageName == "tl" }.col)
        assertEquals(3, placed.first { it.tile.entry.packageName == "tr" }.col)
        assertEquals(0, placed.first { it.tile.entry.packageName == "tl" }.row)
        assertEquals(0, placed.first { it.tile.entry.packageName == "tr" }.row)
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
