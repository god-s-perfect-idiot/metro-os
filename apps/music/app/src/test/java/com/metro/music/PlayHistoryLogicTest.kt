package com.metro.music

import com.metro.music.data.LibrarySource
import com.metro.music.data.PlayHistoryEntry
import com.metro.music.data.PlayHistoryLogic
import com.metro.music.data.PlayHistoryStore
import com.metro.music.data.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class PlayHistoryLogicTest {
    private val local = Song(
        id = "local:1",
        title = "A",
        artist = "Alpha",
        album = "One",
        durationMs = 90_000,
        uri = null,
        artworkUri = null,
        source = LibrarySource.Local,
    )
    private val other = local.copy(id = "local:2", title = "B")

    @Test
    fun record_prependsAndDedupesById() {
        val first = PlayHistoryLogic.record(local, 1_000L, emptyList())
        val second = PlayHistoryLogic.record(other, 2_000L, first)
        val again = PlayHistoryLogic.record(local, 3_000L, second)
        assertEquals(listOf("local:1", "local:2"), again.map { it.songId })
        assertEquals(3_000L, again.first().playedAtMs)
    }

    @Test
    fun record_capsLength() {
        var entries = emptyList<PlayHistoryEntry>()
        repeat(PlayHistoryLogic.MaxEntries + 5) { i ->
            entries = PlayHistoryLogic.record(
                local.copy(id = "local:$i", title = "T$i"),
                i.toLong(),
                entries,
            )
        }
        assertEquals(PlayHistoryLogic.MaxEntries, entries.size)
        assertEquals("local:${PlayHistoryLogic.MaxEntries + 4}", entries.first().songId)
    }

    @Test
    fun resolveSongs_prefersLibrary() {
        val snapshot = PlayHistoryEntry.from(local, 1L)
        val live = local.copy(title = "Live title")
        val resolved = PlayHistoryLogic.resolveSongs(listOf(snapshot), listOf(live))
        assertEquals("Live title", resolved.single().title)
    }

    @Test
    fun resolveSongs_fallsBackToSnapshot() {
        val snapshot = PlayHistoryEntry.from(local, 1L)
        val resolved = PlayHistoryLogic.resolveSongs(listOf(snapshot), emptyList())
        assertEquals(local, resolved.single())
    }

    @Test
    fun encodeDecode_roundTrips() {
        val entries = PlayHistoryLogic.record(local, 42L, emptyList())
        val raw = PlayHistoryStore.encode(entries)
        val decoded = PlayHistoryStore.decode(raw)
        assertEquals(entries, decoded)
        assertTrue(raw.contains("local:1"))
    }
}
