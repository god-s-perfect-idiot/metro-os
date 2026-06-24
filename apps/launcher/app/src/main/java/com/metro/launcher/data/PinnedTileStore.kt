package com.metro.launcher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class PinnedTileStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<PinnedTileEntry> {
        val raw = prefs.getString(KEY_PINS, null) ?: return defaultPins()
        return parsePins(raw).ifEmpty { defaultPins() }
    }

    fun save(tiles: List<PinnedTileEntry>) {
        val array = JSONArray()
        tiles.forEach { tile ->
            array.put(
                JSONObject()
                    .put("package", tile.packageName)
                    .put("tileId", tile.tileId)
                    .put("size", tile.size.storageValue),
            )
        }
        prefs.edit().putString(KEY_PINS, array.toString()).apply()
    }

    fun wideTilesEnabled(): Boolean = prefs.getBoolean(KEY_WIDE_TILES, false)

    fun setWideTilesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WIDE_TILES, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "metro_launcher"
        private const val KEY_PINS = "pinned_tiles"
        private const val KEY_WIDE_TILES = "wide_tiles_enabled"

        fun parsePins(raw: String): List<PinnedTileEntry> {
            val array = JSONArray(raw)
            return buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        PinnedTileEntry(
                            packageName = obj.getString("package"),
                            tileId = obj.optString("tileId", "primary"),
                            size = PinnedTileSize.fromStorage(obj.optString("size", "medium")),
                        ),
                    )
                }
            }
        }

        fun defaultPins(): List<PinnedTileEntry> = listOf(
            PinnedTileEntry("com.metro.browser", size = PinnedTileSize.TwoByTwo),
            PinnedTileEntry("com.metro.notes", size = PinnedTileSize.TwoByTwo),
            PinnedTileEntry("com.metro.music", size = PinnedTileSize.FourByTwo),
            PinnedTileEntry("com.metro.settings", size = PinnedTileSize.OneByOne),
            PinnedTileEntry("com.metro.store", size = PinnedTileSize.OneByOne),
        )
    }
}
