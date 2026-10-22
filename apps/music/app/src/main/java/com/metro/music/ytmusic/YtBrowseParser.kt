package com.metro.music.ytmusic

import android.net.Uri
import com.metro.music.data.Album
import com.metro.music.data.ArtworkUrls
import com.metro.music.data.LibraryLogic
import com.metro.music.data.LibrarySource
import com.metro.music.data.Song
import org.json.JSONArray
import org.json.JSONObject

data class YtBrowsePage(
    val songs: List<Song>,
    val continuation: String? = null,
)

/** Artist row from a YouTube Music artists-filtered search. */
data class YtArtistSearchHit(
    val name: String,
    val browseId: String,
    val thumbnailUrl: String? = null,
    val subscribers: String? = null,
)

/** About shelf + header art from an artist channel browse (`UC…`). */
data class YtArtistPageAbout(
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val views: String? = null,
)

/**
 * Innertube browse/search JSON → songs + the next continuation token.
 *
 * Liked songs live on playlist `LM` (`browseId` `VLLM`). YouTube Music returns the first
 * ~100 tracks on the initial browse, then a `continuationItemRenderer` (or legacy
 * `continuations` array) for the rest. Continuation pages arrive under
 * `onResponseReceivedActions` or `continuationContents`.
 */
object YtBrowseParser {

    fun parse(root: JSONObject): YtBrowsePage {
        continuationItemArray(root)?.let { items ->
            val (songs, token) = songsAndToken(items)
            val legacy = continuationShelf(root)?.let { legacyToken(it) }
            return YtBrowsePage(
                songs = songs.ifEmpty { parseAnySongs(root) },
                continuation = token ?: legacy,
            )
        }

        val songs = mutableListOf<Song>()
        var token: String? = null
        for (shelf in shelves(root)) {
            val contents = shelfContents(shelf) ?: continue
            val (pageSongs, pageToken) = songsAndToken(contents)
            songs += pageSongs
            token = pageToken ?: legacyToken(shelf) ?: token
        }
        return YtBrowsePage(
            songs = songs.ifEmpty { parseAnySongs(root) }.distinctBy { it.id },
            continuation = token,
        )
    }

    fun parseSearch(root: JSONObject): List<Song> {
        val out = mutableListOf<Song>()
        for (shelf in searchShelves(root)) {
            val (songs, _) = songsAndToken(shelf)
            out += songs
        }
        return out
    }

    /**
     * Explore (`FEmusic_explore`) carousels: the Top Songs chart header links to a `VLPL…`
     * playlist (full Top 100). Trending uses `VLOLA…` — ignore those.
     */
    fun parseTopSongsPlaylistId(root: JSONObject): String? {
        val sections = sectionListContents(root) ?: return null
        for (i in 0 until sections.length()) {
            val carousel = sections.optJSONObject(i)
                ?.optJSONObject("musicCarouselShelfRenderer")
                ?: continue
            val browseId = carouselTitleBrowseId(carousel) ?: continue
            if (browseId.startsWith("VLPL")) {
                return browseId.removePrefix("VL")
            }
        }
        return null
    }

    /** Songs already listed on the Explore Top Songs carousel (preview; usually ≪ 100). */
    fun parseExploreTopSongs(root: JSONObject): List<Song> {
        val sections = sectionListContents(root) ?: return emptyList()
        for (i in 0 until sections.length()) {
            val carousel = sections.optJSONObject(i)
                ?.optJSONObject("musicCarouselShelfRenderer")
                ?: continue
            val browseId = carouselTitleBrowseId(carousel) ?: continue
            if (!browseId.startsWith("VLPL")) continue
            val contents = carousel.optJSONArray("contents") ?: continue
            val (songs, _) = songsAndToken(contents)
            if (songs.isNotEmpty()) return songs
        }
        return emptyList()
    }

    private fun sectionListContents(root: JSONObject): JSONArray? =
        root.optJSONObject("contents")
            ?.optJSONObject("singleColumnBrowseResultsRenderer")
            ?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents")

