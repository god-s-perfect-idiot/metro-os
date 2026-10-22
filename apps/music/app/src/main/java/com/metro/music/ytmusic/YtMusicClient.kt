package com.metro.music.ytmusic

import android.util.Log
import com.metro.music.data.Playlist
import com.metro.music.data.Album
import com.metro.music.data.Song
import com.metro.music.playback.YtStreamPlayback
import com.metro.music.ytmusic.potoken.YtPoTokenSession
import com.metro.music.ytmusic.potoken.YtZemerPoToken
import com.metro.music.ytmusic.potoken.appendStreamPoToken
import com.zemer.cipher.potoken.PoTokenResult
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
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
    /** User-Agent that minted [url] — ExoPlayer must reuse it for googlevideo Ranges. */
    val userAgent: String? = null,
    val error: String? = null,
)

/**
 * YouTube Music Innertube client.
 *
 * Search/browse use signed-in WEB_REMIX. Playback follows Metrolist / zemer-app:
 * WEB_REMIX first (cipher + n-transform via [YtStreamUnlock]), then ANDROID_VR, then IOS HLS.
 *
 * GVS past ~1 MiB needs a **video-id-bound** `pot=` on the URL. The player request gets the
 * visitor-bound session pot in `serviceIntegrityDimensions`. IOS progressive alone dies at the
 * 1 MiB preview even with a pot — do not prefer it for full songs.
 *
 * Mobile-app player clients stay anonymous — web cookies on them yield LOGIN_REQUIRED.
 */
