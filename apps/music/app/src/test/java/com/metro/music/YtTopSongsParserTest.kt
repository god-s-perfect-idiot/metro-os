package com.metro.music

import com.metro.music.ytmusic.YtBrowseParser
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class YtTopSongsParserTest {
    @Test
    fun parseTopSongsPlaylistId_readsVlPlCarouselHeader() {
        val root = exploreWithCarousels(
            carousel(
                browseId = "VLPL4fGSI1pDJn6O1LS0XSdF3RyO0Rq_LDeI",
                videoIds = listOf("vid1", "vid2"),
            ),
        )
        assertEquals(
            "PL4fGSI1pDJn6O1LS0XSdF3RyO0Rq_LDeI",
            YtBrowseParser.parseTopSongsPlaylistId(root),
        )
    }

    @Test
    fun parseTopSongsPlaylistId_skipsTrendingVlOla() {
        val root = exploreWithCarousels(
            carousel(
                browseId = "VLOLAK5uy_kNWGJvgWVqlt5LsFDL9Sdluly4M8TvGkM",
                videoIds = listOf("trend1"),
            ),
        )
        assertNull(YtBrowseParser.parseTopSongsPlaylistId(root))
    }

    @Test
    fun parseExploreTopSongs_readsCarouselListItems() {
        val root = exploreWithCarousels(
            carousel(
                browseId = "VLPL4fGSI1pDJn6O1LS0XSdF3RyO0Rq_LDeI",
                videoIds = listOf("aaa", "bbb", "ccc"),
            ),
        )
        val songs = YtBrowseParser.parseExploreTopSongs(root)
        assertEquals(3, songs.size)
        assertEquals("yt:aaa", songs[0].id)
        assertEquals("yt:bbb", songs[1].id)
        assertEquals("yt:ccc", songs[2].id)
    }

    private fun exploreWithCarousels(vararg carousels: JSONObject): JSONObject =
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
                                        JSONArray().apply {
                                            carousels.forEach { put(it) }
                                        },
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

    private fun carousel(browseId: String, videoIds: List<String>): JSONObject =
        JSONObject().put(
            "musicCarouselShelfRenderer",
            JSONObject()
                .put(
                    "header",
                    JSONObject().put(
                        "musicCarouselShelfBasicHeaderRenderer",
                        JSONObject().put(
                            "title",
                            JSONObject().put(
                                "runs",
                                JSONArray().put(
                                    JSONObject()
                                        .put("text", "Top songs")
                                        .put(
                                            "navigationEndpoint",
                                            JSONObject().put(
                                                "browseEndpoint",
                                                JSONObject().put("browseId", browseId),
                                            ),
                                        ),
                                ),
                            ),
                        ),
                    ),
                )
                .put(
                    "contents",
                    JSONArray().apply {
                        videoIds.forEach { id ->
                            put(
                                JSONObject().put(
                                    "musicResponsiveListItemRenderer",
                                    JSONObject()
                                        .put(
                                            "playlistItemData",
                                            JSONObject().put("videoId", id),
                                        )
                                        .put(
                                            "flexColumns",
                                            JSONArray()
                                                .put(
                                                    JSONObject().put(
                                                        "musicResponsiveListItemFlexColumnRenderer",
                                                        JSONObject().put(
                                                            "text",
                                                            JSONObject().put(
                                                                "runs",
                                                                JSONArray().put(
                                                                    JSONObject().put("text", "Song $id"),
                                                                ),
                                                            ),
                                                        ),
                                                    ),
                                                )
                                                .put(
                                                    JSONObject().put(
                                                        "musicResponsiveListItemFlexColumnRenderer",
                                                        JSONObject().put(
                                                            "text",
                                                            JSONObject().put(
                                                                "runs",
                                                                JSONArray().put(
                                                                    JSONObject().put("text", "Artist $id"),
                                                                ),
                                                            ),
                                                        ),
                                                    ),
                                                ),
                                        ),
                                ),
                            )
                        }
                    },
                ),
        )
}
