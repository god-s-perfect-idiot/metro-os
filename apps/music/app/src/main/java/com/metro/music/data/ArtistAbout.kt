package com.metro.music.data

/**
 * Structured artist / band bio for the artist **about** pivot.
 * Sourced from Wikipedia (+ optional Wikidata claims).
 */
data class ArtistAbout(
    val name: String,
    val description: String? = null,
    /** Short lead paragraph (Wikipedia summary extract). */
    val summary: String? = null,
    /** Full article lead split into readable paragraphs. */
    val paragraphs: List<String> = emptyList(),
    /** Named Wikipedia sections (History, Members, …) with body text. */
    val sections: List<ArtistAboutSection> = emptyList(),
    val imageUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
    val formed: String? = null,
    val origin: String? = null,
    val genres: String? = null,
    val members: String? = null,
    val sourceUrl: String? = null,
    val error: String? = null,
)

data class ArtistAboutSection(
    val title: String,
    val paragraphs: List<String>,
)

object ArtistAboutLogic {
    /** Prefer the first non-blank fact line for the about header strip. */
    fun factLines(about: ArtistAbout): List<Pair<String, String>> = buildList {
        about.formed?.takeIf { it.isNotBlank() }?.let { add("formed" to it) }
        about.origin?.takeIf { it.isNotBlank() }?.let { add("origin" to it) }
        about.genres?.takeIf { it.isNotBlank() }?.let { add("genres" to it) }
        about.members?.takeIf { it.isNotBlank() }?.let { add("members" to it) }
    }

    /** Single best hero image URL (summary original → page image → extras). */
    fun heroImageUrl(about: ArtistAbout): String? {
        val candidates = buildList {
            about.imageUrl?.let { add(it) }
            addAll(about.imageUrls)
        }.map { it.trim() }.filter { it.isNotEmpty() }
        if (candidates.isEmpty()) return null
        // Prefer the largest Wikipedia original when several size variants share a path.
        return candidates.maxBy { urlScore(it) }
    }

    private fun urlScore(url: String): Int {
        val pathWidth = Regex("""/(\d+)px-""").find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (pathWidth != null) return pathWidth
        val queryWidth = Regex("""[?&]width=(\d+)""").find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (queryWidth != null) return queryWidth
        return if (url.contains("Special:FilePath", ignoreCase = true)) 800 else 0
    }

    /**
     * Split a Wikipedia plain-text extract into lead paragraphs + named sections.
     * MediaWiki marks section titles as lines like `== History ==`.
     */
    fun parseExtractSections(extract: String): Pair<List<String>, List<ArtistAboutSection>> {
        val lines = extract.replace("\r\n", "\n").split('\n')
        val leadBuffer = mutableListOf<String>()
        val sections = mutableListOf<ArtistAboutSection>()
        var currentTitle: String? = null
        val currentBody = mutableListOf<String>()

        fun flushSection() {
            val title = currentTitle ?: return
            val paragraphs = paragraphsFromLines(currentBody)
            if (paragraphs.isNotEmpty()) {
                sections += ArtistAboutSection(title = title, paragraphs = paragraphs)
            }
            currentBody.clear()
        }

        for (raw in lines) {
            val line = raw.trim()
            val sectionMatch = SECTION_HEADER.matchEntire(line)
            if (sectionMatch != null) {
                if (currentTitle == null) {
                    leadBuffer += paragraphsFromLines(currentBody)
                    currentBody.clear()
                } else {
                    flushSection()
                }
                currentTitle = sectionMatch.groupValues[1].trim()
                continue
            }
            currentBody += raw
        }
        if (currentTitle == null) {
            leadBuffer += paragraphsFromLines(currentBody)
        } else {
            flushSection()
        }
        return leadBuffer.filter { it.isNotBlank() } to sections
    }

    fun paragraphsFromLines(lines: List<String>): List<String> {
        val text = lines.joinToString("\n").trim()
        if (text.isEmpty()) return emptyList()
        return text.split(Regex("\n\\s*\n"))
            .map { it.replace('\n', ' ').replace(Regex("\\s+"), " ").trim() }
            .filter { it.isNotBlank() }
    }

    private val SECTION_HEADER = Regex("""^={2,}\s*(.+?)\s*={2,}$""")
}