    private fun carouselTitleBrowseId(carousel: JSONObject): String? =
        carousel.optJSONObject("header")
            ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
            ?.optJSONObject("title")
            ?.optJSONArray("runs")
            ?.optJSONObject(0)
            ?.optJSONObject("navigationEndpoint")
            ?.optJSONObject("browseEndpoint")
            ?.optString("browseId")
            ?.ifBlank { null }

    fun parseSearchAlbums(root: JSONObject): List<Album> {
        val out = linkedMapOf<String, Album>()
        fun add(album: Album?) {
            if (album == null) return
            out[album.youtubeBrowseId ?: album.id] = album
        }
        for (shelf in searchShelves(root)) {
            for (i in 0 until shelf.length()) {
                val obj = shelf.optJSONObject(i) ?: continue
                obj.optJSONObject("musicResponsiveListItemRenderer")?.let { add(parseAlbumListItem(it)) }
                obj.optJSONObject("musicTwoRowItemRenderer")?.let { add(parseAlbumTwoRow(it)) }
            }
        }
        // Fall back: walk the tree for album browse cards (some layouts nest outside shelves).
        if (out.isEmpty()) {
            walk(root) { node ->
                node.optJSONObject("musicResponsiveListItemRenderer")?.let { add(parseAlbumListItem(it)) }
                node.optJSONObject("musicTwoRowItemRenderer")?.let { add(parseAlbumTwoRow(it)) }
            }
        }
        return out.values.toList()
    }

    fun parseSearchArtists(root: JSONObject): List<YtArtistSearchHit> {
        val out = linkedMapOf<String, YtArtistSearchHit>()
        fun add(hit: YtArtistSearchHit?) {
            if (hit == null) return
            out.putIfAbsent(hit.browseId, hit)
        }
        for (shelf in searchShelves(root)) {
            for (i in 0 until shelf.length()) {
                val obj = shelf.optJSONObject(i) ?: continue
                obj.optJSONObject("musicResponsiveListItemRenderer")?.let { add(parseArtistListItem(it)) }
                obj.optJSONObject("musicTwoRowItemRenderer")?.let { add(parseArtistTwoRow(it)) }
            }
        }
        if (out.isEmpty()) {
            walk(root) { node ->
                node.optJSONObject("musicResponsiveListItemRenderer")?.let { add(parseArtistListItem(it)) }
                node.optJSONObject("musicTwoRowItemRenderer")?.let { add(parseArtistTwoRow(it)) }
            }
        }
        return out.values.toList()
    }

    /**
     * Artist channel browse → about text (musicDescriptionShelf / immersive header) + hero art.
     */
    fun parseArtistAbout(root: JSONObject): YtArtistPageAbout? {
        val header = root.optJSONObject("header")
            ?.optJSONObject("musicImmersiveHeaderRenderer")
            ?: root.optJSONObject("header")
                ?.optJSONObject("musicVisualHeaderRenderer")
        val name = runsText(header?.optJSONObject("title"))
            ?.ifBlank { null }
            ?: return null
        var description = runsText(header?.optJSONObject("description"))?.ifBlank { null }
        var views: String? = null
        walk(root) { node ->
            val shelf = node.optJSONObject("musicDescriptionShelfRenderer") ?: return@walk
            val shelfDesc = runsText(shelf.optJSONObject("description"))?.ifBlank { null }
            // Prefer the About shelf when present — header blurbs are often truncated.
            if (!shelfDesc.isNullOrBlank() &&
                (description.isNullOrBlank() || shelfDesc.length >= description!!.length)
            ) {
                description = shelfDesc
            }
            if (views.isNullOrBlank()) {
                views = runsText(shelf.optJSONObject("subheader"))?.ifBlank { null }
            }
        }
        val imageUrl = bestThumbnailUrl(
            header?.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
                ?: header?.optJSONObject("foregroundThumbnail")
                    ?.optJSONObject("musicThumbnailRenderer")
                    ?.optJSONObject("thumbnail")
                    ?.optJSONArray("thumbnails"),
        )
        if (description.isNullOrBlank() && imageUrl.isNullOrBlank()) return null
        return YtArtistPageAbout(
            name = name,
            description = description,
            imageUrl = imageUrl,
            views = views,
        )
    }

