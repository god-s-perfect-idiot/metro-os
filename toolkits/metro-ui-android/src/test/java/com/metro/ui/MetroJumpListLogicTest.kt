package com.metro.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetroJumpListLogicTest {
    @Test
    fun letterKeys_areHashThenAToZ() {
        assertEquals(27, MetroJumpListLogic.LetterKeys.size)
        assertEquals('#', MetroJumpListLogic.LetterKeys.first())
        assertEquals('a', MetroJumpListLogic.LetterKeys[1])
        assertEquals('z', MetroJumpListLogic.LetterKeys.last())
    }

    @Test
    fun sortKey_mapsNonLettersToHash() {
        assertEquals('#', MetroJumpListLogic.sortKey("911 Scanner"))
        assertEquals('#', MetroJumpListLogic.sortKey("  "))
        assertEquals('#', MetroJumpListLogic.sortKey(""))
        assertEquals('a', MetroJumpListLogic.sortKey("Amazon"))
        assertEquals('z', MetroJumpListLogic.sortKey("Zoom"))
    }

    @Test
    fun normalize_lowercasesLetters() {
        assertEquals('b', MetroJumpListLogic.normalize('B'))
        assertEquals('#', MetroJumpListLogic.normalize('7'))
        assertEquals('#', MetroJumpListLogic.normalize('#'))
    }

    @Test
    fun activeLetters_fromLabels() {
        val active = MetroJumpListLogic.activeLetters(
            listOf("Amazon", "Audible", "Bing", "911"),
        )
        assertTrue(active.contains('a'))
        assertTrue(active.contains('b'))
        assertTrue(active.contains('#'))
        assertFalse(active.contains('c'))
    }

    @Test
    fun isActive_isCaseInsensitive() {
        val active = setOf('A', '#')
        assertTrue(MetroJumpListLogic.isActive('a', active))
        assertTrue(MetroJumpListLogic.isActive('A', active))
        assertFalse(MetroJumpListLogic.isActive('z', active))
    }

    @Test
    fun grid_isFourBySevenIncludingLocale() {
        assertEquals(4, MetroJumpListLogic.GridColumns)
        assertEquals(28, MetroJumpListLogic.GridCellCount)
        assertEquals(
            MetroJumpListLogic.GridCellCount,
            MetroJumpListLogic.LetterKeys.size + 1,
        )
    }

    @Test
    fun showSectionMarkers_hiddenWhileSearchActive() {
        assertTrue(MetroJumpListLogic.showSectionMarkers(searchActive = false))
        assertFalse(MetroJumpListLogic.showSectionMarkers(searchActive = true))
    }
}
