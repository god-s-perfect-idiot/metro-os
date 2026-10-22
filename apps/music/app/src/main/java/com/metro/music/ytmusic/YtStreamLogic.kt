package com.metro.music.ytmusic

import android.net.Uri

/**
 * Picks and sanitises googlevideo audio URLs from an Innertube player response.
 *
 * Adaptive URLs 403 past ~1 MiB without a **video-id-bound** GVS `pot=` — truncates near
 * ~64 s at 128 kbps. Visitor-bound pots do not unlock mid-file Ranges (zemer CDN probes).
 * IOS progressive dies at that preview even with a pot; prefer WEB_REMIX (cipher + n-transform)
 * or ANDROID_VR. [selectPlayable] rejects payloads whose advertised length is the classic
 * ~1 MiB preview for a long track.
 */
object YtStreamLogic {
    /** Soft ceiling used by PO-preview / capped responses (~1 MiB). */
    const val TRUNCATED_STREAM_MAX_BYTES = 1_100_000L

    /** Tracks shorter than this may legitimately fit under the truncated ceiling. */
    const val SHORT_TRACK_MAX_MS = 90_000L

    /**
     * Byte offset past the PO-token preview window. A successful Range GET here means the CDN
     * will serve the rest of the file.
     */
    const val PO_PREVIEW_PROBE_BYTES = 1_500_000L

    data class AudioFormat(
        val url: String,
        val bitrate: Int,
        val contentLength: Long? = null,
        val approxDurationMs: Long? = null,
    )

    data class SelectedStream(
        val url: String,
        val contentLength: Long?,
        val approxDurationMs: Long?,
        val bitrate: Int,
    )

    /**
     * Best non-truncated audio URL, with `clen` stamped onto the URI when Innertube advertised a
     * content length but the signed URL omitted it.
     */
    fun selectPlayable(formats: List<AudioFormat>): SelectedStream? {
        val ranked = formats
            .filter { it.url.isNotBlank() }
            .sortedByDescending { it.bitrate }
        for (format in ranked) {
            if (isTruncated(format.contentLength, format.approxDurationMs)) continue
            val url = ensureClen(format.url, format.contentLength) ?: continue
            return SelectedStream(
                url = url,
                contentLength = format.contentLength,
                approxDurationMs = format.approxDurationMs,
                bitrate = format.bitrate,
            )
        }
        return null
    }

    /**
     * True when the CDN length is the ~1 MiB preview and the track is long enough that a real
     * encode would be larger.
     */
    fun isTruncated(contentLength: Long?, approxDurationMs: Long?): Boolean {
        if (contentLength == null || contentLength <= 0L) return false
        if (contentLength > TRUNCATED_STREAM_MAX_BYTES) return false
        val duration = approxDurationMs
        if (duration != null) return duration > SHORT_TRACK_MAX_MS
        // No duration: treat the classic ~1 MiB cap as truncated.
        return contentLength in 900_000L..TRUNCATED_STREAM_MAX_BYTES
    }

    /**
     * Offset to Range-probe for PO-token gating, or null when the whole object fits inside the
     * preview window (short track) and no mid-file check is needed.
     */
    fun probeOffset(contentLength: Long?): Long? {
        if (contentLength == null || contentLength <= 0L) return PO_PREVIEW_PROBE_BYTES
        if (contentLength <= TRUNCATED_STREAM_MAX_BYTES) return null
        return minOf(PO_PREVIEW_PROBE_BYTES, contentLength / 2)
    }

    /** Appends `clen` when missing so [com.metro.music.playback.ChunkedDataSource] can chunk. */
    fun ensureClen(url: String, contentLength: Long?): String? {
        if (url.isBlank()) return null
        if (contentLength == null || contentLength <= 0L) return url
        val uri = Uri.parse(url)
        if (!uri.getQueryParameter("clen").isNullOrBlank()) return url
        return uri.buildUpon()
            .appendQueryParameter("clen", contentLength.toString())
            .build()
            .toString()
    }
}