    /** Prefer an exact artist-name match, then a contains match. */
    fun pickArtistHit(hits: List<YtArtistSearchHit>, artistName: String): YtArtistSearchHit? {
        val name = artistName.trim()
        if (name.isEmpty() || hits.isEmpty()) return null
        hits.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { return it }
        return hits.firstOrNull { hit ->
            hit.name.contains(name, ignoreCase = true) ||
                name.contains(hit.name, ignoreCase = true)
        }
    }

    private fun searchShelves(root: JSONObject): List<JSONArray> {
        val contents = root.optJSONObject("contents")
            ?.optJSONObject("tabbedSearchResultsRenderer")
            ?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents")
            ?: return emptyList()
        val shelves = mutableListOf<JSONArray>()
        for (i in 0 until contents.length()) {
            contents.optJSONObject(i)
                ?.optJSONObject("musicShelfRenderer")
                ?.optJSONArray("contents")
                ?.let { shelves += it }
        }
        return shelves
    }

    private fun parseAlbumListItem(item: JSONObject): Album? {
        val browseId = albumBrowseId(item) ?: return null
        val flex = item.optJSONArray("flexColumns") ?: JSONArray()
        val title = flex.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")
            ?.optJSONObject(0)
            ?.optString("text")
            ?.ifBlank { null }
            ?: return null
        val subtitleRuns = flex.optJSONObject(1)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")
        val artist = albumArtistFromRuns(subtitleRuns)
        val thumb = bestThumbnail(
            item.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails"),
        )
        return Album(
            id = "yt-album:$browseId",
            title = title,
            artist = artist,
            artworkUri = thumb,
            songCount = 0,
            source = LibrarySource.YouTubeMusic,
            youtubeBrowseId = browseId,
        )
    }

    private fun parseAlbumTwoRow(item: JSONObject): Album? {
        val browseId = albumBrowseId(item) ?: return null
        val title = item.optJSONObject("title")
            ?.optJSONArray("runs")
            ?.optJSONObject(0)
            ?.optString("text")
            ?.ifBlank { null }
            ?: return null
        val artist = albumArtistFromRuns(
            item.optJSONObject("subtitle")?.optJSONArray("runs"),
        )
        val thumb = bestThumbnail(
            item.optJSONObject("thumbnailRenderer")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails"),
        )
        return Album(
            id = "yt-album:$browseId",
            title = title,
            artist = artist,
            artworkUri = thumb,
            songCount = 0,
            source = LibrarySource.YouTubeMusic,
            youtubeBrowseId = browseId,
        )
    }

    private fun albumBrowseId(item: JSONObject): String? {
        val candidates = listOfNotNull(
            item.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")
                ?.optString("browseId"),
            item.optJSONArray("flexColumns")
                ?.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
                ?.optJSONObject(0)
                ?.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")
                ?.optString("browseId"),
            item.optJSONObject("title")
                ?.optJSONArray("runs")
                ?.optJSONObject(0)
                ?.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")
                ?.optString("browseId"),
        )
        return candidates.firstOrNull { id ->
            id.isNotBlank() && (id.startsWith("MPRE") || id.startsWith("MPRL"))
        }
    }

    private fun parseArtistListItem(item: JSONObject): YtArtistSearchHit? {
        val browseId = artistBrowseId(item) ?: return null
        val flex = item.optJSONArray("flexColumns") ?: JSONArray()
        val name = flex.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")
            ?.optJSONObject(0)
            ?.optString("text")
            ?.ifBlank { null }
            ?: return null
        val subscribers = flex.optJSONObject(1)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")
            ?.optJSONObject(0)
            ?.optString("text")
            ?.ifBlank { null }
        val thumb = bestThumbnailUrl(
            item.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails"),
        )
        return YtArtistSearchHit(
            name = name,
            browseId = browseId,
            thumbnailUrl = thumb,
            subscribers = subscribers,
        )
    }

