package com.metro.clock.data

import android.content.Context
import org.json.JSONArray

class WorldClockStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): List<WorldClockEntry> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val id = arr.getString(i)
                    val city = WorldClockLogic.cityById(id) ?: continue
                    add(WorldClockEntry(city))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(entries: List<WorldClockEntry>) {
        val arr = JSONArray()
        entries.forEach { arr.put(it.city.id) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun add(cityId: String): List<WorldClockEntry> {
        val city = WorldClockLogic.cityById(cityId) ?: return load()
        val current = load().toMutableList()
        if (current.any { it.city.id == cityId }) return current
        current.add(WorldClockEntry(city))
        current.sortBy { it.city.name.lowercase() }
        save(current)
        return current
    }

    fun remove(ids: Set<String>): List<WorldClockEntry> {
        val next = load().filterNot { it.city.id in ids }
        save(next)
        return next
    }

    companion object {
        private const val PREFS = "metro_clock_world"
        private const val KEY = "cities_json"
    }
}
