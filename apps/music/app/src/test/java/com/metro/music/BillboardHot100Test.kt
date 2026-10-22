package com.metro.music

import com.metro.music.data.LibrarySource
import com.metro.music.data.Song
import com.metro.music.ytmusic.BillboardHot100
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BillboardHot100Test {
    @Test
    fun parseChart_readsRankTitleArtistInOrder() {
        val root = JSONObject()
            .put("date", "2026-10-24")
            .put(
                "data",
                JSONArray()
                    .put(entry(2, "Second", "Artist B"))
                    .put(entry(1, "First", "Artist A"))
                    .put(entry(3, "Third", "Artist C")),
            )
        val chart = BillboardHot100.parseChart(root, limit = 100)
        assertEquals(3, chart.size)
        assertEquals(1, chart[0].rank)
        assertEquals("First", chart[0].title)
        assertEquals("Artist A", chart[0].artist)
        assertEquals("Second", chart[1].title)
        assertEquals("Third", chart[2].title)
    }

    @Test
    fun parseChart_respectsLimit() {
        val data = JSONArray()
        for (i in 1..5) data.put(entry(i, "Song $i", "Artist $i"))
        val chart = BillboardHot100.parseChart(JSONObject().put("data", data), limit = 3)
        assertEquals(3, chart.size)
        assertEquals(3, chart.last().rank)
    }

    @Test
    fun artistMatches_handlesFeaturingVariants() {
        assertTrue(BillboardHot100.artistMatches("Drake", "Drake Featuring Don Toliver"))
        assertTrue(BillboardHot100.artistMatches("Tame Impala & JENNIE", "Tame Impala"))
        assertFalse(BillboardHot100.artistMatches("Taylor Swift", "Olivia Rodrigo"))
    }

    @Test
    fun resolveSongs_keepsRankOrderAndSkipsMisses() {
        val entries = listOf(
            BillboardHot100.ChartEntry(1, "A", "Artist A"),
            BillboardHot100.ChartEntry(2, "B", "Artist B"),
            BillboardHot100.ChartEntry(3, "C", "Artist C"),
        )
        val songs = BillboardHot100.resolveSongs(entries) { query, _ ->
            when {
                query.startsWith("B ") -> emptyList()
                else -> listOf(
                    Song(
                        id = "yt:${query.take(1)}",
                        title = query.substringBefore(' '),
                        artist = query.substringAfter(' '),
                        album = "",
                        durationMs = 0L,
                        uri = null,
                        artworkUri = null,
                        source = LibrarySource.YouTubeMusic,
                        youtubeVideoId = query.take(1),
                    ),
                )
            }
        }
        assertEquals(2, songs.size)
        assertEquals("A", songs[0].title)
        assertEquals("C", songs[1].title)
        assertTrue(songs[0].id.startsWith("bb:1:"))
        assertTrue(songs[1].id.startsWith("bb:3:"))
    }

    private fun entry(rank: Int, song: String, artist: String): JSONObject =
        JSONObject()
            .put("song", song)
            .put("artist", artist)
            .put("this_week", rank)
}
