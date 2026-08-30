package com.metro.launcher

import com.metro.launcher.data.GalleryLiveTileLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryLiveTileTest {
    @Test
    fun gridFromUris_isCycleAndKeepsPhotoCells() {
        val grid = GalleryLiveTileLogic.gridFromUris(
            imageUris = listOf(
                "content://media/external/images/media/1",
                "content://media/external/images/media/2",
            ),
            accentHex = "#1BA1E2",
        )
        assertTrue(grid.cycle)
        assertEquals(2, grid.cells.size)
        assertTrue(grid.cells.all { !it.imageUri.isNullOrBlank() })
    }

    @Test
    fun gridFromUris_emptyFallsBackToAccentCells() {
        val grid = GalleryLiveTileLogic.gridFromUris(emptyList(), "#E51400")
        assertTrue(grid.cycle)
        assertTrue(grid.hasContent)
        assertTrue(grid.cells.all { it.imageUri.isNullOrBlank() && !it.colorHex.isNullOrBlank() })
    }
}
