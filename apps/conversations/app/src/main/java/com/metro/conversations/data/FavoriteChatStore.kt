package com.metro.conversations.data

import android.content.Context

/**
 * Persists favorited senders per app — package + peer title — so Conversations can
 * filter active shade chats to favorites only.
 */
class FavoriteChatStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): Set<FavoriteChat> =
        prefs.getStringSet(KEY_FAVORITES, emptySet())
            .orEmpty()
            .mapNotNull { FavoriteChat.decode(it) }
            .toSet()

    fun isFavorite(packageName: String, sender: String): Boolean {
        val needle = FavoriteChat(packageName, sender)
        return load().any { it.matches(needle) }
    }

    fun toggle(packageName: String, sender: String): Boolean {
        val next = load().toMutableSet()
        val entry = FavoriteChat(packageName, sender)
        val existing = next.firstOrNull { it.matches(entry) }
        val nowFavorite = if (existing != null) {
            next.remove(existing)
            false
        } else {
            next.add(entry)
            true
        }
        prefs.edit()
            .putStringSet(KEY_FAVORITES, next.map { it.encode() }.toSet())
            .apply()
        return nowFavorite
    }

    companion object {
        private const val PREFS = "conversations_favorites"
        private const val KEY_FAVORITES = "favorites"
    }
}

/** A favorited peer in a specific app (e.g. WhatsApp · Alice). */
data class FavoriteChat(
    val packageName: String,
    val sender: String,
) {
    fun matches(other: FavoriteChat): Boolean =
        packageName == other.packageName &&
            sender.trim().equals(other.sender.trim(), ignoreCase = true)

    fun matchesConversation(conversation: ReplyableConversation): Boolean =
        packageName == conversation.packageName &&
            sender.trim().equals(conversation.title.trim(), ignoreCase = true)

    fun encode(): String = "$packageName$SEP${sender.trim()}"

    companion object {
        private const val SEP = "\u001f"

        fun decode(raw: String): FavoriteChat? {
            val sep = raw.indexOf(SEP)
            if (sep <= 0 || sep >= raw.lastIndex) return null
            val pkg = raw.substring(0, sep).trim()
            val sender = raw.substring(sep + 1).trim()
            if (pkg.isEmpty() || sender.isEmpty()) return null
            return FavoriteChat(pkg, sender)
        }
    }
}
