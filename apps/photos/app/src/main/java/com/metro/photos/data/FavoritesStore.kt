package com.metro.photos.data

import android.content.Context

class FavoritesStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): Set<Long> =
        prefs.getStringSet(KEY_IDS, emptySet())
            .orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()

    fun save(ids: Set<Long>) {
        prefs.edit()
            .putStringSet(KEY_IDS, ids.map { it.toString() }.toSet())
            .apply()
    }

    fun toggle(id: Long): Set<Long> {
        val current = load().toMutableSet()
        if (!current.add(id)) {
            current.remove(id)
        }
        save(current)
        return current
    }

    fun isFavorite(id: Long): Boolean = load().contains(id)

    companion object {
        private const val PREFS_NAME = "photos_favorites"
        private const val KEY_IDS = "favorite_ids"
    }
}