    private fun parseArtistTwoRow(item: JSONObject): YtArtistSearchHit? {
        val browseId = artistBrowseId(item) ?: return null
        val name = item.optJSONObject("title")
            ?.optJSONArray("runs")
            ?.optJSONObject(0)
            ?.optString("text")
            ?.ifBlank { null }
            ?: return null
        val subscribers = item.optJSONObject("subtitle")
            ?.optJSONArray("runs")
            ?.optJSONObject(0)
            ?.optString("text")
            ?.ifBlank { null }
        val thumb = bestThumbnailUrl(
            item.optJSONObject("thumbnailRenderer")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails"),
        )
        return YtArtistSearchHit(
            name = name,
            browseId = browseId,
            thumbnailUrl = thumb,
            subscribers = subscribers,
        )
    }

    private fun artistBrowseId(item: JSONObject): String? {
        val endpoints = buildList {
            item.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")
                ?.let { add(it) }
            item.optJSONArray("flexColumns")
                ?.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
                ?.optJSONObject(0)
                ?.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")
                ?.let { add(it) }
            item.optJSONObject("title")
                ?.optJSONArray("runs")
                ?.optJSONObject(0)
                ?.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")
                ?.let { add(it) }
        }
        return endpoints.firstOrNull { endpoint ->
            val id = endpoint.optString("browseId")
            val pageType = endpoint.optJSONObject("browseEndpointContextSupportedConfigs")
                ?.optJSONObject("browseEndpointContextMusicConfig")
                ?.optString("pageType")
                .orEmpty()
            id.startsWith("UC") &&
                (pageType.isEmpty() || pageType == "MUSIC_PAGE_TYPE_ARTIST")
        }?.optString("browseId")?.ifBlank { null }
    }

    private fun runsText(node: JSONObject?): String? {
        val runs = node?.optJSONArray("runs") ?: return null
        val text = buildString {
            for (i in 0 until runs.length()) {
                append(runs.optJSONObject(i)?.optString("text").orEmpty())
            }
        }.trim()
        return text.ifBlank { null }
    }

    private fun bestThumbnailUrl(thumbnails: JSONArray?): String? =
        bestThumbnail(thumbnails)?.toString()

