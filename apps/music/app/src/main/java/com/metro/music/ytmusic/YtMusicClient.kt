package com.metro.music.ytmusic

import android.util.Log
import com.metro.music.data.Playlist
import com.metro.music.data.Song
import com.metro.music.ytmusic.potoken.YtPoTokenSession
import com.metro.music.ytmusic.potoken.appendStreamPoToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class YtSyncResult(
    val songs: List<Song>,
    val error: String? = null,
)

data class YtPlaylistSyncResult(
    val playlists: List<Playlist>,
    val error: String? = null,
)

data class YtStreamResult(
    val url: String? = null,
    val error: String? = null,
)

/**
 * YouTube Music Innertube client.
 * WEB_REMIX for search/browse (with cookies when signed in);
 * IOS first for the player (plain URLs / HLS), then ANDROID_VR and anonymous WEB_REMIX.
 * Adaptive GVS URLs get a BotGuard streaming `pot=` when [poTokenSession] can mint one.
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
    private val poTokenSession: YtPoTokenSession? = null,
) {

    fun searchSongs(query: String, limit: Int = 25): List<Song> {
        if (query.isBlank()) return emptyList()
        val body = webContext().apply {
            put("query", query)
            // Songs filter (ytmusicapi) — raw, not URL-encoded
            put("params", "EgWKAQIIAWoKEAMQBBAJEAoQBQ==")
        }
        val json = post("search", body, WEB_CLIENT) ?: return emptyList()
        return YtBrowseParser.parseSearch(json)
            .ifEmpty { YtBrowseParser.parse(json).songs }
            .take(limit)
    }

    fun librarySongs(limit: Int = YtLibrarySync.DEFAULT_LIMIT): YtSyncResult {
        if (!authStore.connected) {
            return YtSyncResult(emptyList(), "Not connected to YouTube Music")
        }
        val result = YtLibrarySync.collect(
            fetchBrowse = ::browsePost,
            fetchContinuation = ::continuationPost,
            limit = limit,
        )
        Log.i(TAG, "librarySongs synced ${result.songs.size}" + (result.error?.let { " error=$it" } ?: ""))
        if (result.songs.isNotEmpty()) return result
        // Fallback: search empty-ish charts via search for a space-common letter
        val fallback = searchSongs("a", limit.coerceAtMost(25))
        if (fallback.isNotEmpty()) {
            return YtSyncResult(fallback, null)
        }
        return result
    }

    fun libraryPlaylists(limit: Int = YtPlaylistSync.DEFAULT_LIMIT): YtPlaylistSyncResult {
        if (!authStore.connected) {
            return YtPlaylistSyncResult(emptyList(), "Not connected to YouTube Music")
        }
        val result = YtPlaylistSync.collect(
            fetchBrowse = ::browsePost,
            fetchContinuation = ::continuationPost,
            limit = limit,
        )
        Log.i(
            TAG,
            "libraryPlaylists synced ${result.playlists.size}" +
                (result.error?.let { " error=$it" } ?: ""),
        )
        return result
    }

    fun playlistSongs(playlistId: String, limit: Int = YtLibrarySync.DEFAULT_LIMIT): List<Song> {
        if (playlistId.isBlank()) return emptyList()
        return YtLibrarySync.collectPlaylist(
            playlistId = playlistId,
            fetchBrowse = ::browsePost,
            fetchContinuation = ::continuationPost,
            limit = limit,
        ).songs
    }

    suspend fun resolveStreamUrl(videoId: String): String? = resolveStream(videoId).url

    /**
     * Resolve a playable URL. Mints a GVS streaming PO token (BotGuard) when possible and appends
     * `pot=` so adaptive googlevideo Ranges succeed past ~1 MiB. Innertube clients are tried in
     * order; streams must pass a mid-file Range probe when content length is known.
     */
    suspend fun resolveStream(videoId: String): YtStreamResult {
        if (videoId.isBlank()) return YtStreamResult(error = "Missing video id")

        var visitor = visitorData()
        val streamingPot = mintStreamingPot(visitor)
        val playerPot = if (streamingPot != null) {
            runCatching { poTokenSession?.mintPlayerPot(videoId) }.getOrNull()
        } else {
            null
        }

        var lastError: String? = null
        var fallbackUrl: String? = null
        for (client in PLAYER_CLIENTS) {
            visitor = visitorData()
            var json = requestPlayer(videoId, client, visitor, playerPot)
            var state = playabilityStatus(json)
            // A stale visitor identity reads as a signed-out client; mint a new one and retry.
            if (state == "LOGIN_REQUIRED" && visitor != null) {
                authStore.clearVisitorData()
                visitor = visitorData()
                if (visitor != null) {
                    json = requestPlayer(videoId, client, visitor, playerPot)
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
            val hls = json.optJSONObject("streamingData")
                ?.optString("hlsManifestUrl")
                ?.ifBlank { null }
            if (hls != null) {
                Log.i(TAG, "${client.name} resolved HLS stream")
                return YtStreamResult(url = hls)
            }
            val selected = extractStream(json)
            if (selected == null) {
                lastError = "No playable audio stream"
                Log.w(TAG, "${client.name} had OK playability but no usable audio URL")
                continue
            }
            val stamped = if (streamingPot != null) {
                appendStreamPoToken(selected.url, streamingPot)
            } else {
                selected.url
            }
            val playable = ensureClenOnUrl(stamped)
            if (rangeReachable(playable, client.userAgent, selected.contentLength)) {
                Log.i(
                    TAG,
                    "${client.name} resolved progressive stream itag-br=${selected.bitrate}" +
                        if (streamingPot != null) " (with pot)" else "",
                )
                return YtStreamResult(url = playable)
            }
            Log.w(
                TAG,
                "${client.name} stream fails mid-file Range probe (PO/preview gate) — keeping fallback",
            )
            if (fallbackUrl == null) fallbackUrl = playable
            lastError = "Stream blocked past preview window"
        }
        if (fallbackUrl != null) {
            Log.w(TAG, "falling back to PO-preview progressive stream")
            return YtStreamResult(url = fallbackUrl)
        }
        return YtStreamResult(error = lastError ?: "Unable to play this track")
    }

    private suspend fun mintStreamingPot(visitor: String?): String? {
        val session = poTokenSession ?: return null
        if (visitor.isNullOrBlank()) return null
        return runCatching {
            session.ensureStreamingPot(visitor).also {
                Log.i(TAG, "streaming pot ready")
            }
        }.getOrElse {
            Log.w(TAG, "streaming pot mint failed", it)
            null
        }
    }

    private fun contentLengthFromUrl(url: String): Long? =
        runCatching { android.net.Uri.parse(url).getQueryParameter("clen")?.toLongOrNull() }
            .getOrNull()

    private fun ensureClenOnUrl(url: String): String =
        YtStreamLogic.ensureClen(url, contentLengthFromUrl(url)) ?: url


    private fun requestPlayer(
        videoId: String,
        client: InnertubeClient,
        visitor: String?,
        playerPot: String? = null,
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
            if (!playerPot.isNullOrBlank()) {
                put(
                    "serviceIntegrityDimensions",
                    JSONObject().put("poToken", playerPot),
                )
            }
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

    private fun extractStream(json: JSONObject): YtStreamLogic.SelectedStream? {
        val streaming = json.optJSONObject("streamingData") ?: return null
        val formats = streaming.optJSONArray("adaptiveFormats")
            ?: streaming.optJSONArray("formats")
            ?: return null
        val audio = buildList {
            for (i in 0 until formats.length()) {
                val fmt = formats.optJSONObject(i) ?: continue
                val mime = fmt.optString("mimeType")
                if (!mime.contains("audio")) continue
                val url = fmt.optString("url").ifBlank { null }
                    ?: cipherUrl(fmt.optString("signatureCipher").ifBlank { null })
                    ?: continue
                add(
                    YtStreamLogic.AudioFormat(
                        url = url,
                        bitrate = fmt.optInt("bitrate", 0),
                        contentLength = fmt.optString("contentLength").toLongOrNull()
                            ?: fmt.optLong("contentLength").takeIf { it > 0L },
                        approxDurationMs = fmt.optString("approxDurationMs").toLongOrNull()
                            ?: fmt.optLong("approxDurationMs").takeIf { it > 0L },
                    ),
                )
            }
        }
        return YtStreamLogic.selectPlayable(audio)
    }

    /**
     * Confirms the CDN will serve past the ~1 MiB GVS PO preview window. Without this, ExoPlayer
     * plays ~64 s of a 128 kbps track and then hits 403 on the next Range request.
     */
    private fun rangeReachable(url: String, userAgent: String, contentLength: Long?): Boolean {
        val offset = YtStreamLogic.probeOffset(contentLength) ?: return true
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Range", "bytes=$offset-${offset + 500}")
            .get()
            .build()
        return try {
            http.newCall(request).execute().use { response ->
                response.isSuccessful.also { ok ->
                    if (!ok) {
                        Log.w(TAG, "Range probe HTTP ${response.code} at byte $offset")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Range probe failed at byte $offset", e)
            false
        }
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

    private fun browsePost(browseId: String): JSONObject? =
        post("browse", webContext().apply { put("browseId", browseId) }, WEB_CLIENT)

    private fun continuationPost(token: String): JSONObject? =
        post(
            "browse",
            webContext().apply { put("continuation", token) },
            WEB_CLIENT,
            continuation = token,
        )

    private fun post(
        endpoint: String,
        body: JSONObject,
        client: InnertubeClient,
        visitor: String? = null,
        continuation: String? = null,
    ): JSONObject? {
        val encodedContinuation = continuation?.let { URLEncoder.encode(it, "UTF-8") }
        val url = buildString {
            append("${client.base}/$endpoint?prettyPrint=false&key=${client.apiKey}")
            if (encodedContinuation != null) {
                append("&ctoken=").append(encodedContinuation)
                append("&continuation=").append(encodedContinuation)
            }
        }
        if (continuation != null && !body.has("continuation")) {
            body.put("continuation", continuation)
        }
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
            userAgent = IOS_UA,
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

        /** Playback HTTP default — matches the primary [IOS_CLIENT] stream mint. */
        const val IOS_UA =
            "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)"

        /** Kept for ANDROID_VR player requests; adaptive GVS URLs from this client need a PO token. */
        const val ANDROID_VR_UA =
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
            IOS_CLIENT,
            ANDROID_VR_CLIENT,
            WEB_CLIENT.copy(sendsCookies = false),
        )

        private val VISITOR_DATA_REGEX = Regex("\"visitorData\":\"(.*?)\"")
        private const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/130.0.0.0 Safari/537.36"
    }
}
