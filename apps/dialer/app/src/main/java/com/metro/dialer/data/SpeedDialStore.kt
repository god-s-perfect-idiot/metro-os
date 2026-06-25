package com.metro.dialer.data

import android.content.Context

class SpeedDialStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): List<SpeedDialEntry> {
        return prefs.getStringSet(KEY_ENTRIES, emptySet())
            .orEmpty()
            .mapNotNull(::decodeEntry)
            .sortedBy { it.id }
    }

    fun add(entry: SpeedDialEntry) {
        val current = prefs.getStringSet(KEY_ENTRIES, emptySet()).orEmpty().toMutableSet()
        current.removeAll { decodeEntry(it)?.normalizedKey() == entry.normalizedKey() }
        current.add(encodeEntry(entry))
        prefs.edit().putStringSet(KEY_ENTRIES, current).apply()
    }

    fun remove(normalizedNumber: String) {
        val normalized = DialerCallLogic.normalizeNumber(normalizedNumber)
        val current = prefs.getStringSet(KEY_ENTRIES, emptySet()).orEmpty().toMutableSet()
        current.removeAll { decodeEntry(it)?.normalizedKey() == normalized }
        prefs.edit().putStringSet(KEY_ENTRIES, current).apply()
    }

    fun contains(normalizedNumber: String): Boolean {
        val normalized = DialerCallLogic.normalizeNumber(normalizedNumber)
        return load().any { it.normalizedKey() == normalized }
    }

    private fun SpeedDialEntry.normalizedKey(): String =
        DialerCallLogic.normalizeNumber(phoneNumber)

    private fun encodeEntry(entry: SpeedDialEntry): String =
        listOf(entry.id, entry.displayName, entry.phoneNumber).joinToString(SEPARATOR)

    private fun decodeEntry(raw: String): SpeedDialEntry? {
        val parts = raw.split(SEPARATOR)
        if (parts.size < 3) return null
        return SpeedDialEntry(
            id = parts[0],
            displayName = parts[1],
            phoneNumber = parts[2],
        )
    }

    companion object {
        private const val PREFS = "metro_dialer_speed_dial"
        private const val KEY_ENTRIES = "entries"
        private const val SEPARATOR = "\u001F"
    }
}