    private fun albumArtistFromRuns(runs: JSONArray?): String {
        if (runs == null) return "Unknown artist"
        val texts = (0 until runs.length())
            .mapNotNull { runs.optJSONObject(it)?.optString("text")?.ifBlank { null } }
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "•" && it != "·" }
        val skip = setOf("album", "ep", "single", "lp")
        return texts.firstOrNull { text ->
            text.lowercase() !in skip && !text.matches(Regex("\\d{4}"))
        }.orEmpty().ifBlank { "Unknown artist" }
    }

    private fun continuationItemArray(root: JSONObject): JSONArray? {
        root.optJSONArray("onResponseReceivedActions")?.let { actions ->
            for (i in 0 until actions.length()) {
                actions.optJSONObject(i)
                    ?.optJSONObject("appendContinuationItemsAction")
                    ?.optJSONArray("continuationItems")
                    ?.let { return it }
            }
        }
        val block = continuationShelf(root) ?: return null
        block.optJSONArray("contents")?.let { return it }
        block.optJSONArray("items")?.let { return it }
        return null
    }

    private fun continuationShelf(root: JSONObject): JSONObject? {
        val cc = root.optJSONObject("continuationContents") ?: return null
        CONTINUATION_SHELF_KEYS.firstNotNullOfOrNull { cc.optJSONObject(it) }?.let { return it }
        val sections = cc.optJSONObject("sectionListContinuation")?.optJSONArray("contents")
            ?: return null
        return firstShelfInSections(sections)
    }

    private fun shelves(root: JSONObject): List<JSONObject> {
        val sectionContents = root.optJSONObject("contents")
            ?.optJSONObject("singleColumnBrowseResultsRenderer")
            ?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents")
            ?: root.optJSONObject("contents")
                ?.optJSONObject("twoColumnBrowseResultsRenderer")
                ?.optJSONObject("secondaryContents")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")
            ?: return emptyList()
        return buildList {
            for (i in 0 until sectionContents.length()) {
                val section = sectionContents.optJSONObject(i) ?: continue
                SHELF_KEYS.firstNotNullOfOrNull { section.optJSONObject(it) }?.let { add(it) }
            }
        }
    }

    private fun firstShelfInSections(sections: JSONArray): JSONObject? {
        for (i in 0 until sections.length()) {
            val section = sections.optJSONObject(i) ?: continue
            SHELF_KEYS.firstNotNullOfOrNull { section.optJSONObject(it) }?.let { return it }
        }
        return null
    }

    private fun shelfContents(shelf: JSONObject): JSONArray? =
        shelf.optJSONArray("contents") ?: shelf.optJSONArray("items")

    private fun songsAndToken(items: JSONArray): Pair<List<Song>, String?> {
        val songs = mutableListOf<Song>()
        var token: String? = null
        for (j in 0 until items.length()) {
            val row = items.optJSONObject(j) ?: continue
            modernToken(row)?.let { token = it }
            row.optJSONObject("musicResponsiveListItemRenderer")?.let { parseListItem(it)?.let { s -> songs += s } }
            row.optJSONObject("musicTwoRowItemRenderer")?.let { parseTwoRow(it)?.let { s -> songs += s } }
        }
        return songs to token
    }

    private fun modernToken(item: JSONObject): String? {
        val renderer = item.optJSONObject("continuationItemRenderer") ?: return null
        val endpoint = renderer.optJSONObject("continuationEndpoint") ?: return null
        endpoint.optJSONObject("continuationCommand")
            ?.optString("token")
            ?.ifBlank { null }
            ?.let { return it }
        val commands = endpoint.optJSONObject("commandExecutorCommand")
            ?.optJSONArray("commands")
            ?: return null
        for (i in 0 until commands.length()) {
            val cmd = commands.optJSONObject(i)?.optJSONObject("continuationCommand") ?: continue
            cmd.optString("token").ifBlank { null }?.let { return it }
        }
        return null
    }

    private fun legacyToken(renderer: JSONObject): String? {
        val cont = renderer.optJSONArray("continuations")?.optJSONObject(0) ?: return null
        return cont.optJSONObject("nextContinuationData")
            ?.optString("continuation")
            ?.ifBlank { null }
            ?: cont.optJSONObject("nextRadioContinuationData")
                ?.optString("continuation")
                ?.ifBlank { null }
    }

    private fun parseAnySongs(root: JSONObject): List<Song> {
        val out = mutableListOf<Song>()
        walk(root) { obj ->
            obj.optJSONObject("musicResponsiveListItemRenderer")?.let { parseListItem(it)?.let { s -> out += s } }
        }
        return out.distinctBy { it.id }
    }

    private fun walk(node: Any?, visit: (JSONObject) -> Unit) {
        when (node) {
            is JSONObject -> {
                visit(node)
                val keys = node.keys()
                while (keys.hasNext()) {
                    walk(node.opt(keys.next()), visit)
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) walk(node.opt(i), visit)
            }
        }
    }

    private fun bestThumbnail(thumbnails: JSONArray?): Uri? {
        if (thumbnails == null) return null
        var bestUrl: String? = null
        var bestWidth = -1
        for (i in 0 until thumbnails.length()) {
            val thumb = thumbnails.optJSONObject(i) ?: continue
            val url = thumb.optString("url").ifBlank { null } ?: continue
            val width = thumb.optInt("width", 0)
            if (width >= bestWidth) {
                bestWidth = width
                bestUrl = url
            }
        }
        return bestUrl?.let { Uri.parse(ArtworkUrls.highRes(it)) }
    }

    private fun parseTwoRow(item: JSONObject): Song? {
        val videoId = item.optJSONObject("navigationEndpoint")
            ?.optJSONObject("watchEndpoint")
            ?.optString("videoId")
            ?.ifBlank { null }
            ?: return null
        val title = item.optJSONObject("title")
            ?.optJSONArray("runs")
            ?.optJSONObject(0)
            ?.optString("text")
            ?: "Unknown title"
        val subtitleRuns = item.optJSONObject("subtitle")?.optJSONArray("runs")
        val (artist, album) = artistAndAlbumFromRuns(subtitleRuns)
        val thumb = bestThumbnail(
            item.optJSONObject("thumbnailRenderer")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails"),
        )
        return ytSong(videoId, title, artist, album, thumb)
    }

    private fun parseListItem(item: JSONObject): Song? {
        val videoId = item.optJSONObject("playlistItemData")?.optString("videoId")
            ?.ifBlank { null }
            ?: item.optJSONObject("overlay")
                ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicPlayButtonRenderer")
                ?.optJSONObject("playNavigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")
                ?.ifBlank { null }
            ?: item.optJSONArray("flexColumns")
                ?.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
                ?.optJSONObject(0)
                ?.optJSONObject("navigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")
                ?.ifBlank { null }
            ?: return null

        val flex = item.optJSONArray("flexColumns") ?: JSONArray()
        val title = flex.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")
            ?.optJSONObject(0)
            ?.optString("text")
            ?.ifBlank { null }
            ?: "Unknown title"
        val subtitleRuns = flex.optJSONObject(1)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")
        val col2Runs = flex.optJSONObject(2)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")
        val (artist, albumFromSub) = artistAndAlbumFromRuns(subtitleRuns)
        val albumFromCol2 = col2Runs
            ?.optJSONObject(0)
            ?.optString("text")
            ?.ifBlank { null }
        val album = albumFromSub
            ?: albumFromCol2?.takeUnless { LibraryLogic.isPlaceholderAlbumTitle(it) }
        val thumb = bestThumbnail(
            item.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails"),
        )
        return ytSong(videoId, title, artist, album, thumb)
    }

    /**
     * Subtitle runs are usually `Artist • Album` or `Artist • Album • 3:45`.
     * Prefer browse-linked album runs when present.
     */
    private fun artistAndAlbumFromRuns(runs: JSONArray?): Pair<String, String?> {
        if (runs == null || runs.length() == 0) return "Unknown artist" to null
        val parts = mutableListOf<Pair<String, Boolean>>()
        for (i in 0 until runs.length()) {
            val run = runs.optJSONObject(i) ?: continue
            val text = run.optString("text").trim()
            if (text.isEmpty() || text == "•" || text == "·") continue
            if (text.matches(DURATION_OR_YEAR)) continue
            val isAlbumNav = run.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")
                ?.optString("browseId")
                ?.startsWith("MPRE") == true
            parts += text to isAlbumNav
        }
        if (parts.isEmpty()) return "Unknown artist" to null
        val album = parts.firstOrNull { it.second }?.first
            ?: parts.getOrNull(1)?.first?.takeUnless { LibraryLogic.isPlaceholderAlbumTitle(it) }
        val artist = parts.firstOrNull { !it.second && it.first != album }?.first
            ?: parts.first().first
        return artist to album
    }

    private fun ytSong(
        videoId: String,
        title: String,
        artist: String,
        album: String?,
        thumb: Uri?,
    ) = Song(
        id = "yt:$videoId",
        title = title,
        artist = artist,
        album = album?.takeUnless { LibraryLogic.isPlaceholderAlbumTitle(it) } ?: "",
        durationMs = 0L,
        uri = null,
        artworkUri = thumb,
        source = LibrarySource.YouTubeMusic,
        youtubeVideoId = videoId,
    )

    private val DURATION_OR_YEAR = Regex("""^(\d{1,2}:\d{2}(:\d{2})?|\d{4})$""")

    private val SHELF_KEYS = listOf(
        "musicShelfRenderer",
        "musicPlaylistShelfRenderer",
        "gridRenderer",
    )

    private val CONTINUATION_SHELF_KEYS = listOf(
        "musicPlaylistShelfContinuation",
        "musicShelfContinuation",
        "gridContinuation",
    )
}

