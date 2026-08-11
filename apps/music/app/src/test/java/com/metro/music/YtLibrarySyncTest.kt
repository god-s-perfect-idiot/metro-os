package com.metro.music

import com.metro.music.ytmusic.YtBrowseParser
import com.metro.music.ytmusic.YtLibrarySync
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
class YtLibrarySyncTest {

    @Test
    fun parse_readsPlaylistShelfAndModernContinuationToken() {
        val page = YtBrowseParser.parse(playlistBrowse(listOf("a", "b"), continuation = "tok-2"))

        assertEquals(listOf("a", "b"), page.songs.map { it.youtubeVideoId })
        assertEquals("tok-2", page.continuation)
    }

    @Test
    fun parse_readsContinuationPageSongsAndNextToken() {
        val page = YtBrowseParser.parse(continuationBrowse(listOf("c", "d"), continuation = "tok-3"))

        assertEquals(listOf("c", "d"), page.songs.map { it.youtubeVideoId })
        assertEquals("tok-3", page.continuation)
    }

    @Test
    fun parse_readsLegacyShelfContinuation() {
        val page = YtBrowseParser.parse(legacyShelfBrowse(listOf("x"), continuation = "legacy-tok"))

        assertEquals(listOf("x"), page.songs.map { it.youtubeVideoId })
        assertEquals("legacy-tok", page.continuation)
    }

    @Test
    fun parse_ignoresContinuationMarkerAsASong() {
        val page = YtBrowseParser.parse(playlistBrowse(listOf("only"), continuation = "more"))

        assertEquals(1, page.songs.size)
        assertEquals("only", page.songs[0].youtubeVideoId)
    }

    @Test
    fun collect_walksLikedSongsContinuationUntilExhausted() {
        val pages = mapOf(
            "VLLM" to playlistBrowse(ids("p1-", 100), continuation = "page-2"),
            "page-2" to continuationBrowse(ids("p2-", 35), continuation = null),
        )

        val result = YtLibrarySync.collect(
            fetchBrowse = { id -> pages[id] },
            fetchContinuation = { token -> pages[token] },
            browseIds = listOf("VLLM"),
        )

        assertNull(result.error)
        assertEquals(135, result.songs.size)
        assertEquals("p1-1", result.songs.first().youtubeVideoId)
        assertEquals("p2-35", result.songs.last().youtubeVideoId)
    }

    @Test
    fun collect_dropsTheOldEightySongCap() {
        val pages = mapOf(
            "VLLM" to playlistBrowse(ids("a", 80), continuation = "more"),
            "more" to continuationBrowse(ids("b", 20), continuation = null),
        )

        val result = YtLibrarySync.collect(
            fetchBrowse = { id -> pages[id] },
            fetchContinuation = { token -> pages[token] },
            browseIds = listOf("VLLM"),
        )

        assertEquals(100, result.songs.size)
    }

    @Test
    fun collect_dedupesLibrarySongsAlreadyInLikedPlaylist() {
        val pages = mapOf(
            "VLLM" to playlistBrowse(listOf("same", "liked"), continuation = null),
            "FEmusic_liked_videos" to libraryShelfBrowse(listOf("same", "library")),
        )

        val result = YtLibrarySync.collect(
            fetchBrowse = { id -> pages[id] },
            fetchContinuation = { null },
        )

        assertEquals(listOf("same", "liked", "library"), result.songs.map { it.youtubeVideoId })
    }

    @Test
    fun collect_stopsOnRepeatedContinuationToken() {
        val looping = continuationBrowse(listOf("again"), continuation = "loop")
        val result = YtLibrarySync.collect(
            fetchBrowse = { playlistBrowse(listOf("first"), continuation = "loop") },
            fetchContinuation = { looping },
            browseIds = listOf("VLLM"),
        )

        assertEquals(listOf("first", "again"), result.songs.map { it.youtubeVideoId })
    }

