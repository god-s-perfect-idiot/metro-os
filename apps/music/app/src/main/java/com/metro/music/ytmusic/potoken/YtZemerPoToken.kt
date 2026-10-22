package com.metro.music.ytmusic.potoken

import android.util.Log
import com.zemer.cipher.potoken.PoTokenGenerator
import com.zemer.cipher.potoken.PoTokenResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URLEncoder

/**
 * Metrolist / zemer BotGuard pots (CDN-verified binding).
 *
 * - [PoTokenResult.playerRequestPoToken] = visitor/session → `/player` body
 * - [PoTokenResult.streamingDataPoToken] = videoId → googlevideo `pot=`
 */
class YtZemerPoToken {
    private val generator = PoTokenGenerator()

    suspend fun mint(videoId: String, sessionId: String): PoTokenResult? {
        if (videoId.isBlank() || sessionId.isBlank()) return null
        return withTimeoutOrNull(TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                runCatching {
                    generator.getWebClientPoToken(videoId, sessionId)?.also {
                        Log.i(
                            TAG,
                            "zemer pot ready videoLen=${it.streamingDataPoToken.length} " +
                                "sessionLen=${it.playerRequestPoToken.length}",
                        )
                    }
                }.getOrElse {
                    Log.w(TAG, "zemer pot mint failed", it)
                    null
                }
            }
        }.also {
            if (it == null) Log.w(TAG, "zemer pot mint timed out or failed")
        }
    }

    companion object {
        private const val TAG = "YtZemerPoToken"
        private const val TIMEOUT_MS = 10_000L
    }
}

/** Append `pot=` only (Metrolist / zemer — no `potc=`). */
fun appendStreamPoToken(url: String, pot: String): String {
    if (pot.isBlank()) return url
    if (url.contains("pot=")) return url
    val sep = if (url.contains('?')) '&' else '?'
    return "$url${sep}pot=${URLEncoder.encode(pot, Charsets.UTF_8.name())}"
}
