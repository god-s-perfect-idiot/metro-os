package com.metro.music

import com.metro.music.data.Album
import com.metro.music.data.ArtistAbout
import com.metro.music.data.ArtistAboutLogic
import com.metro.music.data.ArtistDiscoverLogic
import com.metro.music.data.LibrarySource
import com.metro.music.data.Song
import com.metro.music.ytmusic.ArtistAboutParsers
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
class ArtistDiscoverLogicTest {
    @Test
    fun songsNotInCollection_dropsMatchingIdsAndTitles() {
        val collection = listOf(song("local:1", "Karma Police", "Radiohead"))
        val discovered = listOf(
            song("yt:a", "Karma Police", "Radiohead"),
            song("yt:b", "Creep", "Radiohead"),
            song("local:1", "Duplicate id", "X"),
        )
        val out = ArtistDiscoverLogic.songsNotInCollection(discovered, collection)
        assertEquals(listOf("yt:b"), out.map { it.id })
    }

    @Test
    fun albumsNotInCollection_dropsMatchingTitles() {
        val collection = listOf(album("a1", "OK Computer", "Radiohead", browseId = null))
        val discovered = listOf(
            album("yt1", "OK Computer", "Radiohead", browseId = "MPREb_1"),
            album("yt2", "In Rainbows", "Radiohead", browseId = "MPREb_2"),
        )
        val out = ArtistDiscoverLogic.albumsNotInCollection(discovered, collection)
        assertEquals(listOf("yt2"), out.map { it.id })
    }

    @Test
    fun factLines_ordersFormedOriginGenres() {
        val about = ArtistAbout(
            name = "Radiohead",
            formed = "1985",
            origin = "United Kingdom",
            genres = "art rock, alternative rock",
        )
        assertEquals(
            listOf(
                "formed" to "1985",
                "origin" to "United Kingdom",
                "genres" to "art rock, alternative rock",
            ),
            ArtistAboutLogic.factLines(about),
        )
    }

    @Test
    fun parseWikipediaSummary_readsExtractAndImage() {
        val json = JSONObject(
            """
            {
              "title": "Radiohead",
              "description": "English rock band",
              "extract": "Radiohead are an English rock band.",
              "thumbnail": { "source": "https://example.com/thumb.jpg" },
              "content_urls": { "desktop": { "page": "https://en.wikipedia.org/wiki/Radiohead" } }
            }
            """.trimIndent(),
        )
        val about = ArtistAboutParsers.parseWikipediaSummary(json, "Radiohead")
        assertEquals("Radiohead", about.name)
        assertEquals("English rock band", about.description)
        assertTrue(about.summary!!.contains("English rock band"))
        assertEquals("https://example.com/thumb.jpg", about.imageUrl)
        assertNull(about.error)
    }

    @Test
    fun parseMediaWikiExtract_splitsSectionsAndParagraphs() {
        val json = JSONObject(
            """
            {
              "query": {
                "pages": {
                  "1": {
                    "pageid": 1,
                    "title": "Radiohead",
                    "description": "English rock band",
                    "extract": "Lead paragraph one.\n\nLead paragraph two.\n\n== History ==\n\nThey formed in Abingdon.\n\nMore history here.\n\n== Discography ==\n\nStudio albums followed.\n\n== References ==\n\n1. Citation",
                    "original": { "source": "https://example.com/hero.jpg" },
                    "fullurl": "https://en.wikipedia.org/wiki/Radiohead"
                  }
                }
              }
            }
            """.trimIndent(),
        )
        val about = ArtistAboutParsers.parseMediaWikiExtract(json, "Radiohead")
        assertEquals(2, about.paragraphs.size)
        assertEquals("https://example.com/hero.jpg", about.imageUrl)
        assertEquals(listOf("History", "Discography"), about.sections.map { it.title })
        assertTrue(about.sections[0].paragraphs[0].contains("Abingdon"))
        assertTrue(about.sections.none { it.title.equals("References", true) })
    }

    @Test
    fun heroImageUrl_picksLargestVariantOnce() {
        val about = ArtistAbout(
            name = "Radiohead",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a1/x.jpg/330px-x.jpg",
            imageUrls = listOf(
                "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a1/x.jpg/330px-x.jpg",
                "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a1/x.jpg/1200px-x.jpg",
                "https://commons.wikimedia.org/wiki/Special:FilePath/x.jpg?width=1200",
            ),
        )
        assertEquals(
            "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a1/x.jpg/1200px-x.jpg",
            ArtistAboutLogic.heroImageUrl(about),
        )
    }

    @Test
    fun parseExtractSections_handlesPlainLead() {
        val (lead, sections) = ArtistAboutLogic.parseExtractSections(
            "One paragraph.\n\nTwo paragraph.",
        )
        assertEquals(2, lead.size)
        assertTrue(sections.isEmpty())
    }

    @Test
    fun timeClaim_readsFormationYear() {
        val claims = JSONObject(
            """
            {
              "P571": [{
                "mainsnak": {
                  "datavalue": {
                    "value": { "time": "+1985-01-01T00:00:00Z" }
                  }
                }
              }]
            }
            """.trimIndent(),
        )
        assertEquals("1985", ArtistAboutParsers.timeClaim(claims, "P571"))
    }

    private fun song(id: String, title: String, artist: String) = Song(
        id = id,
        title = title,
        artist = artist,
        album = "Album",
        durationMs = 1_000,
        uri = null,
        artworkUri = null,
        source = LibrarySource.Local,
    )

    private fun album(id: String, title: String, artist: String, browseId: String?) = Album(
        id = id,
        title = title,
        artist = artist,
        artworkUri = null,
        songCount = 1,
        source = LibrarySource.YouTubeMusic,
        youtubeBrowseId = browseId,
    )
}
