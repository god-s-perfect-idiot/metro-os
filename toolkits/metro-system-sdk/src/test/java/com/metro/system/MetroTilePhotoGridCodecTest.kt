package com.metro.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MetroTilePhotoGridCodecTest {
    @Test
    fun encodeDecode_roundTripsCells() {
        val grid = MetroTilePhotoGrid(
            cells = listOf(
                MetroTileGridCell(colorHex = "#E86B4A"),
                MetroTileGridCell(imageUri = "content://com.metro.people.tiles/photo/42"),
                MetroTileGridCell(colorHex = "#FFD54F", imageUri = "content://example/1"),
            ),
        )
        val encoded = MetroTilePhotoGridCodec.encode(grid)
        assertNotNull(encoded)
        val decoded = MetroTilePhotoGridCodec.decode(encoded)
        assertNotNull(decoded)
        assertEquals(3, decoded!!.cells.size)
        assertEquals("#E86B4A", decoded.cells[0].colorHex)
        assertEquals("content://com.metro.people.tiles/photo/42", decoded.cells[1].imageUri)
        assertEquals("#FFD54F", decoded.cells[2].colorHex)
    }

    @Test
    fun decode_invalidJsonReturnsNull() {
        assertNull(MetroTilePhotoGridCodec.decode("not-json"))
    }

    @Test
    fun photoGrid_cellsForPadsShortLists() {
        val grid = MetroTilePhotoGrid(
            cells = listOf(MetroTileGridCell(colorHex = "#FF0000")),
        )
        val cells = grid.cellsFor(3, 3)
        assertEquals(9, cells.size)
        assertEquals("#FF0000", cells.first().colorHex)
    }

    @Test
    fun photoGridDimensions_matchesLauncherSizes() {
        assertEquals(3 to 3, MetroTileContract.photoGridDimensions(2, 2))
        assertEquals(6 to 3, MetroTileContract.photoGridDimensions(4, 2))
        assertNull(MetroTileContract.photoGridDimensions(1, 1))
    }

    @Test
    fun tileData_hasPhotoGridWhenCellsPresent() {
        val data = MetroTileData(
            title = "People",
            backgroundColorHex = "#0078D7",
            photoGrid = MetroTilePhotoGrid(listOf(MetroTileGridCell(colorHex = "#E86B4A"))),
        )
        assertTrue(data.hasPhotoGrid)
    }
}
