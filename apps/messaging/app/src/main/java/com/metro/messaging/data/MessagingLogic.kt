package com.metro.messaging.data

import java.util.concurrent.TimeUnit

object MessagingLogic {
    fun normalizeAddress(raw: String): String = raw
        .trim()
        .replace(Regex("[^+\\d]"), "")

    fun threadIdForAddress(address: String): Long {
        val normalized = normalizeAddress(address)
        return normalized.hashCode().toLong() and 0xFFFFFFFFL
    }

    fun displayLabel(thread: ConversationThread): String =
        thread.displayName?.takeIf { it.isNotBlank() }
            ?: formatAddress(thread.address)

    fun formatAddress(address: String): String {
        val digits = normalizeAddress(address)
        if (digits.length == 10) {
            return "(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
        }
        return address.trim().ifEmpty { "unknown" }
    }

    fun previewText(body: String, maxLength: Int = 48): String {
        val singleLine = body.replace('\n', ' ').trim()
        return if (singleLine.length <= maxLength) {
            singleLine
        } else {
            singleLine.take(maxLength - 1) + "…"
        }
    }

    fun relativeTime(timestamp: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val delta = (nowMillis - timestamp).coerceAtLeast(0L)
        return when {
            delta < TimeUnit.MINUTES.toMillis(1) -> "now"
            delta < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
                "${minutes}m"
            }
            delta < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(delta)
                "${hours}h"
            }
            delta < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(delta)
                "${days}d"
            }
            else -> {
                val weeks = TimeUnit.MILLISECONDS.toDays(delta) / 7
                "${weeks}w"
            }
        }
    }

    fun mergeThreads(vararg lists: List<ConversationThread>): List<ConversationThread> {
        val merged = linkedMapOf<Long, ConversationThread>()
        lists.flatMap { it }.forEach { thread ->
            val existing = merged[thread.id]
            merged[thread.id] = when {
                existing == null -> thread
                thread.timestamp >= existing.timestamp -> thread.copy(
                    unreadCount = maxOf(existing.unreadCount, thread.unreadCount),
                    displayName = thread.displayName ?: existing.displayName,
                )
                else -> existing.copy(
                    unreadCount = maxOf(existing.unreadCount, thread.unreadCount),
                    displayName = existing.displayName ?: thread.displayName,
                )
            }
        }
        return merged.values.sortedByDescending { it.timestamp }
    }

    fun mergeMessages(vararg lists: List<MessageItem>): List<MessageItem> {
        return lists
            .flatMap { it }
            .distinctBy { it.id }
            .sortedWith(compareBy<MessageItem> { it.timestamp }.thenBy { it.id })
    }

    fun markThreadRead(threads: List<ConversationThread>, threadId: Long): List<ConversationThread> =
        threads.map { thread ->
            if (thread.id == threadId) thread.copy(unreadCount = 0) else thread
        }
}