class YtMusicClient(
    private val authStore: YtMusicAuthStore,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val zemerPoToken: YtZemerPoToken? = YtZemerPoToken(),
    private val legacyPoTokenSession: YtPoTokenSession? = null,
) {

    fun searchSongs(query: String, limit: Int = 25): List<Song> {
        if (query.isBlank()) return emptyList()
        val body = webContext().apply {
            put("query", query)
            // Songs filter (ytmusicapi) — raw, not URL-encoded
            put("params", SEARCH_FILTER_SONGS)
        }
        val json = post("search", body, WEB_CLIENT) ?: return emptyList()
        return YtBrowseParser.parseSearch(json)
            .ifEmpty { YtBrowseParser.parse(json).songs }
            .take(limit)
    }

    fun searchAlbums(query: String, limit: Int = 25): List<Album> {
        if (query.isBlank()) return emptyList()
        val body = webContext().apply {
            put("query", query)
            put("params", SEARCH_FILTER_ALBUMS)
        }
        val json = post("search", body, WEB_CLIENT) ?: return emptyList()
        return YtBrowseParser.parseSearchAlbums(json).take(limit)
    }

    fun searchArtists(query: String, limit: Int = 10): List<YtArtistSearchHit> {
        if (query.isBlank()) return emptyList()
        val body = webContext().apply {
            put("query", query)
            put("params", SEARCH_FILTER_ARTISTS)
        }
        val json = post("search", body, WEB_CLIENT) ?: return emptyList()
        return YtBrowseParser.parseSearchArtists(json).take(limit)
    }

    /**
     * YouTube Music artist about: search → channel browse → description shelf / header art.
     * Returns null when Innertube finds no matching artist page.
     */
    fun artistAbout(artistName: String): YtArtistPageAbout? {
        val name = artistName.trim()
        if (name.isEmpty()) return null
        val hit = YtBrowseParser.pickArtistHit(searchArtists(name, limit = 8), name) ?: return null
        val page = browsePost(hit.browseId) ?: return null
        val about = YtBrowseParser.parseArtistAbout(page)
        if (about != null) {
            return about.copy(
                imageUrl = about.imageUrl ?: hit.thumbnailUrl,
            )
        }
        // Page may omit the about shelf (common for smaller artists) but still has search art.
        if (hit.thumbnailUrl.isNullOrBlank()) return null
        return YtArtistPageAbout(
            name = hit.name,
            description = null,
            imageUrl = hit.thumbnailUrl,
        )
    }

    /** Tracks on a YouTube Music album browse page (`MPREb_…`). */
    fun albumSongs(browseId: String, limit: Int = YtLibrarySync.DEFAULT_LIMIT): List<Song> {
        if (browseId.isBlank()) return emptyList()
        return YtLibrarySync.collectBrowse(
            browseId = browseId,
            fetchBrowse = ::browsePost,
            fetchContinuation = ::continuationPost,
            limit = limit,
        ).songs
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

    /**
     * YouTube Music Top Songs chart (typically 100 tracks).
     *
     * Explore (`FEmusic_explore`) exposes a Top Songs carousel whose header links to the
     * ranked chart playlist; we load that playlist with continuations. Falls back to the
     * carousel preview, then a known global Top Songs playlist id.
     */
    fun topSongs(limit: Int = TOP_SONGS_LIMIT): List<Song> {
        val explore = browsePost("FEmusic_explore")
        val playlistId = explore?.let { YtBrowseParser.parseTopSongsPlaylistId(it) }
            ?: FALLBACK_TOP_SONGS_PLAYLIST
        val fromPlaylist = playlistSongs(playlistId, limit)
        if (fromPlaylist.isNotEmpty()) {
            Log.i(TAG, "topSongs playlist=$playlistId count=${fromPlaylist.size}")
            return fromPlaylist
        }
        val preview = explore?.let { YtBrowseParser.parseExploreTopSongs(it) }.orEmpty()
        if (preview.isNotEmpty()) {
            Log.i(TAG, "topSongs explore preview count=${preview.size}")
            return preview.take(limit)
        }
        Log.w(TAG, "topSongs empty (playlist=$playlistId)")
        return emptyList()
    }

    /**
     * Billboard Hot 100 → YouTube Music search resolve (up to [limit] playable tracks).
     * Rank order follows the published chart; unresolved titles are dropped.
     */
    fun billboardHot100(limit: Int = TOP_SONGS_LIMIT): List<Song> {
        val entries = BillboardHot100.fetchChart(http, limit)
        if (entries.isEmpty()) {
            Log.w(TAG, "billboardHot100: empty chart")
            return emptyList()
        }
        val songs = BillboardHot100.resolveSongs(entries) { query, searchLimit ->
            searchSongs(query, searchLimit)
        }
        Log.i(TAG, "billboardHot100 resolved ${songs.size}/${entries.size}")
        return songs
    }

    suspend fun resolveStreamUrl(videoId: String): String? = resolveStream(videoId).url

    /**
     * Resolve a playable URL (Metrolist / zemer path).
     *
     * Never returns a URL that fails the mid-file Range probe — that is the classic ~1 min cut.
     */
    suspend fun resolveStream(videoId: String): YtStreamResult {
        if (videoId.isBlank()) return YtStreamResult(error = "Missing video id")

        var visitor = visitorData()
        val pots = mintZemerPots(videoId, visitor)
        val sessionPot = pots?.playerRequestPoToken
        val videoPot = pots?.streamingDataPoToken
        val signatureTimestamp = YtStreamUnlock.signatureTimestamp()

        var lastError: String? = null
        for (client in PLAYER_CLIENTS) {
            visitor = visitorData()
            var json = requestPlayer(
                videoId = videoId,
                client = client,
                visitor = visitor,
                playerPot = sessionPot,
                signatureTimestamp = if (client.needsSignatureTimestamp) signatureTimestamp else null,
            )
            var state = playabilityStatus(json)
            if (state == "LOGIN_REQUIRED" && visitor != null) {
                authStore.clearVisitorData()
                visitor = visitorData()
                if (visitor != null) {
                    json = requestPlayer(
                        videoId = videoId,
                        client = client,
                        visitor = visitor,
                        playerPot = sessionPot,
                        signatureTimestamp = if (client.needsSignatureTimestamp) {
                            signatureTimestamp
                        } else {
                            null
                        },
                    )
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
            if (hls != null && (client.acceptsProgressive || client.name == "IOS")) {
                Log.i(TAG, "${client.name} resolved HLS stream")
                return acceptedStream(hls, client)
            }
            if (!client.acceptsProgressive) {
                if (hls == null) {
                    Log.w(TAG, "${client.name} skipped — progressive not trusted past GVS preview")
                }
                continue
            }
            val selected = extractStream(json, videoId, decipher = client.needsCipher)
            if (selected == null) {
                lastError = "No playable audio stream"
                Log.w(TAG, "${client.name} had OK playability but no usable audio URL")
                if (client.needsCipher) YtStreamUnlock.onStreamRejected()
                continue
            }

            val candidates = buildList {
                if (!videoPot.isNullOrBlank()) {
                    add("video-pot" to appendStreamPoToken(selected.url, videoPot))
                }
                add("bare" to selected.url)
                // Session pot on the URL is usually wrong — only try after video/bare.
                if (!sessionPot.isNullOrBlank()) {
                    add("session-pot" to appendStreamPoToken(selected.url, sessionPot))
                }
            }

            for ((label, rawUrl) in candidates) {
                val playable = ensureClenOnUrl(rawUrl)
                if (rangeReachable(playable, client.userAgent, selected.contentLength)) {
                    Log.i(
                        TAG,
                        "${client.name} unlocked itag-br=${selected.bitrate} ($label)",
                    )
                    return acceptedStream(playable, client)
                }
                Log.w(TAG, "${client.name} Range probe failed ($label)")
            }
            if (client.needsCipher) YtStreamUnlock.onStreamRejected()
            lastError = "Stream blocked past preview window"
        }
        return YtStreamResult(error = lastError ?: "Unable to play this track")
    }

    private fun acceptedStream(url: String, client: InnertubeClient): YtStreamResult {
        YtStreamPlayback.register(
            url = url,
            userAgent = client.userAgent,
            origin = if (client.usesWebOrigin) MUSIC_ORIGIN else null,
            referer = if (client.usesWebOrigin) "$MUSIC_ORIGIN/" else null,
            cookie = if (client.usesWebOrigin) authStore.cookie.takeIf { it.isNotBlank() } else null,
        )
        return YtStreamResult(url = url, userAgent = client.userAgent)
    }

    private suspend fun mintZemerPots(videoId: String, visitor: String?): PoTokenResult? {
        if (visitor.isNullOrBlank()) return null
        zemerPoToken?.mint(videoId, visitor)?.let { return it }
        // Fallback: NewPipe-style WebView (metro_po_token.html) if zemer BotGuard fails.
        val legacy = legacyPoTokenSession ?: return null
        return runCatching {
            val sessionPot = legacy.ensureStreamingPot(visitor)
            val videoPot = legacy.mintPlayerPot(videoId) ?: return null
            PoTokenResult(
                playerRequestPoToken = sessionPot,
                streamingDataPoToken = videoPot,
            )
        }.getOrElse {
            Log.w(TAG, "legacy pot mint failed", it)
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
        signatureTimestamp: Int? = null,
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
            if (signatureTimestamp != null && signatureTimestamp > 0) {
                put(
                    "playbackContext",
                    JSONObject().put(
                        "contentPlaybackContext",
                        JSONObject().put("signatureTimestamp", signatureTimestamp),
                    ),
                )
            }
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

    private suspend fun extractStream(
        json: JSONObject,
        videoId: String,
        decipher: Boolean,
    ): YtStreamLogic.SelectedStream? {
        val streaming = json.optJSONObject("streamingData") ?: return null
        val formats = streaming.optJSONArray("adaptiveFormats")
            ?: streaming.optJSONArray("formats")
            ?: return null
        val audio = buildList {
            for (i in 0 until formats.length()) {
                val fmt = formats.optJSONObject(i) ?: continue
                val mime = fmt.optString("mimeType")
                if (!mime.contains("audio")) continue
                val clear = fmt.optString("url").ifBlank { null }
                val cipher = fmt.optString("signatureCipher").ifBlank { null }
                    ?: fmt.optString("cipher").ifBlank { null }
                val url = when {
                    decipher || !cipher.isNullOrBlank() ->
                        YtStreamUnlock.resolveFormatUrl(clear, cipher, videoId)
                    clear != null -> YtStreamUnlock.transformN(clear)
                    else -> null
                } ?: continue
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
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Range", "bytes=$offset-${offset + 500}")
            .get()
        // WEB_REMIX CDN often wants the music origin; cookies help when the account is signed in.
        if (userAgent.contains("Chrome") || userAgent.contains("Mozilla")) {
            builder.header("Origin", MUSIC_ORIGIN)
            builder.header("Referer", "$MUSIC_ORIGIN/")
            authStore.cookie.takeIf { it.isNotBlank() }?.let { builder.header("Cookie", it) }
        }
        return try {
            http.newCall(builder.build()).execute().use { response ->
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
        /** WEB clients need player.js STS so deciphered sig matches the player generation. */
        val needsSignatureTimestamp: Boolean = false,
        /** WEB_REMIX formats are usually signatureCipher — run zemer-cipher. */
        val needsCipher: Boolean = false,
        /**
         * IOS progressive dies at the GVS 1 MiB preview even with pot; keep false for IOS and
         * accept HLS only from that client.
         */
        val acceptsProgressive: Boolean = true,
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

        const val WEB_REMIX_UA =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/130.0.0.0 Mobile Safari/537.36"

        private val WEB_CLIENT = InnertubeClient(
            name = "WEB_REMIX",
            base = WEB_BASE,
            apiKey = "AIzaSyC9XL3ZjWddXya6QbzdlsLjKEVUhNnIfjy",
            clientNumber = "67",
            clientVersion = "1.20241028.01.00",
            userAgent = WEB_REMIX_UA,
            sendsCookies = true,
            usesWebOrigin = true,
            needsSignatureTimestamp = true,
            needsCipher = true,
            acceptsProgressive = true,
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
            // Progressive 403s past ~1 MiB; HLS (when present) still works end-to-end.
            acceptsProgressive = false,
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

        /** Metrolist order: signed-in WEB_REMIX primary, ANDROID_VR fallback, IOS HLS last. */
        private val PLAYER_CLIENTS = listOf(
            WEB_CLIENT,
            ANDROID_VR_CLIENT,
            IOS_CLIENT,
        )

        private val VISITOR_DATA_REGEX = Regex("\"visitorData\":\"(.*?)\"")
        private const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/130.0.0.0 Safari/537.36"

        /** ytmusicapi search filter — songs (ignore_spelling=false). */
        private const val SEARCH_FILTER_SONGS = "EgWKAQIIAWoMEA4QChADEAQQCRAF"

        /** ytmusicapi search filter — albums. */
        private const val SEARCH_FILTER_ALBUMS = "EgWKAQIYAWoMEA4QChADEAQQCRAF"

        /** ytmusicapi search filter — artists. */
        private const val SEARCH_FILTER_ARTISTS = "EgWKAQIgAWoMEA4QChADEAQQCRAF"

        /** Cap for the Top Songs chart list (YT Music chart playlist size). */
        const val TOP_SONGS_LIMIT = 100

        /**
         * Global Top Songs chart playlist when Explore does not yield a `VLPL…` header
         * (same id ytmusicapi documents for explore top_songs).
         */
        private const val FALLBACK_TOP_SONGS_PLAYLIST = "PL4fGSI1pDJn6O1LS0XSdF3RyO0Rq_LDeI"
    }
}
