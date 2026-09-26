package com.metro.music.ytmusic

import com.metro.music.data.ArtistAbout
import com.metro.music.data.ArtistAboutLogic
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Wikipedia + Wikidata lookup for the artist **about** pivot.
 * Wikipedia/Wikimedia require a descriptive User-Agent.
 */
class ArtistAboutClient(
    private val http: OkHttpClient = OkHttpClient(),
) {
    fun fetch(artistName: String): ArtistAbout {
        val name = artistName.trim()
        if (name.isEmpty()) {
            return ArtistAbout(name = artistName, error = "Missing artist name")
        }

        val resolvedTitle = resolveWikiTitle(name) ?: name
        val summaryJson = getJson(summaryUrl(resolvedTitle))
        val summary = summaryJson?.let { ArtistAboutParsers.parseWikipediaSummary(it, name) }

        val extractJson = getJson(extractUrl(resolvedTitle))
        val full = extractJson?.let { ArtistAboutParsers.parseMediaWikiExtract(it, name) }

        var about = when {
            full != null && full.error == null -> full.copy(
                description = full.description ?: summary?.description,
                imageUrl = full.imageUrl ?: summary?.imageUrl,
                imageUrls = (full.imageUrls + listOfNotNull(summary?.imageUrl)).distinct(),
                sourceUrl = full.sourceUrl ?: summary?.sourceUrl,
                summary = summary?.summary ?: full.summary,
            )
            summary != null && summary.error == null -> summary
            full != null -> full
            summary != null -> summary
            else -> ArtistAbout(name = name, error = "Couldn't reach Wikipedia")
        }
        if (about.error != null && about.summary == null && about.paragraphs.isEmpty()) {
            return about
        }

        val wikiTitle = summaryJson?.optString("title")?.ifBlank { null }
            ?: resolvedTitle
        val entitiesJson = getJson(wikidataEntitiesUrl(wikiTitle))
        if (entitiesJson != null) {
            val claimIds = ArtistAboutParsers.claimEntityIds(entitiesJson)
            val labels = if (claimIds.isEmpty()) {
                emptyMap()
            } else {
                ArtistAboutParsers.parseLabels(getJson(wikidataLabelsUrl(claimIds)))
            }
            about = ArtistAboutParsers.mergeWikidata(about, entitiesJson, labels)
            val imageFromClaims = ArtistAboutParsers.commonsImageUrls(entitiesJson, labels)
            if (imageFromClaims.isNotEmpty()) {
                about = about.copy(
                    imageUrl = about.imageUrl ?: imageFromClaims.first(),
                    imageUrls = (about.imageUrls + imageFromClaims).distinct(),
                )
            }
        }
        return about
    }

    /** Prefer an exact page; fall back to Wikipedia search. */
    private fun resolveWikiTitle(name: String): String? {
        val encoded = encodeTitle(name)
        val summary = getJson(summaryUrl(encoded))
        if (summary != null && !summary.optString("type").contains("not_found", ignoreCase = true)) {
            val type = summary.optString("type")
            if (type != "disambiguation") {
                return summary.optString("title").ifBlank { name }
            }
        }
        val search = getJson(searchUrl(name)) ?: return null
        return ArtistAboutParsers.firstSearchTitle(search)
    }

    private fun getJson(url: String): JSONObject? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .get()
            .build()
        return runCatching {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) null else JSONObject(body)
            }
        }.getOrNull()
    }

    private fun summaryUrl(title: String): String =
        "$WIKI_SUMMARY${encodeTitle(title)}"

    private fun extractUrl(title: String): String {
        val encoded = URLEncoder.encode(title, StandardCharsets.UTF_8.name())
        return "$WIKI_API?action=query&format=json&redirects=1&titles=$encoded" +
            "&prop=extracts|pageimages|description|info&inprop=url&explaintext=1" +
            "&exsectionformat=wiki&piprop=original|thumbnail&pithumbsize=1200"
    }

    private fun searchUrl(name: String): String {
        val encoded = URLEncoder.encode(name, StandardCharsets.UTF_8.name())
        return "$WIKI_API?action=query&format=json&list=search&srlimit=5&srsearch=$encoded"
    }

    private fun wikidataEntitiesUrl(wikiTitle: String): String {
        val encoded = URLEncoder.encode(wikiTitle, StandardCharsets.UTF_8.name())
        return "$WIKIDATA_API?action=wbgetentities&sites=enwiki&titles=$encoded" +
            "&props=claims&languages=en&format=json"
    }

    private fun wikidataLabelsUrl(ids: List<String>): String {
        val joined = ids.distinct().take(20).joinToString("|")
        val encoded = URLEncoder.encode(joined, StandardCharsets.UTF_8.name())
        return "$WIKIDATA_API?action=wbgetentities&ids=$encoded&props=labels&languages=en&format=json"
    }

    private fun encodeTitle(title: String): String =
        URLEncoder.encode(title.trim().replace(' ', '_'), StandardCharsets.UTF_8.name())

    companion object {
        const val USER_AGENT =
            "MetroOS-Music/1.0 (https://github.com/metro-os; artist about) OkHttp"
        private const val WIKI_SUMMARY = "https://en.wikipedia.org/api/rest_v1/page/summary/"
        private const val WIKI_API = "https://en.wikipedia.org/w/api.php"
        private const val WIKIDATA_API = "https://www.wikidata.org/w/api.php"
    }
}

