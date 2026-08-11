package com.metro.music.ytmusic

import android.net.Uri
import com.metro.music.data.ArtworkUrls
import com.metro.music.data.LibrarySource
import com.metro.music.data.Playlist
import org.json.JSONArray
import org.json.JSONObject

/**
 * Innertube playlist-grid parsing. Kept off [YtMusicClient] so unit tests can feed fixtures
 * without standing up OkHttp.
 */
internal object YtMusicLibraryParsers {
    private val SONG_COUNT = Regex("(\\d[\\d,]*)\\s+songs?", RegexOption.IGNORE_CASE)

    fun parsePlaylists(root: JSONObject): List<Playlist> {
        val out = linkedMapOf<String, Playlist>()
        walk(root) { obj ->
            obj.optJSONObject("musicTwoRowItemRenderer")?.let { parsePlaylistRenderer(it) }
                ?.let { out.putIfAbsent(it.id, it) }
            obj.optJSONObject("musicResponsiveListItemRenderer")?.let { parsePlaylistRenderer(it) }
                ?.let { out.putIfAbsent(it.id, it) }
        }
        return out.values.toList()
    }

    fun continuationToken(root: JSONObject): String? {
        var found: String? = null
        walk(root) { obj ->
            if (found != null) return@walk
            obj.optJSONObject("nextContinuationData")
                ?.optString("continuation")
                ?.ifBlank { null }
                ?.let {
                    found = it
                    return@walk
                }
            val endpoint = obj.optJSONObject("continuationItemRenderer")
                ?.optJSONObject("continuationEndpoint")
                ?: return@walk
            endpoint.optJSONObject("continuationCommand")
                ?.optString("token")
                ?.ifBlank { null }
                ?.let {
                    found = it
                    return@walk
                }
            val commands = endpoint.optJSONObject("commandExecutorCommand")?.optJSONArray("commands")
                ?: return@walk
            for (i in 0 until commands.length()) {
                val command = commands.optJSONObject(i)?.optJSONObject("continuationCommand") ?: continue
                if (command.optString("request") == "CONTINUATION_REQUEST_TYPE_BROWSE") {
                    found = command.optString("token").ifBlank { null }
                    if (found != null) return@walk
                }
            }
        }
        return found
    }

    fun playlistIdFromBrowseId(browseId: String): String? {
        val id = browseId.removePrefix("VL").ifBlank { return null }
        if (id == "SE" ||
            id.startsWith("MPRE") ||
            id.startsWith("MPSP") ||
            id.startsWith("MPED") ||
            id.startsWith("UC") ||
            id.startsWith("OLAK") ||
            id.startsWith("RDAM")
        ) {
            return null
        }
        return id
    }

    internal fun parsePlaylistRenderer(item: JSONObject): Playlist? {
        if (item.hasCreatePlaylistEndpoint()) return null

        val browse = item.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
            ?: titleBrowseEndpoint(item)
        val pageType = browse
            ?.optJSONObject("browseEndpointContextSupportedConfigs")
            ?.optJSONObject("browseEndpointContextMusicConfig")
            ?.optString("pageType")
            ?.ifBlank { null }
        if (pageType != null && pageType != "MUSIC_PAGE_TYPE_PLAYLIST") return null

        val rawId = browse?.optString("browseId")?.ifBlank { null }
            ?: watchPlaylistId(item)
            ?: return null
        val playlistId = playlistIdFromBrowseId(rawId) ?: return null
        if (pageType == null && !isLikelyPlaylistId(playlistId)) return null

        val title = titleText(item)?.ifBlank { null } ?: return null
        if (title.equals("New playlist", ignoreCase = true)) return null

        return Playlist(
            id = "yt-pl:$playlistId",
            title = title,
            songCount = songCount(item),
            source = LibrarySource.YouTubeMusic,
            artworkUri = thumbnailUri(item),
            youtubePlaylistId = playlistId,
        )
    }

    private fun isLikelyPlaylistId(id: String): Boolean =
        id == "LM" || id.startsWith("PL") || id.startsWith("RD")

    private fun JSONObject.hasCreatePlaylistEndpoint(): Boolean =
        optJSONObject("navigationEndpoint")?.has("createPlaylistEndpoint") == true

