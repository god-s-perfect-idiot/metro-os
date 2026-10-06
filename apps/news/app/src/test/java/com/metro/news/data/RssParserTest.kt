package com.metro.news.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RssParserTest {
    @Test
    fun parse_readsItemsWithMediaThumbnail() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/">
              <channel>
                <title>Demo</title>
                <item>
                  <title>Sample Headline</title>
                  <link>https://example.com/a</link>
                  <description><![CDATA[<p>Hello <b>world</b></p>]]></description>
                  <pubDate>Wed, 06 Oct 2026 10:00:00 GMT</pubDate>
                  <media:thumbnail width="240" height="135" url="https://example.com/img.jpg"/>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val stories = RssParser.parse(xml, NewsCategory.Top, nowMs = 1_700_000_000_000L)
        assertEquals(1, stories.size)
        assertEquals("Sample Headline", stories[0].title)
        assertEquals("https://example.com/a", stories[0].link)
        assertEquals("Hello world", stories[0].summary)
        assertEquals("https://example.com/img.jpg", stories[0].imageUrl)
        assertTrue(stories[0].id.startsWith("top:"))
    }

    @Test
    fun stripHtml_collapsesWhitespace() {
        val text = RssParser.stripHtml("<p>One</p><br/>Two&nbsp;&amp; three")
        assertEquals("One Two & three", text)
    }
}