object ArtistAboutParsers {
    private val SKIP_SECTIONS = setOf(
        "references", "see also", "external links", "notes", "bibliography",
        "further reading", "sources", "citations", "footnotes", "navboxes",
    )

    fun parseWikipediaSummary(root: JSONObject, fallbackName: String): ArtistAbout {
        val type = root.optString("type")
        if (type.contains("not_found", ignoreCase = true)) {
            return ArtistAbout(name = fallbackName, error = "No Wikipedia page found")
        }
        if (type == "disambiguation") {
            return ArtistAbout(
                name = fallbackName,
                error = "Wikipedia has several pages for this name — try a more specific spelling.",
            )
        }
        val title = root.optString("title").ifBlank { fallbackName }
        val extract = root.optString("extract").ifBlank { null }
        val description = root.optString("description").ifBlank { null }
        val imageUrl = root.optJSONObject("originalimage")?.optString("source")?.ifBlank { null }
            ?: root.optJSONObject("thumbnail")?.optString("source")?.ifBlank { null }
        val sourceUrl = root.optJSONObject("content_urls")
            ?.optJSONObject("desktop")
            ?.optString("page")
            ?.ifBlank { null }
        if (extract == null && description == null && imageUrl == null) {
            return ArtistAbout(name = title, error = "No Wikipedia page found")
        }
        return ArtistAbout(
            name = title,
            description = description,
            summary = extract,
            paragraphs = extract?.let { listOf(it) }.orEmpty(),
            imageUrl = imageUrl,
            imageUrls = listOfNotNull(imageUrl),
            sourceUrl = sourceUrl,
        )
    }

    fun parseMediaWikiExtract(root: JSONObject, fallbackName: String): ArtistAbout {
        val pages = root.optJSONObject("query")?.optJSONObject("pages")
            ?: return ArtistAbout(name = fallbackName, error = "No Wikipedia page found")
        val page = pages.keys().asSequence()
            .mapNotNull { pages.optJSONObject(it) }
            .firstOrNull { it.optString("pageid").isNotBlank() || it.has("extract") }
            ?: return ArtistAbout(name = fallbackName, error = "No Wikipedia page found")
        if (page.has("missing") || page.optInt("pageid", -1) < 0 && !page.has("extract")) {
            return ArtistAbout(name = fallbackName, error = "No Wikipedia page found")
        }
        val title = page.optString("title").ifBlank { fallbackName }
        val extract = page.optString("extract").ifBlank { null }
        val description = page.optString("description").ifBlank { null }
        val imageUrl = page.optJSONObject("original")?.optString("source")?.ifBlank { null }
            ?: page.optJSONObject("thumbnail")?.optString("source")?.ifBlank { null }
        val sourceUrl = page.optString("fullurl").ifBlank { null }
            ?: "https://en.wikipedia.org/wiki/${title.replace(' ', '_')}"
        if (extract == null && imageUrl == null) {
            return ArtistAbout(name = title, error = "No Wikipedia page found")
        }
        val (lead, sections) = if (extract != null) {
            ArtistAboutLogic.parseExtractSections(extract)
        } else {
            emptyList<String>() to emptyList()
        }
        val keptSections = sections.filter { it.title.lowercase() !in SKIP_SECTIONS }
        return ArtistAbout(
            name = title,
            description = description,
            summary = lead.firstOrNull() ?: extract,
            paragraphs = lead,
            sections = keptSections,
            imageUrl = imageUrl,
            imageUrls = listOfNotNull(imageUrl),
            sourceUrl = sourceUrl,
        )
    }

