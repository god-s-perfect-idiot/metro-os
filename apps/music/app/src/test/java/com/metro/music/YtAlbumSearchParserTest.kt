package com.metro.music

import com.metro.music.ytmusic.YtBrowseParser
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class YtAlbumSearchParserTest {
    @Test
    fun parseSearchAlbums_readsBrowseIdTitleAndArtist() {
        val root = searchAlbumsResponse(
            listOf(
                albumItem("MPREb_ok", "OK Computer", "Radiohead"),
                albumItem("MPREb_ir", "In Rainbows", "Radiohead"),
            ),
        )
        val albums = YtBrowseParser.parseSearchAlbums(root)
        assertEquals(2, albums.size)
        assertEquals("OK Computer", albums[0].title)
        assertEquals("Radiohead", albums[0].artist)
        assertEquals("MPREb_ok", albums[0].youtubeBrowseId)
        assertEquals("In Rainbows", albums[1].title)
    }

    @Test
    fun parseSearchAlbums_readsTwoRowCards() {
        val root = JSONObject().put(
            "contents",
            JSONObject().put(
                "tabbedSearchResultsRenderer",
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
                                                JSONObject().put(
                                                    "contents",
                                                    JSONArray().put(
                                                        JSONObject().put(
                                                            "musicTwoRowItemRenderer",
                                                            JSONObject()
                                                                .put(
                                                                    "navigationEndpoint",
                                                                    JSONObject().put(
                                                                        "browseEndpoint",
                                                                        JSONObject().put("browseId", "MPREb_two"),
                                                                    ),
                                                                )
                                                                .put(
                                                                    "title",
                                                                    JSONObject().put(
                                                                        "runs",
                                                                        JSONArray().put(JSONObject().put("text", "Kid A")),
                                                                    ),
                                                                )
                                                                .put(
                                                                    "subtitle",
                                                                    JSONObject().put(
                                                                        "runs",
                                                                        JSONArray().put(JSONObject().put("text", "Radiohead")),
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
                ),
            ),
        )
        val albums = YtBrowseParser.parseSearchAlbums(root)
        assertEquals(1, albums.size)
        assertEquals("Kid A", albums[0].title)
        assertEquals("MPREb_two", albums[0].youtubeBrowseId)
    }

    private fun searchAlbumsResponse(items: List<JSONObject>): JSONObject =
        JSONObject().put(
            "contents",
            JSONObject().put(
                "tabbedSearchResultsRenderer",
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
                                                JSONObject().put(
                                                    "contents",
                                                    JSONArray().apply {
                                                        items.forEach { put(JSONObject().put("musicResponsiveListItemRenderer", it)) }
                                                    },
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

    private fun albumItem(browseId: String, title: String, artist: String): JSONObject =
        JSONObject()
            .put(
                "navigationEndpoint",
                JSONObject().put("browseEndpoint", JSONObject().put("browseId", browseId)),
            )
            .put(
                "flexColumns",
                JSONArray()
                    .put(flexColumn(title))
                    .put(flexColumnRuns(listOf("Album", "•", artist, "•", "1997"))),
            )
            .put(
                "thumbnail",
                JSONObject().put(
                    "musicThumbnailRenderer",
                    JSONObject().put(
                        "thumbnail",
                        JSONObject().put(
                            "thumbnails",
                            JSONArray().put(JSONObject().put("url", "https://example.com/a.jpg").put("width", 200)),
                        ),
                    ),
                ),
            )

    private fun flexColumn(text: String): JSONObject =
        JSONObject().put(
            "musicResponsiveListItemFlexColumnRenderer",
            JSONObject().put(
                "text",
                JSONObject().put("runs", JSONArray().put(JSONObject().put("text", text))),
            ),
        )

    private fun flexColumnRuns(texts: List<String>): JSONObject =
        JSONObject().put(
            "musicResponsiveListItemFlexColumnRenderer",
            JSONObject().put(
                "text",
                JSONObject().put(
                    "runs",
                    JSONArray().apply { texts.forEach { put(JSONObject().put("text", it)) } },
                ),
            ),
        )
}
