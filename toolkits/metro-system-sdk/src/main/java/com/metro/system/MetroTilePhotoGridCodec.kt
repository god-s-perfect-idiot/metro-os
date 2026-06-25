package com.metro.system

import org.json.JSONArray
import org.json.JSONObject

internal object MetroTilePhotoGridCodec {
    const val COLUMN = "photo_grid"

    fun encode(grid: MetroTilePhotoGrid?): String? {
        if (grid == null || !grid.hasContent) return null
        val array = JSONArray()
        grid.cells.forEach { cell ->
            array.put(
                JSONObject().apply {
                    cell.colorHex?.let { put("color", it) }
                    cell.imageUri?.let { put("image", it) }
                },
            )
        }
        return array.toString()
    }

    fun decode(raw: String?): MetroTilePhotoGrid? {
        if (raw.isNullOrBlank()) return null
        return try {
            val array = JSONArray(raw)
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
            MetroTilePhotoGrid(cells).takeIf { it.hasContent }
        } catch (_: Exception) {
            null
        }
    }
}
