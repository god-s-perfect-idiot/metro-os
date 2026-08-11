package com.metro.music.ytmusic

import android.net.Uri
import com.metro.music.data.ArtworkUrls
import com.metro.music.data.LibrarySource
import com.metro.music.data.Song
import org.json.JSONArray
import org.json.JSONObject

data class YtBrowsePage(
    val songs: List<Song>,
    val continuation: String? = null,
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
        val contents = root.optJSONObject("contents")
            ?.optJSONObject("tabbedSearchResultsRenderer")
            ?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents")
            ?: return out

        for (i in 0 until contents.length()) {
            val shelf = contents.optJSONObject(i)
                ?.optJSONObject("musicShelfRenderer")
                ?.optJSONArray("contents")
                ?: continue
            val (songs, _) = songsAndToken(shelf)
            out += songs
        }
        return out
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
        val artist = item.optJSONObject("subtitle")
            ?.optJSONArray("runs")
            ?.optJSONObject(0)
            ?.optString("text")
            ?: "Unknown artist"
        val thumb = bestThumbnail(
            item.optJSONObject("thumbnailRenderer")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails"),
        )
        return ytSong(videoId, title, artist, thumb)
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
        val artist = subtitleRuns?.optJSONObject(0)?.optString("text").orEmpty()
            .ifBlank { "Unknown artist" }
        val thumb = bestThumbnail(
            item.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails"),
        )
        return ytSong(videoId, title, artist, thumb)
    }

    private fun ytSong(videoId: String, title: String, artist: String, thumb: Uri?) = Song(
        id = "yt:$videoId",
        title = title,
        artist = artist,
        album = "YouTube Music",
        durationMs = 0L,
        uri = null,
        artworkUri = thumb,
        source = LibrarySource.YouTubeMusic,
        youtubeVideoId = videoId,
    )

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
        return browseAll(browseId, limit, fetchBrowse, fetchContinuation)
    }
}
