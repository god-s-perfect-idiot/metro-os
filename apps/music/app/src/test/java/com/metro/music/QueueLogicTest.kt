package com.metro.music

import com.metro.music.data.LibrarySource
import com.metro.music.data.QueueLogic
import com.metro.music.data.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class QueueLogicTest {
    private val songs = listOf(
        song("a", "Alpha"),
        song("b", "Beta"),
        song("c", "Gamma"),
        song("d", "Delta"),
        song("e", "Epsilon"),
        song("f", "Zeta"),
        song("g", "Eta"),
        song("h", "Theta"),
    )

    @Test
    fun indexOfSong_findsCurrent() {
        assertEquals(1, QueueLogic.indexOfSong(songs, "b"))
        assertEquals(-1, QueueLogic.indexOfSong(songs, "missing"))
        assertEquals(-1, QueueLogic.indexOfSong(songs, null))
    }

    @Test
    fun upNext_returnsFollowingTitle() {
        assertEquals("Gamma", QueueLogic.upNext(songs, "b")?.title)
        assertNull(QueueLogic.upNext(songs, "h"))
        assertNull(QueueLogic.upNext(emptyList(), "a"))
    }

    @Test
    fun upNextLabel_formatsOrDash() {
        assertEquals("Up next: Beta", QueueLogic.upNextLabel(songs, "a"))
        assertEquals("Up next: —", QueueLogic.upNextLabel(songs, "h"))
        assertEquals("Up next: —", QueueLogic.upNextLabel(emptyList(), null))
    }

    @Test
    fun missingAhead_onlyUnresolvedWithinLookahead() {
        val materialised = setOf("a", "b", "c")
        val missing = QueueLogic.missingAhead(songs, currentIndex = 1, lookahead = 4, materialised)
        assertEquals(listOf("d", "e", "f"), missing.map { it.id })
    }

    @Test
    fun missingAhead_emptyWhenWindowFull() {
        val materialised = setOf("b", "c", "d", "e", "f", "g")
        val missing = QueueLogic.missingAhead(songs, currentIndex = 1, lookahead = 5, materialised)
        assertTrue(missing.isEmpty())
    }

    @Test
    fun missingBehind_onlyUnresolvedWithinLookbehind() {
        val materialised = setOf("d", "e")
        val missing = QueueLogic.missingBehind(songs, currentIndex = 4, lookbehind = 3, materialised)
        assertEquals(listOf("b", "c"), missing.map { it.id })
    }

    @Test
    fun shuffleKeepingCurrent_pinsCurrentAndPermutesRest() {
        val shuffled = QueueLogic.shuffleKeepingCurrent(songs, "c", Random(42))
        assertEquals("c", shuffled.first().id)
        assertEquals(songs.map { it.id }.toSet(), shuffled.map { it.id }.toSet())
        assertTrue(shuffled.drop(1) != songs.filter { it.id != "c" })
    }

    private fun song(id: String, title: String) = Song(
        id = id,
        title = title,
        artist = "Artist",
        album = "Album",
        durationMs = 60_000,
        uri = null,
        artworkUri = null,
        source = LibrarySource.Local,
    )
}
