package com.metro.people.tiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.metro.people.data.PersonSummary
import com.metro.system.MetroTileContract

@RunWith(RobolectricTestRunner::class)
class PeopleTileLogicTest {
    private val accent = "#1BA1E2"

    @Test
    fun fallbackCells_producesRequestedCount() {
        val cells = PeopleTileLogic.fallbackCells(9, accent)
        assertEquals(9, cells.size)
        assertEquals(PeopleTileLogic.colorForIndex(0, accent), cells.first().colorHex)
    }

    @Test
    fun accentShades_stayWithinSameHueFamily() {
        val shades = PeopleTileLogic.accentShades(accent, 9)
        assertEquals(9, shades.size)
        shades.forEach { hex ->
            assertTrue(hex.startsWith("#"))
            assertTrue(hex.length == 7)
        }
    }

    @Test
    fun cellsFromContacts_usesPhotoUriWhenAvailable() {
        val contacts = listOf(
            person(id = 1L, photoUri = "content://contacts/1"),
            person(id = 2L, photoUri = null),
        )
        val cells = PeopleTileLogic.cellsFromContacts(contacts, "com.metro.people.tiles", accent)
        assertEquals(MetroTileContract.MAX_PHOTO_GRID_CELLS, cells.size)
        assertEquals("content://com.metro.people.tiles/photo/1", cells[0].imageUri)
        assertNull(cells[1].imageUri)
        assertEquals(PeopleTileLogic.colorForContact(2L, accent), cells[1].colorHex)
    }

    @Test
    fun cellsFromContacts_padsToMaxCells() {
        val contacts = listOf(person(id = 5L, photoUri = null))
        val cells = PeopleTileLogic.cellsFromContacts(contacts, "com.metro.people.tiles", accent)
        assertEquals(MetroTileContract.MAX_PHOTO_GRID_CELLS, cells.size)
    }

    private fun person(id: Long, photoUri: String?) = PersonSummary(
        id = id,
        displayName = "Test $id",
        photoUri = photoUri,
        hasPhone = true,
        defaultPhone = "555",
        defaultEmail = null,
        sourceLabel = "device",
        sortKey = 'T',
    )
}
