package com.metro.music

import com.metro.music.ytmusic.YtMusicLibraryParsers
import com.metro.music.ytmusic.YtPlaylistSync
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class YtPlaylistParserTest {

    @Test
    fun parse_skipsNewPlaylistChipAndAlbums() {
        val page = gridBrowse(
            listOf(
                createPlaylistTile(),
                playlistTile("VLPL111", "Late night", "12 songs"),
                albumTile("MPREb_album", "Some Album"),
            ),
            continuation = "next-page",
        )

        val playlists = YtMusicLibraryParsers.parsePlaylists(page)
        assertEquals(listOf("Late night"), playlists.map { it.title })
        assertEquals("PL111", playlists[0].youtubePlaylistId)
        assertEquals(12, playlists[0].songCount)
        assertEquals("next-page", YtMusicLibraryParsers.continuationToken(page))
    }

    @Test
    fun parse_readsLikedMusicAndStripsVlPrefix() {
        val page = gridBrowse(listOf(playlistTile("VLLM", "Liked music", "247 songs")))
        val playlists = YtMusicLibraryParsers.parsePlaylists(page)
        assertEquals("LM", playlists.single().youtubePlaylistId)
        assertEquals("yt-pl:LM", playlists.single().id)
    }

    @Test
    fun playlistIdFromBrowseId_rejectsAlbumsArtistsAndRadio() {
        assertEquals("PLABC", YtMusicLibraryParsers.playlistIdFromBrowseId("VLPLABC"))
        assertNull(YtMusicLibraryParsers.playlistIdFromBrowseId("MPREb_x"))
        assertNull(YtMusicLibraryParsers.playlistIdFromBrowseId("UCchannel"))
        assertNull(YtMusicLibraryParsers.playlistIdFromBrowseId("RDAMPL123"))
        assertNull(YtMusicLibraryParsers.playlistIdFromBrowseId("OLAK5uy_album"))
    }

    @Test
    fun collect_followsGridContinuationsUntilExhausted() {
        val pages = mapOf(
            "FEmusic_liked_playlists" to gridBrowse(
                listOf(
                    createPlaylistTile(),
                    playlistTile("VLPL1", "One", "1 song"),
                    playlistTile("VLPL2", "Two", "2 songs"),
                ),
                continuation = "page-2",
            ),
            "page-2" to continuationGrid(
                listOf(
                    playlistTile("VLPL3", "Three", "3 songs"),
                    playlistTile("VLPL4", "Four", "4 songs"),
                ),
            ),
        )

        val result = YtPlaylistSync.collect(
            fetchBrowse = { id -> pages[id] },
            fetchContinuation = { token -> pages[token] },
            browseIds = listOf("FEmusic_liked_playlists"),
        )

        assertNull(result.error)
        assertEquals(listOf("One", "Two", "Three", "Four"), result.playlists.map { it.title })
    }

    @Test
    fun collect_dedupesTheSamePlaylistFromBothBrowseIds() {
        val liked = gridBrowse(listOf(playlistTile("VLPL1", "Shared", "8 songs")))
        val library = gridBrowse(listOf(playlistTile("VLPL1", "Shared", "8 songs"), playlistTile("VLPL2", "Only library", "1 song")))

        val result = YtPlaylistSync.collect(
            fetchBrowse = { id -> if (id == "FEmusic_liked_playlists") liked else library },
            fetchContinuation = { null },
        )

        assertEquals(listOf("Shared", "Only library"), result.playlists.map { it.title })
    }

    @Test
    fun collect_returnsBrowseErrorWhenEmpty() {
        val result = YtPlaylistSync.collect(
            fetchBrowse = { null },
            fetchContinuation = { null },
            browseIds = listOf("FEmusic_liked_playlists"),
        )
        assertTrue(result.playlists.isEmpty())
        assertEquals("Browse failed (FEmusic_liked_playlists)", result.error)
    }

    private fun playlistTile(browseId: String, title: String, count: String): JSONObject =
        JSONObject().put(
            "musicTwoRowItemRenderer",
            JSONObject()
                .put("title", titleRun(title, browseId))
                .put("subtitle", runs("Playlist", " • ", count))
                .put(
                    "navigationEndpoint",
                    JSONObject().put(
                        "browseEndpoint",
                        JSONObject()
                            .put("browseId", browseId)
                            .put(
                                "browseEndpointContextSupportedConfigs",
                                JSONObject().put(
                                    "browseEndpointContextMusicConfig",
                                    JSONObject().put("pageType", "MUSIC_PAGE_TYPE_PLAYLIST"),
                                ),
                            ),
                    ),
                ),
        )

    private fun albumTile(browseId: String, title: String): JSONObject =
        JSONObject().put(
            "musicTwoRowItemRenderer",
            JSONObject()
                .put("title", titleRun(title, browseId))
                .put(
                    "navigationEndpoint",
                    JSONObject().put(
                        "browseEndpoint",
                        JSONObject()
                            .put("browseId", browseId)
                            .put(
                                "browseEndpointContextSupportedConfigs",
                                JSONObject().put(
                                    "browseEndpointContextMusicConfig",
                                    JSONObject().put("pageType", "MUSIC_PAGE_TYPE_ALBUM"),
                                ),
                            ),
                    ),
                ),
        )

    private fun createPlaylistTile(): JSONObject =
        JSONObject().put(
            "musicTwoRowItemRenderer",
            JSONObject()
                .put("title", runs("New playlist"))
                .put(
                    "navigationEndpoint",
                    JSONObject().put("createPlaylistEndpoint", JSONObject()),
                ),
        )

    private fun runs(vararg texts: String): JSONObject =
        JSONObject().put(
            "runs",
            JSONArray().apply { texts.forEach { put(JSONObject().put("text", it)) } },
        )

    private fun titleRun(text: String, browseId: String): JSONObject =
        JSONObject().put(
            "runs",
            JSONArray().put(
                JSONObject()
                    .put("text", text)
                    .put(
                        "navigationEndpoint",
                        JSONObject().put("browseEndpoint", JSONObject().put("browseId", browseId)),
                    ),
            ),
        )

    private fun continuationItem(token: String): JSONObject = JSONObject().put(
        "continuationItemRenderer",
        JSONObject().put(
            "continuationEndpoint",
            JSONObject().put(
                "continuationCommand",
                JSONObject().put("token", token),
            ),
        ),
    )

    private fun gridBrowse(items: List<JSONObject>, continuation: String? = null): JSONObject {
        val gridItems = JSONArray()
        items.forEach { gridItems.put(it) }
        if (continuation != null) gridItems.put(continuationItem(continuation))
        return JSONObject().put(
            "contents",
            JSONObject().put(
                "singleColumnBrowseResultsRenderer",
                JSONObject().put(
                    "tabs",
                    JSONArray().put(
                        JSONObject().put(
                            "tabRenderer",
                            JSONObject().put(
                                "content",
                                JSONObject().put(
                                    "sectionListRenderer",
                                    JSONObject().put(
                                        "contents",
                                        JSONArray().put(
                                            JSONObject().put(
                                                "gridRenderer",
                                                JSONObject().put("items", gridItems),
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }

    private fun continuationGrid(items: List<JSONObject>): JSONObject {
        val gridItems = JSONArray()
        items.forEach { gridItems.put(it) }
        return JSONObject().put(
            "onResponseReceivedActions",
            JSONArray().put(
                JSONObject().put(
                    "appendContinuationItemsAction",
                    JSONObject().put("continuationItems", gridItems),
                ),
            ),
        )
    }
}