    private fun titleBrowseEndpoint(item: JSONObject): JSONObject? =
        item.optJSONObject("title")
            ?.optJSONArray("runs")
            ?.optJSONObject(0)
            ?.optJSONObject("navigationEndpoint")
            ?.optJSONObject("browseEndpoint")
            ?: flexRun(item, 0)
                ?.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")

    private fun watchPlaylistId(item: JSONObject): String? {
        var found: String? = null
        walk(item) { obj ->
            if (found != null) return@walk
            obj.optJSONObject("watchPlaylistEndpoint")
                ?.optString("playlistId")
                ?.ifBlank { null }
                ?.let { found = it }
        }
        return found
    }

    private fun titleText(item: JSONObject): String? =
        item.optJSONObject("title")
            ?.optJSONArray("runs")
            ?.optJSONObject(0)
            ?.optString("text")
            ?.ifBlank { null }
            ?: flexRun(item, 0)?.optString("text")?.ifBlank { null }

    private fun flexRun(item: JSONObject, column: Int): JSONObject? =
        item.optJSONArray("flexColumns")
            ?.optJSONObject(column)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")
            ?.optJSONObject(0)

    private fun songCount(item: JSONObject): Int {
        val texts = mutableListOf<String>()
        walk(item) { obj ->
            obj.optJSONArray("runs")?.let { runs ->
                for (i in 0 until runs.length()) {
                    runs.optJSONObject(i)?.optString("text")?.let { texts += it }
                }
            }
        }
        for (text in texts) {
            val match = SONG_COUNT.find(text) ?: continue
            return match.groupValues[1].replace(",", "").toIntOrNull() ?: 0
        }
        return 0
    }

    private fun thumbnailUri(item: JSONObject): Uri? {
        var bestUrl: String? = null
        var bestWidth = -1
        walk(item) { obj ->
            val thumbs = obj.optJSONArray("thumbnails") ?: return@walk
            for (i in 0 until thumbs.length()) {
                val thumb = thumbs.optJSONObject(i) ?: continue
                val url = thumb.optString("url").ifBlank { null } ?: continue
                val width = thumb.optInt("width", 0)
                if (width >= bestWidth) {
                    bestWidth = width
                    bestUrl = url
                }
            }
        }
        return bestUrl?.let { Uri.parse(ArtworkUrls.highRes(it)) }
    }

    private fun walk(node: Any?, visit: (JSONObject) -> Unit) {
        when (node) {
            is JSONObject -> {
                visit(node)
                val keys = node.keys()
                while (keys.hasNext()) walk(node.opt(keys.next()), visit)
            }
            is JSONArray -> {
                for (i in 0 until node.length()) walk(node.opt(i), visit)
            }
        }
    }
}

internal object YtPlaylistSync {
    const val DEFAULT_LIMIT = 1_000
    const val MAX_PAGES = 50
    val BROWSE_IDS = listOf(
        "FEmusic_liked_playlists",
        "FEmusic_library_playlists",
    )

    fun collect(
        fetchBrowse: (browseId: String) -> JSONObject?,
        fetchContinuation: (token: String) -> JSONObject?,
        browseIds: List<String> = BROWSE_IDS,
        limit: Int = DEFAULT_LIMIT,
    ): YtPlaylistSyncResult {
        val collected = linkedMapOf<String, Playlist>()
        var lastError: String? = null
        for (browseId in browseIds) {
            if (collected.size >= limit) break
            val first = fetchBrowse(browseId)
            if (first == null) {
                lastError = "Browse failed ($browseId)"
                continue
            }
            val playability = first.optJSONObject("error")?.optString("message")
            if (!playability.isNullOrBlank()) {
                lastError = playability
                continue
            }
            val seenTokens = mutableSetOf<String>()
            var json: JSONObject? = first
            var pages = 0
            while (json != null && pages < MAX_PAGES && collected.size < limit) {
                YtMusicLibraryParsers.parsePlaylists(json).forEach { collected.putIfAbsent(it.id, it) }
                pages++
                val token = YtMusicLibraryParsers.continuationToken(json) ?: break
                if (!seenTokens.add(token) || collected.size >= limit) break
                json = fetchContinuation(token)
            }
        }
        if (collected.isEmpty()) {
            return YtPlaylistSyncResult(
                emptyList(),
                lastError ?: "No playlists in YouTube Music library",
            )
        }
        return YtPlaylistSyncResult(collected.values.take(limit).toList(), null)
    }
}
