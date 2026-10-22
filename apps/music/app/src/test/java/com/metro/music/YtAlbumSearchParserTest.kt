package com.metro.music

import com.metro.music.ytmusic.YtBrowseParser
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun parseSearchArtists_readsBrowseIdAndName() {
        val root = searchAlbumsResponse(
            listOf(
                artistItem("UCIsO58POZa3DofINzEMq7Fg", "AKLO", "102K subscribers"),
                artistItem("UCJTyxF0P4_xTtWn2VHK7lMA", "ZORN", "500K subscribers"),
            ),
        )
        val artists = YtBrowseParser.parseSearchArtists(root)
        assertEquals(2, artists.size)
        assertEquals("AKLO", artists[0].name)
        assertEquals("UCIsO58POZa3DofINzEMq7Fg", artists[0].browseId)
        val pick = YtBrowseParser.pickArtistHit(artists, "Aklo")
        assertEquals("AKLO", pick?.name)
    }

    @Test
    fun parseArtistAbout_readsDescriptionShelf() {
        val root = JSONObject()
            .put(
                "header",
                JSONObject().put(
                    "musicImmersiveHeaderRenderer",
                    JSONObject()
                        .put("title", runs("Radiohead"))
                        .put("description", runs("Short header blurb.")),
                ),
            )
            .put(
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
                                                    "musicDescriptionShelfRenderer",
                                                    JSONObject()
                                                        .put("header", runs("About"))
                                                        .put(
                                                            "description",
                                                            runs(
                                                                "Radiohead are an English rock band formed in Abingdon.",
                                                            ),
                                                        )
                                                        .put("subheader", runs("1B views")),
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
        val about = YtBrowseParser.parseArtistAbout(root)!!
        assertEquals("Radiohead", about.name)
        assertTrue(about.description!!.contains("English rock band"))
        assertEquals("1B views", about.views)
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

    private fun artistItem(browseId: String, name: String, subscribers: String): JSONObject =
        JSONObject()
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
                                JSONObject().put("pageType", "MUSIC_PAGE_TYPE_ARTIST"),
                            ),
                        ),
                ),
            )
            .put(
                "flexColumns",
                JSONArray()
                    .put(flexColumn(name))
                    .put(flexColumn(subscribers)),
            )
            .put(
                "thumbnail",
                JSONObject().put(
                    "musicThumbnailRenderer",
                    JSONObject().put(
                        "thumbnail",
                        JSONObject().put(
                            "thumbnails",
                            JSONArray().put(
                                JSONObject().put("url", "https://example.com/artist.jpg").put("width", 200),
                            ),
                        ),
                    ),
                ),
            )

    private fun runs(text: String): JSONObject =
        JSONObject().put("runs", JSONArray().put(JSONObject().put("text", text)))

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
