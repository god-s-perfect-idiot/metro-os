package com.metro.music.ytmusic.potoken

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
/**
 * Session cache for YouTube PO tokens.
 *
 * Mint order (zemer / Metrolist CDN-verified): create the WebView once, mint the **session** pot
 * bound to [visitorData] first (sent in `/player` `serviceIntegrityDimensions`), then mint
 * **video-id** pots for googlevideo URL `pot=`. A visitor-bound pot on the URL still 403s past
 * ~1 MiB — only the video-bound token unlocks mid-file Ranges.
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
// appendStreamPoToken lives in YtZemerPoToken.kt (Metrolist: pot= only, no potc).
