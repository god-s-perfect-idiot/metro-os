package com.metro.music.ytmusic.potoken

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses BotGuard challenge / integrity payloads and converts identifiers to the JS shapes
 * expected by `assets/po_token.html` (ported from NewPipe's JavaScriptUtil).
 */
internal object PoTokenJsUtil {
    fun parseChallengeData(rawChallengeData: String): String {
        val scrambled = JSONArray(rawChallengeData)
        val challengeData = if (scrambled.length() > 1 && scrambled.opt(1) is String) {
            JSONArray(descramble(scrambled.getString(1)))
        } else {
            scrambled.getJSONArray(0)
        }

        val interpreterJs = challengeData.optJSONArray(1)?.let { arr ->
            (0 until arr.length()).firstNotNullOfOrNull { i -> arr.opt(i) as? String }
        }
        val trustedUrl = challengeData.optJSONArray(2)?.let { arr ->
            (0 until arr.length()).firstNotNullOfOrNull { i -> arr.opt(i) as? String }
        }

        return JSONObject()
            .put("messageId", challengeData.getString(0))
            .put(
                "interpreterJavascript",
                JSONObject()
                    .put("privateDoNotAccessOrElseSafeScriptWrappedValue", interpreterJs)
                    .put("privateDoNotAccessOrElseTrustedResourceUrlWrappedValue", trustedUrl),
            )
            .put("interpreterHash", challengeData.getString(3))
            .put("program", challengeData.getString(4))
            .put("globalName", challengeData.getString(5))
            .put("clientExperimentsStateBlob", challengeData.getString(7))
            .toString()
    }

    fun parseIntegrityTokenData(rawIntegrityTokenData: String): Pair<String, Long> {
        val integrityTokenData = JSONArray(rawIntegrityTokenData)
        return base64ToU8(integrityTokenData.getString(0)) to integrityTokenData.getLong(1)
    }

    fun stringToU8(identifier: String): String = newUint8Array(identifier.toByteArray())

    fun u8ToBase64(poToken: String): String {
        val bytes = poToken.split(",")
            .map { it.toUByte().toByte() }
            .toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
            .replace('+', '-')
            .replace('/', '_')
    }

    private fun descramble(scrambledChallenge: String): String =
        base64ToBytes(scrambledChallenge)
            .map { (it + 97).toByte() }
            .toByteArray()
            .decodeToString()

    private fun base64ToU8(base64: String): String = newUint8Array(base64ToBytes(base64))

    private fun newUint8Array(contents: ByteArray): String =
        "new Uint8Array([" + contents.joinToString(",") { it.toUByte().toString() } + "])"

    private fun base64ToBytes(base64: String): ByteArray {
        val normalized = base64
            .replace('-', '+')
            .replace('_', '/')
            .replace('.', '=')
        return Base64.decode(normalized, Base64.DEFAULT)
            ?: throw PoTokenException("Cannot base64 decode")
    }
}
