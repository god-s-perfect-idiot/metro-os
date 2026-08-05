package com.metro.music.ytmusic

import android.net.Uri
import android.util.Log
import com.metro.music.data.ArtworkUrls
import com.metro.music.data.LibrarySource
import com.metro.music.data.Song
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class YtSyncResult(
    val songs: List<Song>,
    val error: String? = null,
)

data class YtStreamResult(
    val url: String? = null,
    val error: String? = null,
)

/**
 * YouTube Music Innertube client.
 * WEB_REMIX for search/browse (with cookies when signed in);
 * IOS / ANDROID_VR for the player, which still return plain stream URLs with no cipher.
 *
 * Player requests must stay anonymous — attaching web cookies to a mobile-app client makes
 * Innertube answer LOGIN_REQUIRED.
 */
class YtMusicClient(
    private val authStore: YtMusicAuthStore,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
) {

    fun searchSongs(query: String, limit: Int = 25): List<Song> {
        if (query.isBlank()) return emptyList()
        val body = webContext().apply {
            put("query", query)
            // Songs filter (ytmusicapi) — raw, not URL-encoded
            put("params", "EgWKAQIIAWoKEAMQBBAJEAoQBQ==")
        }
        val json = post("search", body, WEB_CLIENT) ?: return emptyList()
        return parseSearchSongs(json).ifEmpty { parseAnySongs(json) }.take(limit)
    }

    fun librarySongs(limit: Int = 80): YtSyncResult {
        if (!authStore.connected) {
            return YtSyncResult(emptyList(), "Not connected to YouTube Music")
        }
        val browseIds = listOf(
            "FEmusic_liked_videos",
            "FEmusic_library_corpus_track_artists",
            "FEmusic_history",
        )
        val collected = linkedMapOf<String, Song>()
        var lastError: String? = null
        for (browseId in browseIds) {
            val body = webContext().apply { put("browseId", browseId) }
            val json = post("browse", body, WEB_CLIENT)
            if (json == null) {
                lastError = "Browse failed ($browseId)"
                continue
            }
            val playability = json.optJSONObject("error")?.optString("message")
            if (!playability.isNullOrBlank()) {
                lastError = playability
                continue
            }
            val parsed = parseBrowseSongs(json).ifEmpty { parseAnySongs(json) }
            parsed.forEach { collected[it.id] = it }
            if (collected.size >= limit) break
        }
        if (collected.isEmpty()) {
            // Fallback: search empty-ish charts via search for a space-common letter
            val fallback = searchSongs("a", limit)
            if (fallback.isNotEmpty()) {
                return YtSyncResult(fallback, null)
            }
            return YtSyncResult(
                emptyList(),
                lastError ?: "No YouTube Music library songs found. Try search in get music.",
            )
        }
        return YtSyncResult(collected.values.take(limit).toList(), null)
    }

    fun resolveStreamUrl(videoId: String): String? = resolveStream(videoId).url

    /**
     * Walks the player clients in order of reliability. ANDROID_VR carrying a `visitorData`
     * identity is the only combination that still returns uncapped plain URLs for YouTube Music
     * art tracks; IOS and WEB_REMIX are fallbacks.
     */
    fun resolveStream(videoId: String): YtStreamResult {
        if (videoId.isBlank()) return YtStreamResult(error = "Missing video id")
        var lastError: String? = null
        for (client in PLAYER_CLIENTS) {
            var visitor = visitorData()
            var json = requestPlayer(videoId, client, visitor)
            var state = playabilityStatus(json)
            // A stale visitor identity reads as a signed-out client; mint a new one and retry.
            if (state == "LOGIN_REQUIRED" && visitor != null) {
                authStore.clearVisitorData()
                visitor = visitorData()
                if (visitor != null) {
                    json = requestPlayer(videoId, client, visitor)
                    state = playabilityStatus(json)
                }
            }
            if (json == null) {
                lastError = "Network error"
                continue
            }
            if (state != null && state != "OK") {
                lastError = json.optJSONObject("playabilityStatus")
                    ?.optString("reason")
                    ?.ifBlank { null }
                    ?: state
                Log.w(TAG, "${client.name} playabilityStatus=$state")
                continue
            }
            val url = extractStreamUrl(json)
            if (url != null) return YtStreamResult(url = url)
            lastError = "No playable audio stream"
        }
        return YtStreamResult(error = lastError ?: "Unable to play this track")
    }

    private fun requestPlayer(
        videoId: String,
        client: InnertubeClient,
        visitor: String?,
    ): JSONObject? {
        val body = JSONObject().apply {
            put(
                "context",
                JSONObject().apply {
                    put(
                        "client",
                        client.context().apply {
                            if (visitor != null) put("visitorData", visitor)
                        },
                    )
                },
            )
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
        }
        return post("player", body, client, visitor)
    }

    private fun playabilityStatus(json: JSONObject?): String? =
        json?.optJSONObject("playabilityStatus")?.optString("status")?.ifBlank { null }

    /** Cached Innertube visitor identity, refetched from the YouTube home page when stale. */
    private fun visitorData(): String? {
        if (authStore.visitorDataFresh) return authStore.visitorData
        val fetched = fetchVisitorData()
        if (fetched != null) {
            authStore.visitorData = fetched
            return fetched
        }
        return authStore.visitorData.ifBlank { null }
    }

    private fun fetchVisitorData(): String? {
        val request = Request.Builder()
            .url("https://www.youtube.com/")
            .header("User-Agent", DESKTOP_UA)
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()
        return try {
            http.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val html = resp.body?.string().orEmpty()
                val raw = VISITOR_DATA_REGEX.find(html)?.groupValues?.getOrNull(1) ?: return null
                // The captured value is JSON-escaped (\u003d and friends).
                JSONObject("{\"v\":\"$raw\"}").optString("v").ifBlank { null }
            }
        } catch (e: Exception) {
            Log.w(TAG, "visitorData fetch failed", e)
            null
        }
    }

    private fun extractStreamUrl(json: JSONObject): String? {
        val streaming = json.optJSONObject("streamingData") ?: return null
        val formats = streaming.optJSONArray("adaptiveFormats")
            ?: streaming.optJSONArray("formats")
            ?: return null
        var bestUrl: String? = null
        var bestBitrate = -1
        for (i in 0 until formats.length()) {
            val fmt = formats.optJSONObject(i) ?: continue
            val mime = fmt.optString("mimeType")
            if (!mime.contains("audio")) continue
            val url = fmt.optString("url").ifBlank { null }
                ?: cipherUrl(fmt.optString("signatureCipher").ifBlank { null })
                ?: continue
            val br = fmt.optInt("bitrate", 0)
            if (br >= bestBitrate) {
                bestBitrate = br
                bestUrl = url
            }
        }
        return bestUrl
    }

    private fun cipherUrl(cipher: String?): String? {
        if (cipher.isNullOrBlank()) return null
        // Without JS decipher we can only use clear "url=" segments (rare).
        val parts = cipher.split("&").associate { pair ->
            val idx = pair.indexOf('=')
            if (idx < 0) "" to pair
            else pair.substring(0, idx) to URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
        }
        return parts["url"]?.takeIf { !parts.containsKey("s") && !parts.containsKey("sig") }
    }

    private fun webContext(): JSONObject = JSONObject().apply {
        put("context", JSONObject().apply { put("client", WEB_CLIENT.context()) })
    }

    private fun post(
        endpoint: String,
        body: JSONObject,
        client: InnertubeClient,
        visitor: String? = null,
    ): JSONObject? {
        val url = "${client.base}/$endpoint?prettyPrint=false&key=${client.apiKey}"
        val reqBody = body.toString().toRequestBody(JSON)
        val builder = Request.Builder()
            .url(url)
            .post(reqBody)
            .header("Content-Type", "application/json")
            .header("User-Agent", client.userAgent)
            .header("X-YouTube-Client-Name", client.clientNumber)
            .header("X-YouTube-Client-Version", client.clientVersion)
        if (visitor != null) {
            builder.header("X-Goog-Visitor-Id", visitor)
        }
        if (client.usesWebOrigin) {
            builder
                .header("Origin", MUSIC_ORIGIN)
                .header("Referer", "$MUSIC_ORIGIN/")
        }
        val cookie = authStore.cookie
        // Mobile-app clients must stay signed out; a web cookie makes them answer LOGIN_REQUIRED.
        if (client.sendsCookies && cookie.isNotBlank()) {
            builder.header("Cookie", cookie)
            sapisidHash(cookie)?.let {
                builder
                    .header("Authorization", it)
                    .header("X-Origin", MUSIC_ORIGIN)
                    .header("X-Goog-AuthUser", "0")
            }
        }
        return try {
            http.newCall(builder.build()).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    Log.w(TAG, "HTTP ${resp.code} $endpoint: ${text.take(200)}")
                    return null
                }
                if (text.isBlank()) null else JSONObject(text)
            }
        } catch (e: Exception) {
            Log.w(TAG, "post $endpoint failed", e)
            null
        }
    }

    /** Innertube rejects cookie auth unless the SAPISID digest accompanies it. */
    private fun sapisidHash(cookie: String): String? {
        val sapisid = cookie.split(';')
            .map { it.trim() }
            .firstNotNullOfOrNull { entry ->
                SAPISID_KEYS.firstNotNullOfOrNull { key ->
                    entry.takeIf { it.startsWith("$key=") }?.substringAfter('=')
                }
            }
            ?.ifBlank { null }
            ?: return null
        val seconds = System.currentTimeMillis() / 1000
        val digest = MessageDigest.getInstance("SHA-1")
            .digest("$seconds $sapisid $MUSIC_ORIGIN".toByteArray())
            .joinToString("") { "%02x".format(it) }
        return "SAPISIDHASH ${seconds}_$digest"
    }

    private fun parseSearchSongs(root: JSONObject): List<Song> {
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
            collectShelf(shelf, out)
        }
        return out
    }

    private fun parseBrowseSongs(root: JSONObject): List<Song> {
        val out = mutableListOf<Song>()
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
            ?: return out

        for (i in 0 until sectionContents.length()) {
            val section = sectionContents.optJSONObject(i) ?: continue
            val shelf = section.optJSONObject("musicShelfRenderer")?.optJSONArray("contents")
                ?: section.optJSONObject("musicPlaylistShelfRenderer")?.optJSONArray("contents")
                ?: section.optJSONObject("gridRenderer")?.optJSONArray("items")
                ?: continue
            collectShelf(shelf, out)
        }
        return out
    }

    /** Deep walk for musicResponsiveListItemRenderer / playlistItemData anywhere in the tree. */
    private fun parseAnySongs(root: JSONObject): List<Song> {
        val out = mutableListOf<Song>()
        walk(root) { obj ->
            obj.optJSONObject("musicResponsiveListItemRenderer")?.let { parseListItem(it)?.let { s -> out += s } }
            val videoId = obj.optJSONObject("playlistItemData")?.optString("videoId")?.ifBlank { null }
            if (videoId != null && out.none { it.youtubeVideoId == videoId }) {
                // Minimal song from videoId alone if nested oddly
            }
        }
        return out.distinctBy { it.id }
    }

    private fun collectShelf(shelf: JSONArray, out: MutableList<Song>) {
        for (j in 0 until shelf.length()) {
            val row = shelf.optJSONObject(j) ?: continue
            row.optJSONObject("musicResponsiveListItemRenderer")?.let { parseListItem(it)?.let { s -> out += s } }
            row.optJSONObject("musicTwoRowItemRenderer")?.let { parseTwoRow(it)?.let { s -> out += s } }
        }
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

    /** Innertube lists thumbnails smallest-first; take the largest and ask the CDN to render big. */
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
        return Song(
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

        return Song(
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
    }

    /** One Innertube client identity: endpoint, key, and the headers that must match it. */
    private data class InnertubeClient(
        val name: String,
        val base: String,
        val apiKey: String,
        val clientNumber: String,
        val clientVersion: String,
        val userAgent: String,
        val sendsCookies: Boolean,
        val usesWebOrigin: Boolean,
        val extraContext: Map<String, Any> = emptyMap(),
    ) {
        fun context(): JSONObject = JSONObject().apply {
            put("clientName", name)
            put("clientVersion", clientVersion)
            extraContext.forEach { (key, value) -> put(key, value) }
            put("hl", "en")
            put("gl", "US")
        }
    }

    companion object {
        private const val TAG = "YtMusicClient"
        private const val WEB_BASE = "https://music.youtube.com/youtubei/v1"
        private const val ANDROID_BASE = "https://youtubei.googleapis.com/youtubei/v1"
        private const val MUSIC_ORIGIN = "https://music.youtube.com"
        private val SAPISID_KEYS = listOf("SAPISID", "__Secure-3PAPISID", "__Secure-1PAPISID")
        private val JSON = "application/json".toMediaType()

        private val WEB_CLIENT = InnertubeClient(
            name = "WEB_REMIX",
            base = WEB_BASE,
            apiKey = "AIzaSyC9XL3ZjWddXya6QbzdlsLjKEVUhNnIfjy",
            clientNumber = "67",
            clientVersion = "1.20241028.01.00",
            userAgent = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/130.0.0.0 Mobile Safari/537.36",
            sendsCookies = true,
            usesWebOrigin = true,
        )

        private val IOS_CLIENT = InnertubeClient(
            name = "IOS",
            base = ANDROID_BASE,
            apiKey = "AIzaSyB-63vPrdThhKuerbB2N_l7Kwwcxj6yUAc",
            clientNumber = "5",
            clientVersion = "20.10.4",
            userAgent = "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)",
            sendsCookies = false,
            usesWebOrigin = false,
            extraContext = mapOf(
                "deviceMake" to "Apple",
                "deviceModel" to "iPhone16,2",
                "osName" to "iPhone",
                "osVersion" to "18.3.2.22D82",
            ),
        )

        private const val ANDROID_VR_VERSION = "1.65.10"
        private const val ANDROID_VR_UA =
            "com.google.android.apps.youtube.vr.oculus/$ANDROID_VR_VERSION " +
                "(Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"

        private val ANDROID_VR_CLIENT = InnertubeClient(
            name = "ANDROID_VR",
            base = ANDROID_BASE,
            apiKey = "AIzaSyAOghZGza2MQSZkY_zfZ370N-PUdXEo8AI",
            clientNumber = "28",
            clientVersion = ANDROID_VR_VERSION,
            userAgent = ANDROID_VR_UA,
            sendsCookies = false,
            usesWebOrigin = false,
            extraContext = mapOf(
                "deviceMake" to "Oculus",
                "deviceModel" to "Quest 3",
                "androidSdkVersion" to 32,
                "userAgent" to ANDROID_VR_UA,
                "osName" to "Android",
                "osVersion" to "12L",
                "timeZone" to "UTC",
                "utcOffsetMinutes" to 0,
            ),
        )

        private val PLAYER_CLIENTS = listOf(
            ANDROID_VR_CLIENT,
            IOS_CLIENT,
            WEB_CLIENT.copy(sendsCookies = false),
        )

        private val VISITOR_DATA_REGEX = Regex("\"visitorData\":\"(.*?)\"")
        private const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/130.0.0.0 Safari/537.36"
    }
}
