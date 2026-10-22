package com.metro.music.playback

import android.net.Uri
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-stream HTTP identity for googlevideo playback.
 *
 * Innertube mints are bound to a client User-Agent (and for WEB_REMIX, Origin/Referer/Cookie).
 * ExoPlayer must replay those headers — a Chrome default against an IOS/ANDROID_VR URL is a
 * common cause of mid-file 403s past the ~1 MiB GVS preview window.
 */
object YtStreamPlayback {
    data class Headers(
        val userAgent: String,
        val origin: String? = null,
        val referer: String? = null,
        val cookie: String? = null,
    )

    private val byKey = ConcurrentHashMap<String, Headers>()

    fun register(
        url: String,
        userAgent: String,
        origin: String? = null,
        referer: String? = null,
        cookie: String? = null,
    ) {
        if (url.isBlank() || userAgent.isBlank()) return
        val headers = Headers(userAgent, origin, referer, cookie)
        for (key in keysFor(Uri.parse(url))) {
            byKey[key] = headers
        }
    }

    fun headersFor(uri: Uri?): Headers? {
        if (uri == null) return null
        for (key in keysFor(uri)) {
            byKey[key]?.let { return it }
        }
        return null
    }

    fun userAgentFor(uri: Uri?): String? = headersFor(uri)?.userAgent

    fun clear() {
        byKey.clear()
    }

    private fun keysFor(uri: Uri): List<String> {
        val full = uri.toString()
        val id = uri.getQueryParameter("id")?.takeIf { it.isNotBlank() }
        val mh = uri.getQueryParameter("mh")?.takeIf { it.isNotBlank() }
        return buildList {
            add(full)
            if (id != null) add("id:$id")
            if (mh != null) add("mh:$mh")
        }
    }
}
