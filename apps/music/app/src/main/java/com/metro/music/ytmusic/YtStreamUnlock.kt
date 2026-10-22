package com.metro.music.ytmusic

import android.util.Log
import com.zemer.cipher.CipherDeobfuscator
import java.net.URLDecoder

/**
 * Metrolist / zemer-cipher helpers for WEB_REMIX stream URLs.
 *
 * WEB_REMIX returns ciphered formats; without sig + n-transform the CDN either rejects the URL
 * or throttles. Video-bound `pot=` (handled by [YtMusicClient]) unlocks Ranges past ~1 MiB.
 */
internal object YtStreamUnlock {
    private const val TAG = "YtStreamUnlock"

    /**
     * Resolve a clear googlevideo URL from an adaptive format's `url` or `signatureCipher`.
     * Applies n-transform when the cipher path (or a clear URL with `n=`) needs it.
     */
    suspend fun resolveFormatUrl(
        clearUrl: String?,
        signatureCipher: String?,
        videoId: String,
    ): String? {
        val fromCipher = signatureCipher?.takeIf { it.isNotBlank() }?.let { cipher ->
            runCatching { CipherDeobfuscator.deobfuscateStreamUrl(cipher, videoId) }
                .onFailure { Log.w(TAG, "cipher deobfuscate failed for $videoId", it) }
                .getOrNull()
                ?: clearCipherUrl(cipher)
        }
        val base = clearUrl?.takeIf { it.isNotBlank() } ?: fromCipher ?: return null
        return transformN(base)
    }

    suspend fun transformN(url: String): String {
        if (!url.contains("n=")) return url
        return runCatching { CipherDeobfuscator.transformNParamInUrl(url) }
            .onFailure { Log.w(TAG, "n-transform failed", it) }
            .getOrDefault(url)
    }

    suspend fun signatureTimestamp(): Int? =
        runCatching { CipherDeobfuscator.signatureTimestamp() }
            .onFailure { Log.w(TAG, "signatureTimestamp failed", it) }
            .getOrNull()

    suspend fun onStreamRejected() {
        runCatching { CipherDeobfuscator.onStreamRejected() }
            .onFailure { Log.w(TAG, "cipher config refresh failed", it) }
    }

    /** Rare clear `url=` segment with no `s=` / `sig=` — usable without JS. */
    private fun clearCipherUrl(cipher: String): String? {
        val parts = cipher.split("&").associate { pair ->
            val idx = pair.indexOf('=')
            if (idx < 0) "" to pair
            else pair.substring(0, idx) to URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
        }
        return parts["url"]?.takeIf { !parts.containsKey("s") && !parts.containsKey("sig") }
    }
}
