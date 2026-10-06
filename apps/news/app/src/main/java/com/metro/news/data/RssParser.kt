package com.metro.news.data

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Lightweight RSS 2.0 / Atom reader — regex-based so JVM unit tests do not need Android's
 * XmlPullParser mock.
 */
object RssParser {
    private val itemRegex = Regex(
        pattern = "(?is)<item\\b[^>]*>(.*?)</item>|<entry\\b[^>]*>(.*?)</entry>",
    )
    private val titleRegex = Regex("(?is)<title\\b[^>]*>(.*?)</title>")
    private val linkHrefRegex = Regex("(?is)<link\\b[^>]*href=[\"']([^\"']+)[\"'][^>]*/?>")
    private val linkTextRegex = Regex("(?is)<link\\b[^>]*>(.*?)</link>")
    private val descriptionRegex = Regex(
        "(?is)<description\\b[^>]*>(.*?)</description>|" +
            "<summary\\b[^>]*>(.*?)</summary>|" +
            "<content:encoded\\b[^>]*>(.*?)</content:encoded>|" +
            "<content\\b[^>]*>(.*?)</content>",
    )
    private val pubDateRegex = Regex(
        "(?is)<pubDate\\b[^>]*>(.*?)</pubDate>|" +
            "<published\\b[^>]*>(.*?)</published>|" +
            "<updated\\b[^>]*>(.*?)</updated>",
    )
    private val authorRegex = Regex(
        "(?is)<(?:dc:)?creator\\b[^>]*>(.*?)</(?:dc:)?creator>|" +
            "<author\\b[^>]*>(?:.*?<name\\b[^>]*>)?(.*?)</",
    )
    private val guidRegex = Regex("(?is)<guid\\b[^>]*>(.*?)</guid>|<id\\b[^>]*>(.*?)</id>")
    private val enclosureRegex = Regex(
        "(?is)<enclosure\\b[^>]*url=[\"']([^\"']+)[\"'][^>]*>",
    )
    private val mediaUrlRegex = Regex(
        "(?is)<(?:media:)?(?:content|thumbnail)\\b[^>]*url=[\"']([^\"']+)[\"'][^>]*/?>",
    )
    private val imgSrcRegex = Regex("(?is)<img[^>]+src=[\"']([^\"']+)[\"']")

    private val pubDateFormats = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss z",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ssZ",
    ).map { pattern ->
        SimpleDateFormat(pattern, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = true
        }
    }

    fun parse(xml: String, category: NewsCategory, nowMs: Long = System.currentTimeMillis()): List<NewsStory> {
        return itemRegex.findAll(xml).mapNotNull { match ->
            val body = match.groupValues.drop(1).firstOrNull { it.isNotBlank() }.orEmpty()
            if (body.isBlank()) return@mapNotNull null

            val title = decodeXmlText(firstGroup(titleRegex, body)).trim()
            val link = firstGroup(linkHrefRegex, body)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: decodeXmlText(firstGroup(linkTextRegex, body)).trim()
            if (title.isEmpty() || link.isEmpty()) return@mapNotNull null

            val descriptionRaw = firstGroup(descriptionRegex, body).orEmpty()
            val summary = stripHtml(decodeXmlText(descriptionRaw)).trim()
            val published = parseDate(decodeXmlText(firstGroup(pubDateRegex, body))) ?: nowMs
            val author = decodeXmlText(firstGroup(authorRegex, body)).trim().takeIf { it.isNotEmpty() }
            val guid = decodeXmlText(firstGroup(guidRegex, body)).trim().ifBlank { link }
            val imageUrl = firstGroup(mediaUrlRegex, body)
                ?: firstGroup(enclosureRegex, body)?.takeIf { it.looksLikeImage() }
                ?: firstImgSrc(descriptionRaw)

            NewsStory(
                id = "${category.id}:$guid",
                title = title,
                summary = summary,
                link = link,
                source = category.sourceLabel,
                publishedAtMs = published,
                imageUrl = imageUrl,
                category = category,
                author = author,
            )
        }.toList()
    }

    fun stripHtml(html: String): String {
        return html
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>"), "\n")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun firstGroup(regex: Regex, input: String): String? {
        val match = regex.find(input) ?: return null
        return match.groupValues.drop(1).firstOrNull { it.isNotBlank() }
    }

    private fun decodeXmlText(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var text = raw.trim()
        if (text.startsWith("<![CDATA[", ignoreCase = true) && text.endsWith("]]>")) {
            text = text.removePrefix("<![CDATA[").removePrefix("<![cdata[").removeSuffix("]]>")
        }
        return text
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
    }

    private fun parseDate(raw: String?): Long? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        for (format in pubDateFormats) {
            runCatching { return format.parse(value)?.time }.getOrNull()
        }
        return null
    }

    private fun firstImgSrc(html: String?): String? {
        if (html.isNullOrBlank()) return null
        return imgSrcRegex.find(html)?.groupValues?.getOrNull(1)
    }

    private fun String.looksLikeImage(): Boolean {
        val lower = lowercase(Locale.US)
        return lower.endsWith(".jpg") ||
            lower.endsWith(".jpeg") ||
            lower.endsWith(".png") ||
            lower.endsWith(".webp") ||
            lower.contains("/image") ||
            lower.contains("img.")
    }
}