/**
 * Walks liked-songs (`VLLM`) then library songs, following continuation tokens until
 * Innertube runs out of pages. The old 80-track cap dropped anything past the first page.
 */
object YtLibrarySync {
    const val DEFAULT_LIMIT = 5_000
    const val MAX_PAGES_PER_BROWSE = 100

    val BROWSE_IDS = listOf(
        "VLLM",
        "FEmusic_liked_videos",
    )

    fun collect(
        fetchBrowse: (browseId: String) -> JSONObject?,
        fetchContinuation: (token: String) -> JSONObject?,
        browseIds: List<String> = BROWSE_IDS,
        limit: Int = DEFAULT_LIMIT,
    ): YtSyncResult {
        val collected = linkedMapOf<String, Song>()
        var lastError: String? = null
        for (browseId in browseIds) {
            if (collected.size >= limit) break
            val page = browseAll(
                browseId = browseId,
                remaining = limit - collected.size,
                fetchBrowse = fetchBrowse,
                fetchContinuation = fetchContinuation,
            )
            if (page.songs.isEmpty() && page.error != null) {
                lastError = page.error
            }
            page.songs.forEach { collected[it.id] = it }
        }
        if (collected.isEmpty()) {
            return YtSyncResult(
                emptyList(),
                lastError ?: "No YouTube Music library songs found. Try search in get music.",
            )
        }
        return YtSyncResult(collected.values.toList(), null)
    }

