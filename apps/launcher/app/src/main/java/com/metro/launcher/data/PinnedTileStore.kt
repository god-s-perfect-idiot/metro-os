package com.metro.launcher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class PinnedTileStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<PinnedTileEntry> {
        val raw = prefs.getString(KEY_PINS, null) ?: return defaultPins()
        val parsed = parsePins(raw)
        if (parsed.isEmpty()) return defaultPins()
        // Replace the pre-suite placeholder seed (IE / Notes / Music / …) with apps that exist.
        if (parsed.map { it.packageName }.toSet() == LEGACY_PLACEHOLDER_PACKAGES) {
            val migrated = defaultPins()
            save(migrated)
            return migrated
        }
        return parsed
    }

    fun save(tiles: List<PinnedTileEntry>) {
        val array = JSONArray()
        tiles.forEach { tile ->
            array.put(
                JSONObject()
                    .put("package", tile.packageName)
                    .put("tileId", tile.tileId)
                    .put("size", tile.size.storageValue)
                    .apply {
                        tile.gridCol?.let { put("col", it) }
                        tile.gridRow?.let { put("row", it) }
                    },
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
                            gridCol = obj.optInt("col", -1).takeIf { it >= 0 },
                            gridRow = obj.optInt("row", -1).takeIf { it >= 0 },
                        ),
                    )
                }
            }
        }

        /** Apps that were seeded before their packages shipped — migrate off this set. */
        private val LEGACY_PLACEHOLDER_PACKAGES = setOf(
            "com.metro.browser",
            "com.metro.notes",
            "com.metro.music",
            "com.metro.settings",
            "com.metro.store",
        )

        /** Preferred Start seed — only packages that exist under `apps/` today. */
        fun defaultPins(): List<PinnedTileEntry> = listOf(
            PinnedTileEntry("com.metro.people", size = PinnedTileSize.TwoByTwo),
            PinnedTileEntry("com.metro.photos", size = PinnedTileSize.TwoByTwo),
            PinnedTileEntry("com.metro.messaging", size = PinnedTileSize.TwoByTwo),
            PinnedTileEntry("com.metro.calendar", size = PinnedTileSize.TwoByTwo),
            PinnedTileEntry("com.metro.dialer", size = PinnedTileSize.OneByOne),
            PinnedTileEntry("com.metro.calculator", size = PinnedTileSize.OneByOne),
        )
    }
}
