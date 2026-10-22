package com.metro.music.ytmusic

import com.metro.music.data.ArtistAbout
import com.metro.music.data.ArtistAboutLogic
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Artist **about** pivot lookup.
 *
 * Order: YouTube Music artist page → Wikidata music entity (+ enwiki when present) →
 * filtered Wikipedia search. Exact Wikipedia title matches that are not musicians/bands
 * (e.g. "Aklo" the fictional language) are rejected.
 */
class ArtistAboutClient(
    private val http: OkHttpClient = OkHttpClient(),
    private val ytClient: YtMusicClient? = null,
) {
    /**
     * Lightweight hero image for the hub artist backdrop.
     * Prefer YouTube Music channel art, then a music-filtered Wikipedia / Commons image.
     */
    fun fetchHeroImageUrl(artistName: String): String? {
        val name = artistName.trim()
        if (name.isEmpty()) return null
        ytClient?.artistAbout(name)?.imageUrl?.let { return it }
        val wiki = fetchWikipediaMusic(name) ?: return null
        return ArtistAboutLogic.heroImageUrl(wiki)
    }

    fun fetch(artistName: String): ArtistAbout {
        val name = artistName.trim()
        if (name.isEmpty()) {
            return ArtistAbout(name = artistName, error = "Missing artist name")
        }

        val yt = runCatching { ytClient?.artistAbout(name) }.getOrNull()
        val wiki = fetchWikipediaMusic(name)

        val merged = ArtistAboutLogic.mergeSources(
            artistName = name,
            youtube = yt?.let {
                ArtistAbout(
                    name = it.name.ifBlank { name },
                    description = null,
                    summary = it.description,
                    paragraphs = it.description?.let { text -> listOf(text) }.orEmpty(),
                    imageUrl = it.imageUrl,
                    imageUrls = listOfNotNull(it.imageUrl),
                    sourceUrl = null,
                )
            },
            wikipedia = wiki,
        )
        if (merged != null) return merged

        return ArtistAbout(
            name = name,
            error = "No artist information found",
            imageUrl = yt?.imageUrl,
            imageUrls = listOfNotNull(yt?.imageUrl),
        )
    }

    /**
     * Resolve a music-related Wikipedia / Wikidata page for [name].
     * Never returns a non-music same-name article.
     */
    private fun fetchWikipediaMusic(name: String): ArtistAbout? {
        val entity = resolveMusicEntity(name)
        if (entity != null) {
            val fromWiki = entity.enwikiTitle?.let { loadWikipediaPage(it, name) }
            if (fromWiki != null && fromWiki.error == null) {
                return enrichWithWikidata(fromWiki, entity.id)
            }
            // Music entity without an English Wikipedia article — use Wikidata description.
            if (!entity.description.isNullOrBlank() || entity.hasMusicFacts) {
                val stub = ArtistAbout(
                    name = entity.label ?: name,
                    description = entity.description,
                    // Keep short Wikidata copy in description only (UI shows it once).
                    summary = null,
                    paragraphs = emptyList(),
                    sourceUrl = "https://www.wikidata.org/wiki/${entity.id}",
                )
                return enrichWithWikidata(stub, entity.id)
            }
        }

        // Last resort: Wikipedia search biased toward musicians, then validate.
        for (title in musicBiasedSearchTitles(name)) {
            val page = loadWikipediaPage(title, name) ?: continue
            if (page.error != null) continue
            val wikiItem = pageWikiItemId(title)
            if (wikiItem != null && !isMusicEntityId(wikiItem)) continue
            if (wikiItem == null && !ArtistAboutMusicFilter.looksLikeMusicDescription(page.description)) {
                continue
            }
            return if (wikiItem != null) enrichWithWikidata(page, wikiItem) else page
        }
        return null
    }

    private fun loadWikipediaPage(title: String, fallbackName: String): ArtistAbout? {
        val summaryJson = getJson(summaryUrl(title))
        val summary = summaryJson?.let { ArtistAboutParsers.parseWikipediaSummary(it, fallbackName) }
        val extractJson = getJson(extractUrl(title))
        val full = extractJson?.let { ArtistAboutParsers.parseMediaWikiExtract(it, fallbackName) }

        return when {
            full != null && full.error == null -> full.copy(
                description = full.description ?: summary?.description,
                imageUrl = full.imageUrl ?: summary?.imageUrl,
                imageUrls = (full.imageUrls + listOfNotNull(summary?.imageUrl)).distinct(),
                sourceUrl = full.sourceUrl ?: summary?.sourceUrl,
                summary = summary?.summary ?: full.summary,
            )
            summary != null && summary.error == null -> summary
            else -> null
        }
    }

    private fun enrichWithWikidata(base: ArtistAbout, entityId: String): ArtistAbout {
        val entitiesJson = getJson(wikidataEntitiesByIdUrl(listOf(entityId))) ?: return base
        val claimIds = ArtistAboutParsers.claimEntityIds(entitiesJson)
        val labels = if (claimIds.isEmpty()) {
            emptyMap()
        } else {
            ArtistAboutParsers.parseLabels(getJson(wikidataLabelsUrl(claimIds)))
        }
        var about = ArtistAboutParsers.mergeWikidata(base, entitiesJson, labels)
        val imageFromClaims = ArtistAboutParsers.commonsImageUrls(entitiesJson, labels)
        if (imageFromClaims.isNotEmpty()) {
            about = about.copy(
                imageUrl = about.imageUrl ?: imageFromClaims.first(),
                imageUrls = (about.imageUrls + imageFromClaims).distinct(),
            )
        }
        return about
    }

    private fun resolveMusicEntity(name: String): MusicEntityHit? {
        val search = getJson(wikidataSearchUrl(name)) ?: return null
        val candidates = ArtistAboutParsers.parseWikidataSearch(search)
            .take(8)
        if (candidates.isEmpty()) return null

        val ids = candidates.map { it.id }
        val entitiesJson = getJson(wikidataEntitiesByIdUrl(ids)) ?: return null
        val entities = entitiesJson.optJSONObject("entities") ?: return null

        var best: MusicEntityHit? = null
        var bestScore = 0
        for (candidate in candidates) {
            val entity = entities.optJSONObject(candidate.id) ?: continue
            val score = ArtistAboutMusicFilter.scoreEntity(name, candidate, entity)
            if (score > bestScore) {
                bestScore = score
                best = MusicEntityHit(
                    id = candidate.id,
                    label = candidate.label,
                    description = candidate.description
                        ?: entity.optJSONObject("descriptions")
                            ?.optJSONObject("en")
                            ?.optString("value")
                            ?.ifBlank { null },
                    enwikiTitle = entity.optJSONObject("sitelinks")
                        ?.optJSONObject("enwiki")
                        ?.optString("title")
                        ?.ifBlank { null },
                    hasMusicFacts = ArtistAboutMusicFilter.hasMusicClaims(entity),
                )
            }
        }
        return best.takeIf { bestScore >= ArtistAboutMusicFilter.MIN_ACCEPT_SCORE }
    }

    private fun musicBiasedSearchTitles(name: String): List<String> {
        val queries = listOf(
            "\"$name\" (musician OR singer OR rapper OR band OR \"musical artist\" OR DJ)",
            "$name musician",
            "$name (singer)",
            "$name (rapper)",
            "$name (band)",
        )
        val titles = linkedSetOf<String>()
        for (query in queries) {
            val search = getJson(searchUrl(query)) ?: continue
            ArtistAboutParsers.searchTitles(search).forEach { titles += it }
            if (titles.size >= 8) break
        }
        return titles.toList()
    }

    private fun pageWikiItemId(title: String): String? {
        val summary = getJson(summaryUrl(title)) ?: return null
        return summary.optString("wikibase_item").ifBlank { null }
    }

    private fun isMusicEntityId(entityId: String): Boolean {
        val entitiesJson = getJson(wikidataEntitiesByIdUrl(listOf(entityId))) ?: return false
        val entity = entitiesJson.optJSONObject("entities")?.optJSONObject(entityId) ?: return false
        return ArtistAboutMusicFilter.scoreEntity(
            name = entity.optJSONObject("labels")
                ?.optJSONObject("en")
                ?.optString("value")
                .orEmpty(),
            candidate = WikidataSearchHit(
                id = entityId,
                label = entity.optJSONObject("labels")
                    ?.optJSONObject("en")
                    ?.optString("value"),
                description = entity.optJSONObject("descriptions")
                    ?.optJSONObject("en")
                    ?.optString("value"),
            ),
            entity = entity,
        ) >= ArtistAboutMusicFilter.MIN_ACCEPT_SCORE
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

    private fun searchUrl(query: String): String {
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
        return "$WIKI_API?action=query&format=json&list=search&srlimit=8&srsearch=$encoded"
    }

    private fun wikidataSearchUrl(name: String): String {
        val encoded = URLEncoder.encode(name, StandardCharsets.UTF_8.name())
        return "$WIKIDATA_API?action=wbsearchentities&search=$encoded&language=en&uselang=en" +
            "&type=item&limit=8&format=json"
    }

    private fun wikidataEntitiesByIdUrl(ids: List<String>): String {
        val joined = ids.distinct().take(12).joinToString("|")
        val encoded = URLEncoder.encode(joined, StandardCharsets.UTF_8.name())
        return "$WIKIDATA_API?action=wbgetentities&ids=$encoded" +
            "&props=claims|descriptions|labels|sitelinks&languages=en&format=json"
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

data class WikidataSearchHit(
    val id: String,
    val label: String?,
    val description: String?,
)

private data class MusicEntityHit(
    val id: String,
    val label: String?,
    val description: String?,
    val enwikiTitle: String?,
    val hasMusicFacts: Boolean,
)

/**
 * Scores Wikidata entities so "Aklo" the fictional language loses to "AKLO" the MC/singer.
 */
object ArtistAboutMusicFilter {
    const val MIN_ACCEPT_SCORE = 40

    /** Musical group / ensemble instance-of values. */
    private val MUSIC_GROUP_TYPES = setOf(
        "Q215380", // musical group / band
        "Q2088357", // musical ensemble
        "Q5741069", // rock band
        "Q105756581", // musical duo
        "Q9212979", // musical trio
        "Q4063284", // boy band
        "Q253137", // girl group
        "Q1135557", // jazz band
        "Q207338", // orchestra
    )

    /** Musician occupations (P106). */
    private val PERFORMER_OCCUPATIONS = setOf(
        "Q177220", // singer
        "Q36834", // composer
        "Q639669", // musician
        "Q183945", // record producer
        "Q488205", // rapper
        "Q753110", // songwriter
        "Q855091", // conductor
        "Q130857", // disc jockey
        "Q497240", // master of ceremonies / MC
        "Q2252262", // rapper (alt)
        "Q2643890", // music arranger
        "Q2865819", // musical artist
        "Q15981151", // jazz musician
        "Q18629921", // multi-instrumentalist
        "Q13424387", // beatmaker
        "Q16145150", // singer-guitarist
        "Q55960555", // hip hop musician
        "Q10833314", // trumpeter / instrumentalist roles
    )

    private val MUSIC_DESC_HINTS = listOf(
        "singer", "songwriter", "rapper", "musician", "band", "dj", "vocalist",
        "composer", "mc", "hip hop", "hip-hop", "musical artist", "record producer",
        "guitarist", "pianist", "drummer", "bassist", "orchestra", "ensemble",
        "master of ceremonies", "music producer", "recording artist",
    )

    private val NON_MUSIC_DESC_HINTS = listOf(
        "language", "village", "mountain", "chemical", "fictional", "disambiguation",
        "township", "settlement", "asteroid", "genus", "species", "river", "album series",
    )

    fun looksLikeMusicDescription(description: String?): Boolean {
        val d = description?.lowercase().orEmpty()
        if (d.isEmpty()) return false
        if (NON_MUSIC_DESC_HINTS.any { it in d } && MUSIC_DESC_HINTS.none { it in d }) {
            return false
        }
        return MUSIC_DESC_HINTS.any { it in d }
    }

    fun hasMusicClaims(entity: JSONObject): Boolean {
        val claims = entity.optJSONObject("claims") ?: return false
        val instanceOf = entityIds(claims, "P31")
        if (instanceOf.any { it in MUSIC_GROUP_TYPES }) return true
        val occupations = entityIds(claims, "P106")
        if (occupations.any { it in PERFORMER_OCCUPATIONS }) return true
        // Genre claim is a weak music signal for humans/groups.
        if (claims.has("P136") && (instanceOf.contains("Q5") || instanceOf.any { it in MUSIC_GROUP_TYPES })) {
            return true
        }
        return false
    }

    fun scoreEntity(
        name: String,
        candidate: WikidataSearchHit,
        entity: JSONObject,
    ): Int {
        val claims = entity.optJSONObject("claims")
        val instanceOf = claims?.let { entityIds(it, "P31") }.orEmpty()
        val occupations = claims?.let { entityIds(it, "P106") }.orEmpty()
        val description = candidate.description
            ?: entity.optJSONObject("descriptions")
                ?.optJSONObject("en")
                ?.optString("value")
                ?.ifBlank { null }
        val label = candidate.label
            ?: entity.optJSONObject("labels")
                ?.optJSONObject("en")
                ?.optString("value")

        var score = 0
        if (label != null && label.equals(name, ignoreCase = true)) score += 25
        else if (label != null &&
            (label.contains(name, ignoreCase = true) || name.contains(label, ignoreCase = true))
        ) {
            score += 10
        }

        if (instanceOf.any { it in MUSIC_GROUP_TYPES }) score += 50
        if (occupations.any { it in PERFORMER_OCCUPATIONS }) score += 50
        if (instanceOf.contains("Q5") && occupations.any { it in PERFORMER_OCCUPATIONS }) score += 10
        if (claims?.has("P136") == true) score += 15
        if (looksLikeMusicDescription(description)) score += 30

        // Hard penalties for clear non-music pages that share a short name.
        if (description != null && NON_MUSIC_DESC_HINTS.any { it in description.lowercase() } &&
            !looksLikeMusicDescription(description)
        ) {
            score -= 60
        }
        // Fictional language / non-human without music occupation.
        if (instanceOf.isNotEmpty() &&
            !instanceOf.contains("Q5") &&
            instanceOf.none { it in MUSIC_GROUP_TYPES } &&
            occupations.none { it in PERFORMER_OCCUPATIONS }
        ) {
            score -= 40
        }
        return score
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

    fun firstSearchTitle(root: JSONObject): String? = searchTitles(root).firstOrNull()

    fun searchTitles(root: JSONObject): List<String> {
        val results = root.optJSONObject("query")?.optJSONArray("search") ?: return emptyList()
        return buildList {
            for (i in 0 until results.length()) {
                val title = results.optJSONObject(i)?.optString("title")?.ifBlank { null } ?: continue
                add(title)
            }
        }
    }

    fun parseWikidataSearch(root: JSONObject): List<WikidataSearchHit> {
        val arr = root.optJSONArray("search") ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val id = item.optString("id").ifBlank { null } ?: continue
                add(
                    WikidataSearchHit(
                        id = id,
                        label = item.optString("label").ifBlank { null },
                        description = item.optString("description").ifBlank { null },
                    ),
                )
            }
        }
    }

    fun claimEntityIds(root: JSONObject): List<String> {
        val claims = firstEntityClaims(root) ?: return emptyList()
        return buildList {
            addAll(entityIds(claims, "P495")) // country of origin
            addAll(entityIds(claims, "P27")) // country of citizenship
            addAll(entityIds(claims, "P136")) // genre
            addAll(entityIds(claims, "P527")) // has part / members
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
            ?: entityIds(claims, "P27")
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
