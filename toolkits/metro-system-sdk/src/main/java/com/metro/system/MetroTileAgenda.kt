package com.metro.system

import org.json.JSONArray
import org.json.JSONObject

/**
 * Agenda-style live tile payload (WP8.1 Calendar tile).
 *
 * The launcher renders, top to bottom, the event [lines] (e.g. title, location, time range), an
 * optional [footer] flush bottom-left (the app name, "Calendar"), and a large bottom-right date
 * badge built from [dayLabel] (e.g. "Thu") above [dayNumber] (e.g. "15"). Only medium (2×2) and
 * wide (4×2) tiles draw the agenda; smaller tiles fall back to the icon.
 */
data class MetroTileAgenda(
    val lines: List<String> = emptyList(),
    val dayLabel: String? = null,
    val dayNumber: String? = null,
    val footer: String? = null,
) {
    val hasContent: Boolean
        get() = lines.any { it.isNotBlank() } || !dayNumber.isNullOrBlank()
}

internal object MetroTileAgendaCodec {
    fun encode(agenda: MetroTileAgenda?): String? {
        if (agenda == null || !agenda.hasContent) return null
        val lines = JSONArray()
        agenda.lines.forEach { lines.put(it) }
        return JSONObject().apply {
            put("lines", lines)
            agenda.dayLabel?.let { put("day_label", it) }
            agenda.dayNumber?.let { put("day_number", it) }
            agenda.footer?.let { put("footer", it) }
        }.toString()
    }

    fun decode(raw: String?): MetroTileAgenda? {
        if (raw.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(raw)
            val array = obj.optJSONArray("lines") ?: JSONArray()
            val lines = buildList {
                for (i in 0 until array.length()) {
                    array.optString(i).takeIf { it.isNotBlank() }?.let { add(it) }
                }
            }
            MetroTileAgenda(
                lines = lines,
                dayLabel = obj.optString("day_label").takeIf { it.isNotBlank() },
                dayNumber = obj.optString("day_number").takeIf { it.isNotBlank() },
                footer = obj.optString("footer").takeIf { it.isNotBlank() },
            ).takeIf { it.hasContent }
        } catch (_: Exception) {
            null
        }
    }
}
