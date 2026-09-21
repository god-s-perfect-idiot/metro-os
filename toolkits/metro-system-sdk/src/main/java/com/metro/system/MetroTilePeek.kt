package com.metro.system

import org.json.JSONArray
import org.json.JSONObject

/**
 * One notification peek face for [MetroTileWidgetFaceKind.PEEK_CYCLE] Start tiles.
 * Launcher cycles these without showing the host app icon.
 */
data class MetroTilePeek(
    val title: String? = null,
    val subtitle: String? = null,
    val body: String? = null,
    /** Source app name (Start-style footer), not the host tile title. */
    val footer: String? = null,
) {
    val hasContent: Boolean
        get() = !title.isNullOrBlank() || !subtitle.isNullOrBlank() || !body.isNullOrBlank()
}

internal object MetroTilePeekCodec {
    fun encode(peeks: List<MetroTilePeek>?): String? {
        if (peeks.isNullOrEmpty()) return null
        val array = JSONArray()
        peeks.forEach { peek ->
            if (!peek.hasContent) return@forEach
            array.put(
                JSONObject().apply {
                    peek.title?.let { put("title", it) }
                    peek.subtitle?.let { put("subtitle", it) }
                    peek.body?.let { put("body", it) }
                    peek.footer?.let { put("footer", it) }
                },
            )
        }
        return array.takeIf { it.length() > 0 }?.toString()
    }

    fun decode(raw: String?): List<MetroTilePeek>? {
        if (raw.isNullOrBlank()) return null
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val peek = MetroTilePeek(
                        title = obj.optString("title").takeIf { it.isNotBlank() },
                        subtitle = obj.optString("subtitle").takeIf { it.isNotBlank() },
                        body = obj.optString("body").takeIf { it.isNotBlank() },
                        footer = obj.optString("footer").takeIf { it.isNotBlank() },
                    )
                    if (peek.hasContent) add(peek)
                }
            }.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }
}
