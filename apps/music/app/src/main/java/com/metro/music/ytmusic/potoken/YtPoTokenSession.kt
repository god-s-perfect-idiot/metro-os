package com.metro.music.ytmusic.potoken

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.net.URLEncoder

/**
 * Session cache for YouTube PO tokens.
 *
 * Mint order matters (NewPipe): create the WebView once, mint the **streaming** pot bound to
 * [visitorData] first, then mint **player** pots bound to each videoId. GVS URLs need the
 * streaming pot as `pot=`; player requests may include the video-bound pot in
 * `serviceIntegrityDimensions.poToken`.
 */
class YtPoTokenSession(
    private val appContext: Context,
    private val okHttp: OkHttpClient,
) {
    private val lock = Mutex()
    private var webView: PoTokenWebView? = null
    private var visitorData: String? = null
    private var streamingPot: String? = null

    suspend fun ensureStreamingPot(visitorData: String): String = lock.withLock {
        if (
            streamingPot != null &&
            this.visitorData == visitorData &&
            webView != null &&
            webView?.isExpired != true
        ) {
            return streamingPot!!
        }
        resetLocked()
        this.visitorData = visitorData
        val web = withContext(Dispatchers.IO) {
            PoTokenWebView.create(appContext, okHttp)
        }
        webView = web
        // Streaming pot must be minted before any video-bound pot.
        val pot = web.generatePoToken(visitorData)
        streamingPot = pot
        Log.i(TAG, "Minted streaming pot (len=${pot.length})")
        pot
    }

    suspend fun mintPlayerPot(videoId: String): String? = lock.withLock {
        val web = webView ?: return null
        if (web.isExpired) {
            Log.w(TAG, "Integrity token expired; clearing session")
            resetLocked()
            return null
        }
        return runCatching {
            web.generatePoToken(videoId).also {
                Log.i(TAG, "Minted player pot for $videoId (len=${it.length})")
            }
        }.getOrElse {
            Log.w(TAG, "Player pot mint failed for $videoId", it)
            null
        }
    }

    fun close() {
        webView?.close()
        webView = null
        streamingPot = null
        visitorData = null
    }

    private fun resetLocked() {
        webView?.close()
        webView = null
        streamingPot = null
        visitorData = null
    }

    companion object {
        private const val TAG = "YtPoToken"
    }
}

/** Append `pot=` / `potc=1` to a googlevideo URL if missing. */
fun appendStreamPoToken(url: String, pot: String): String {
    if (pot.isBlank()) return url
    if (url.contains("pot=") || url.contains("potc=")) return url
    val sep = if (url.contains('?')) '&' else '?'
    return "$url${sep}pot=${URLEncoder.encode(pot, Charsets.UTF_8.name())}&potc=1"
}