    @Test
    fun collect_respectsLimitAcrossPages() {
        val result = YtLibrarySync.collect(
            fetchBrowse = { playlistBrowse(ids("a", 10), continuation = "more") },
            fetchContinuation = { continuationBrowse(ids("b", 10), continuation = null) },
            browseIds = listOf("VLLM"),
            limit = 12,
        )

        assertEquals(12, result.songs.size)
    }

    @Test
    fun collectPlaylist_prefixesVlOnTheBrowseId() {
        var requested: String? = null
        YtLibrarySync.collectPlaylist(
            playlistId = "PL123",
            fetchBrowse = { id ->
                requested = id
                playlistBrowse(listOf("a"), continuation = null)
            },
            fetchContinuation = { null },
        )
        assertEquals("VLPL123", requested)
    }

    @Test
    fun collect_returnsBrowseErrorWhenEmpty() {
        val result = YtLibrarySync.collect(
            fetchBrowse = { null },
            fetchContinuation = { null },
            browseIds = listOf("VLLM"),
        )

        assertTrue(result.songs.isEmpty())
        assertEquals("Browse failed (VLLM)", result.error)
    }

    private fun ids(prefix: String, count: Int): List<String> =
        (1..count).map { "$prefix$it" }

    private fun listItem(videoId: String): JSONObject = JSONObject()
        .put("playlistItemData", JSONObject().put("videoId", videoId))
        .put(
            "flexColumns",
            JSONArray().put(
                JSONObject().put(
                    "musicResponsiveListItemFlexColumnRenderer",
                    JSONObject().put(
                        "text",
                        JSONObject().put(
                            "runs",
                            JSONArray().put(JSONObject().put("text", "Song $videoId")),
                        ),
                    ),
                ),
            ).put(
                JSONObject().put(
                    "musicResponsiveListItemFlexColumnRenderer",
                    JSONObject().put(
                        "text",
                        JSONObject().put(
                            "runs",
                            JSONArray().put(JSONObject().put("text", "Artist $videoId")),
                        ),
                    ),
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

    private fun contentsArray(videoIds: List<String>, continuation: String?): JSONArray {
        val contents = JSONArray()
        videoIds.forEach { contents.put(JSONObject().put("musicResponsiveListItemRenderer", listItem(it))) }
        if (continuation != null) contents.put(continuationItem(continuation))
        return contents
    }

    private fun playlistBrowse(videoIds: List<String>, continuation: String?): JSONObject =
        JSONObject().put(
            "contents",
            JSONObject().put(
                "twoColumnBrowseResultsRenderer",
                JSONObject().put(
                    "secondaryContents",
                    JSONObject().put(
                        "sectionListRenderer",
                        JSONObject().put(
                            "contents",
                            JSONArray().put(
                                JSONObject().put(
                                    "musicPlaylistShelfRenderer",
                                    JSONObject().put("contents", contentsArray(videoIds, continuation)),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

    private fun libraryShelfBrowse(videoIds: List<String>): JSONObject =
        JSONObject().put(
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
                                                "musicShelfRenderer",
                                                JSONObject().put("contents", contentsArray(videoIds, null)),
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

    private fun legacyShelfBrowse(videoIds: List<String>, continuation: String): JSONObject =
        JSONObject().put(
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
                                                "musicShelfRenderer",
                                                JSONObject()
                                                    .put("contents", contentsArray(videoIds, null))
                                                    .put(
                                                        "continuations",
                                                        JSONArray().put(
                                                            JSONObject().put(
                                                                "nextContinuationData",
                                                                JSONObject().put("continuation", continuation),
                                                            ),
                                                        ),
                                                    ),
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

    private fun continuationBrowse(videoIds: List<String>, continuation: String?): JSONObject =
        JSONObject().put(
            "onResponseReceivedActions",
            JSONArray().put(
                JSONObject().put(
                    "appendContinuationItemsAction",
                    JSONObject().put("continuationItems", contentsArray(videoIds, continuation)),
                ),
            ),
        )
}
