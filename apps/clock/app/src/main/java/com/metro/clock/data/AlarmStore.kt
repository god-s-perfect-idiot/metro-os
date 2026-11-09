package com.metro.clock.data

import android.content.Context
import org.json.JSONArray

class AlarmStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): List<AlarmItem> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    add(AlarmItem.fromJson(arr.getJSONObject(i)))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(alarms: List<AlarmItem>) {
        val arr = JSONArray()
        alarms.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun upsert(alarm: AlarmItem): List<AlarmItem> {
        val current = load().toMutableList()
        val index = current.indexOfFirst { it.id == alarm.id }
        if (index >= 0) {
            current[index] = alarm
        } else {
            current.add(alarm)
        }
        current.sortWith(compareBy({ it.hour }, { it.minute }, { it.name }))
        save(current)
        return current
    }

    fun delete(id: String): List<AlarmItem> {
        val next = load().filterNot { it.id == id }
        save(next)
        return next
    }

    fun setEnabled(id: String, enabled: Boolean): List<AlarmItem> {
        val next = load().map { if (it.id == id) it.copy(enabled = enabled) else it }
        save(next)
        return next
    }

    companion object {
        private const val PREFS = "metro_clock_alarms"
        private const val KEY = "alarms_json"
    }
}