    fun firstSearchTitle(root: JSONObject): String? {
        val results = root.optJSONObject("query")?.optJSONArray("search") ?: return null
        for (i in 0 until results.length()) {
            val title = results.optJSONObject(i)?.optString("title")?.ifBlank { null } ?: continue
            return title
        }
        return null
    }

    fun claimEntityIds(root: JSONObject): List<String> {
        val claims = firstEntityClaims(root) ?: return emptyList()
        return buildList {
            addAll(entityIds(claims, "P495"))
            addAll(entityIds(claims, "P136"))
            addAll(entityIds(claims, "P527"))
            // P18 is commons file name (string), not an entity id
        }
    }

    fun parseLabels(root: JSONObject?): Map<String, String> {
        if (root == null) return emptyMap()
        val entities = root.optJSONObject("entities") ?: return emptyMap()
        val out = mutableMapOf<String, String>()
        val keys = entities.keys()
        while (keys.hasNext()) {
            val id = keys.next()
            val label = entities.optJSONObject(id)
                ?.optJSONObject("labels")
                ?.optJSONObject("en")
                ?.optString("value")
                ?.ifBlank { null }
            if (label != null) out[id] = label
        }
        return out
    }

    fun mergeWikidata(
        base: ArtistAbout,
        entitiesRoot: JSONObject,
        labels: Map<String, String>,
    ): ArtistAbout {
        val claims = firstEntityClaims(entitiesRoot) ?: return base
        val formed = timeClaim(claims, "P571") ?: base.formed
        val origin = entityIds(claims, "P495")
            .mapNotNull { labels[it] }
            .firstOrNull()
            ?: base.origin
        val genres = entityIds(claims, "P136")
            .mapNotNull { labels[it] }
            .take(6)
            .joinToString(", ")
            .ifBlank { null }
            ?: base.genres
        val members = entityIds(claims, "P527")
            .mapNotNull { labels[it] }
            .take(8)
            .joinToString(", ")
            .ifBlank { null }
            ?: base.members
        return base.copy(formed = formed, origin = origin, genres = genres, members = members)
    }

    /** Wikidata P18 commons filenames → Special:FilePath URLs. */
    fun commonsImageUrls(entitiesRoot: JSONObject, labels: Map<String, String> = emptyMap()): List<String> {
        val claims = firstEntityClaims(entitiesRoot) ?: return emptyList()
        val arr = claims.optJSONArray("P18") ?: return emptyList()
        return buildList {
            for (i in 0 until minOf(arr.length(), 4)) {
                val file = arr.optJSONObject(i)
                    ?.optJSONObject("mainsnak")
                    ?.optJSONObject("datavalue")
                    ?.optString("value")
                    ?.ifBlank { null }
                    ?: continue
                val encoded = URLEncoder.encode(file.replace(' ', '_'), StandardCharsets.UTF_8.name())
                add("https://commons.wikimedia.org/wiki/Special:FilePath/$encoded?width=1200")
            }
        }
    }

    internal fun timeClaim(claims: JSONObject, property: String): String? {
        val value = claims.optJSONArray(property)
            ?.optJSONObject(0)
            ?.optJSONObject("mainsnak")
            ?.optJSONObject("datavalue")
            ?.optJSONObject("value")
            ?: return null
        val time = value.optString("time").ifBlank { return null }
        return Regex("""([+-]?\d{1,4})-""").find(time)?.groupValues?.getOrNull(1)?.trimStart('+')
    }

    private fun firstEntityClaims(root: JSONObject): JSONObject? {
        val entities = root.optJSONObject("entities") ?: return null
        val keys = entities.keys()
        while (keys.hasNext()) {
            val id = keys.next()
            if (id == "-1") continue
            val claims = entities.optJSONObject(id)?.optJSONObject("claims")
            if (claims != null) return claims
        }
        return null
    }

    private fun entityIds(claims: JSONObject, property: String): List<String> {
        val arr = claims.optJSONArray(property) ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val id = arr.optJSONObject(i)
                    ?.optJSONObject("mainsnak")
                    ?.optJSONObject("datavalue")
                    ?.optJSONObject("value")
                    ?.optString("id")
                    ?.ifBlank { null }
                if (id != null) add(id)
            }
        }
    }
}
