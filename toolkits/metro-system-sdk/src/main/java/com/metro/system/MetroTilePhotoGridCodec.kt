package com.metro.system

import org.json.JSONArray
import org.json.JSONObject

internal object MetroTilePhotoGridCodec {
    const val COLUMN = "photo_grid"

    fun encode(grid: MetroTilePhotoGrid?): String? {
        if (grid == null || !grid.hasContent) return null
        val cells = JSONArray()
        grid.cells.forEach { cell ->
            cells.put(
                JSONObject().apply {
                    cell.colorHex?.let { put("color", it) }
                    cell.imageUri?.let { put("image", it) }
                },
            )
        }
        return JSONObject().apply {
            put("cycle", grid.cycle)
            put("cells", cells)
        }.toString()
    }

    fun decode(raw: String?): MetroTilePhotoGrid? {
        if (raw.isNullOrBlank()) return null
        return try {
            // New object form: {"cycle":bool,"cells":[...]}. Legacy form: bare [...] array.
            val cycle: Boolean
            val array: JSONArray
            if (raw.trimStart().startsWith("{")) {
                val obj = JSONObject(raw)
                cycle = obj.optBoolean("cycle", false)
                array = obj.optJSONArray("cells") ?: JSONArray()
            } else {
                cycle = false
                array = JSONArray(raw)
            }
            val cells = buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        MetroTileGridCell(
                            colorHex = obj.optString("color").takeIf { it.isNotBlank() },
                            imageUri = obj.optString("image").takeIf { it.isNotBlank() },
                        ),
                    )
                }
            }
            MetroTilePhotoGrid(cells, cycle = cycle).takeIf { it.hasContent }
        } catch (_: Exception) {
            null
        }
    }
}
