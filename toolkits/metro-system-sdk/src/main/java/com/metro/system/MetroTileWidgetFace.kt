package com.metro.system

import org.json.JSONObject

/**
 * Custom Start widget face — apps export kind + state; the launcher renders without loading
 * in-process UI from the source app (same pattern as [MetroTileAgenda]).
 *
 * Use with [MetroTileData.tapAction] so Start taps run the widget action instead of launching
 * the catalog app.
 */
data class MetroTileWidgetFace(
    /** One of [MetroTileWidgetFaceKind] values. */
    val kind: String,
    /** Battery charge 0–100 when [kind] is [MetroTileWidgetFaceKind.BATTERY]. */
    val batteryPercent: Int? = null,
    /**
     * Suite glyph key when [kind] is [MetroTileWidgetFaceKind.GLYPH] or
     * [MetroTileWidgetFaceKind.GLYPH_TOGGLE] — see [MetroTileWidgetGlyph].
     */
    val glyph: String? = null,
    /** Toggle / lit state for [MetroTileWidgetFaceKind.GLYPH_TOGGLE]. */
    val toggleOn: Boolean? = null,
    /** When true, glyph reads unavailable (dimmed). */
    val dimmed: Boolean? = null,
) {
    val hasContent: Boolean
        get() = kind.isNotBlank()
}

object MetroTileWidgetFaceKind {
    /** Wide digital clock — launcher ticks locally; payload need only declare the kind. */
    const val DIGITAL_CLOCK = "digital_clock"

    /** 1×1 analog dial — launcher ticks locally. */
    const val ANALOG_CLOCK = "analog_clock"

    /** 1×1 vertical battery + digits. */
    const val BATTERY = "battery"

    /** Static action glyph (e.g. lock). */
    const val GLYPH = "glyph"

    /** Toggle glyph (e.g. torch, battery saver) — white fill when [MetroTileWidgetFace.toggleOn]. */
    const val GLYPH_TOGGLE = "glyph_toggle"

    /**
     * Cycle notification peeks only (no host-app icon front). Payload is [MetroTileData.peeks]
     * (+ optional [MetroTileData.counter] badge).
     */
    const val PEEK_CYCLE = "peek_cycle"
}

object MetroTileWidgetGlyph {
    const val TORCH = "torch"
    const val LOCK = "lock"
    const val BATTERY_SAVER = "battery_saver"
}

internal object MetroTileWidgetFaceCodec {
    fun encode(face: MetroTileWidgetFace?): String? {
        if (face == null || !face.hasContent) return null
        return JSONObject().apply {
            put("kind", face.kind)
            face.batteryPercent?.let { put("battery_percent", it) }
            face.glyph?.let { put("glyph", it) }
            face.toggleOn?.let { put("toggle_on", it) }
            face.dimmed?.let { put("dimmed", it) }
        }.toString()
    }

    fun decode(raw: String?): MetroTileWidgetFace? {
        if (raw.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(raw)
            val kind = obj.optString("kind").takeIf { it.isNotBlank() } ?: return null
            MetroTileWidgetFace(
                kind = kind,
                batteryPercent = if (obj.has("battery_percent")) obj.optInt("battery_percent") else null,
                glyph = obj.optString("glyph").takeIf { it.isNotBlank() },
                toggleOn = if (obj.has("toggle_on")) obj.optBoolean("toggle_on") else null,
                dimmed = if (obj.has("dimmed")) obj.optBoolean("dimmed") else null,
            ).takeIf { it.hasContent }
        } catch (_: Exception) {
            null
        }
    }
}
