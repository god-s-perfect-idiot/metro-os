package com.metro.music

import com.metro.music.data.ArtworkUrls
import com.metro.music.data.LibraryLogic
import com.metro.music.data.LibrarySource
import com.metro.music.data.Playlist
import com.metro.music.data.ShowingFilter
import com.metro.music.data.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryLogicTest {
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
    private val yt = Song(
        id = "yt:1",
        title = "B",
        artist = "Beta",
        album = "Two",
        durationMs = 120_000,
        uri = null,
        artworkUri = null,
        source = LibrarySource.YouTubeMusic,
        youtubeVideoId = "abc",
    )

    @Test
    fun filterSongs_respectsShowing() {
        val all = listOf(local, yt)
        assertEquals(2, LibraryLogic.filterSongs(all, ShowingFilter.All).size)
        assertEquals(listOf(local), LibraryLogic.filterSongs(all, ShowingFilter.OnDevice))
        assertEquals(listOf(yt), LibraryLogic.filterSongs(all, ShowingFilter.YouTubeMusic))
    }

    @Test
    fun filterPlaylists_respectsShowing() {
        val localPl = Playlist("local-pl:1", "On device", 3, LibrarySource.Local, localMediaStoreId = 1L)
        val ytPl = Playlist("yt-pl:LM", "Liked music", 12, LibrarySource.YouTubeMusic, youtubePlaylistId = "LM")
        val all = listOf(localPl, ytPl)
        assertEquals(2, LibraryLogic.filterPlaylists(all, ShowingFilter.All).size)
        assertEquals(listOf(localPl), LibraryLogic.filterPlaylists(all, ShowingFilter.OnDevice))
        assertEquals(listOf(ytPl), LibraryLogic.filterPlaylists(all, ShowingFilter.YouTubeMusic))
    }

    @Test
    fun artistsFrom_groupsCaseInsensitive() {
        val songs = listOf(
            local,
            local.copy(id = "local:2", title = "A2"),
            yt,
        )
        val artists = LibraryLogic.artistsFrom(songs)
        assertEquals(2, artists.size)
        assertTrue(artists.any { it.name == "Alpha" && it.songCount == 2 })
    }

    @Test
    fun formatDuration_andRemaining() {
        assertEquals("1:30", LibraryLogic.formatDuration(90_000))
        assertEquals("-0:30", LibraryLogic.formatRemaining(60_000, 90_000))
    }

    @Test
    fun groupByJumpKey_putsHashFirstAndSortsSections() {
        val titles = listOf("Zebra", "alpha", "123 Go", "Beta")
        val grouped = LibraryLogic.groupByJumpKey(titles) { it }

        assertEquals(listOf('#', 'a', 'b', 'z'), grouped.keys.toList())
        assertEquals(listOf("123 Go"), grouped['#'])
    }

    @Test
    fun groupByJumpKey_ordersRowsAlphabeticallyWithinSection() {
        val songs = listOf(yt.copy(id = "yt:2", title = "Blue"), local.copy(title = "Amber"))
        val grouped = LibraryLogic.groupByJumpKey(songs) { it.title }

        assertEquals(listOf("Amber"), grouped['a']?.map { it.title })
        assertEquals(listOf("Blue"), grouped['b']?.map { it.title })
    }

    @Test
    fun highRes_rewritesInnertubeThumbnailSize() {
        assertEquals(
            "https://lh3.googleusercontent.com/abc=w1024-h1024-l90-rj",
            ArtworkUrls.highRes("https://lh3.googleusercontent.com/abc=w60-h60-l90-rj"),
        )
        assertEquals(
            "https://i.ytimg.com/vi/abc/hqdefault.jpg",
            ArtworkUrls.highRes("https://i.ytimg.com/vi/abc/hqdefault.jpg"),
        )
    }

    @Test
    fun showingLabel_matchesWpCopy() {
        assertEquals("showing all music", LibraryLogic.showingLabel(ShowingFilter.All))
        assertEquals("showing on this device", LibraryLogic.showingLabel(ShowingFilter.OnDevice))
        assertEquals("showing youtube music", LibraryLogic.showingLabel(ShowingFilter.YouTubeMusic))
    }
}