    private fun browseAll(
        browseId: String,
        remaining: Int,
        fetchBrowse: (browseId: String) -> JSONObject?,
        fetchContinuation: (token: String) -> JSONObject?,
    ): YtSyncResult {
        if (remaining <= 0) return YtSyncResult(emptyList())
        val first = fetchBrowse(browseId)
            ?: return YtSyncResult(emptyList(), "Browse failed ($browseId)")
        val playability = first.optJSONObject("error")?.optString("message")
        if (!playability.isNullOrBlank()) {
            return YtSyncResult(emptyList(), playability)
        }

        val collected = linkedMapOf<String, Song>()
        val seenTokens = mutableSetOf<String>()
        var page = YtBrowseParser.parse(first)
        page.songs.forEach { collected[it.id] = it }
        var token = page.continuation
        var pages = 1
        while (
            token != null &&
            collected.size < remaining &&
            pages < MAX_PAGES_PER_BROWSE &&
            seenTokens.add(token)
        ) {
            val json = fetchContinuation(token) ?: break
            page = YtBrowseParser.parse(json)
            page.songs.forEach { collected[it.id] = it }
            token = page.continuation
            pages++
        }
        return YtSyncResult(collected.values.take(remaining).toList(), null)
    }

    fun collectPlaylist(
        playlistId: String,
        fetchBrowse: (browseId: String) -> JSONObject?,
        fetchContinuation: (token: String) -> JSONObject?,
        limit: Int = DEFAULT_LIMIT,
    ): YtSyncResult {
        val browseId = if (playlistId.startsWith("VL")) playlistId else "VL$playlistId"
        return collectBrowse(browseId, fetchBrowse, fetchContinuation, limit)
    }

    /** Browse any Innertube id as-is (album `MPREb_…`, playlist `VL…`, etc.). */
    fun collectBrowse(
        browseId: String,
        fetchBrowse: (browseId: String) -> JSONObject?,
        fetchContinuation: (token: String) -> JSONObject?,
        limit: Int = DEFAULT_LIMIT,
    ): YtSyncResult = browseAll(browseId, limit, fetchBrowse, fetchContinuation)
}
