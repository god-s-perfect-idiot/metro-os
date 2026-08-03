package com.metro.people.tiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.metro.people.data.PersonSummary
import com.metro.system.MetroTileContract
import kotlin.random.Random

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
    fun cellsFromContacts_assignsUriAndLabelForEveryContact() {
        val contacts = listOf(
            person(id = 1L, photoUri = "content://contacts/1", name = "Alice"),
            person(id = 2L, photoUri = null, name = "Bob"),
        )
        val cells = PeopleTileLogic.cellsFromContacts(
            contacts,
            "com.metro.people.tiles",
            accent,
            random = Random(0),
        )
        val pool = cells.filter { !it.imageUri.isNullOrBlank() }
        assertEquals(2, pool.size)
        assertTrue(pool.any { it.imageUri!!.endsWith("/1") && it.label == "A" })
        assertTrue(pool.any { it.imageUri!!.endsWith("/2") && it.label == "B" })
        assertTrue(cells.size > pool.size)
    }

    @Test
    fun cellsFromContacts_padsWithAccentTemplates() {
        val contacts = listOf(person(id = 5L, photoUri = null, name = "Eve"))
        val cells = PeopleTileLogic.cellsFromContacts(
            contacts,
            "com.metro.people.tiles",
            accent,
            random = Random(0),
        )
        assertEquals(1 + MetroTileContract.MAX_PHOTO_GRID_CELLS, cells.size)
        assertEquals("content://com.metro.people.tiles/photo/5", cells[0].imageUri)
        assertEquals("E", cells[0].label)
        assertNull(cells.last().imageUri)
        assertNull(cells.last().label)
    }

    @Test
    fun cellsFromContacts_prefersPhotoContactsFirst() {
        val contacts = listOf(
            person(id = 1L, photoUri = null, name = "Ann"),
            person(id = 2L, photoUri = "content://contacts/2", name = "Bea"),
            person(id = 3L, photoUri = null, name = "Cal"),
            person(id = 4L, photoUri = "content://contacts/4", name = "Dee"),
        )
        val cells = PeopleTileLogic.cellsFromContacts(
            contacts,
            "com.metro.people.tiles",
            accent,
            random = Random(1),
        )
        val pool = cells.filter { !it.imageUri.isNullOrBlank() }
        assertTrue(pool[0].imageUri!!.endsWith("/2") || pool[0].imageUri!!.endsWith("/4"))
        assertTrue(pool[1].imageUri!!.endsWith("/2") || pool[1].imageUri!!.endsWith("/4"))
        assertTrue(pool[0].imageUri != pool[1].imageUri)
    }

    @Test
    fun cellsFromContacts_capsPoolSize() {
        val contacts = (1L..60L).map { person(id = it, photoUri = "content://c/$it", name = "N$it") }
        val cells = PeopleTileLogic.cellsFromContacts(
            contacts,
            "com.metro.people.tiles",
            accent,
            random = Random(2),
        )
        val pool = cells.filter { !it.imageUri.isNullOrBlank() }
        assertEquals(PeopleTileLogic.LIVE_POOL_SIZE, pool.size)
    }

    @Test
    fun tileLabel_usesFirstLetter() {
        assertEquals("S", PeopleTileLogic.tileLabel("samar"))
        assertEquals("7", PeopleTileLogic.tileLabel("7-eleven"))
        assertEquals("#", PeopleTileLogic.tileLabel("   "))
    }

    @Test
    fun contactTileId_roundTrips() {
        assertEquals("contact:42", PeopleTileLogic.contactTileId(42L))
        assertEquals(42L, PeopleTileLogic.parseContactTileId("contact:42"))
        assertNull(PeopleTileLogic.parseContactTileId("primary"))
        assertNull(PeopleTileLogic.parseContactTileId("contact:abc"))
    }

    @Test
    fun contactDeepLink_roundTrips() {
        val uri = android.net.Uri.parse(PeopleTileLogic.contactDeepLinkUri(7L))
        assertEquals(7L, PeopleTileLogic.parseContactDeepLink(uri))
        assertNull(PeopleTileLogic.parseContactDeepLink(android.net.Uri.parse("metro://other/contact/7")))
    }

    private fun person(id: Long, photoUri: String?, name: String = "Test $id") = PersonSummary(
        id = id,
        displayName = name,
        photoUri = photoUri,
        hasPhone = true,
        defaultPhone = "555",
        defaultEmail = null,
        sourceLabel = "device",
        sortKey = name.first().uppercaseChar(),
    )
}
