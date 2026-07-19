package com.metro.messaging.data

import java.util.Calendar
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

    /** Conversation bubble time, e.g. `4:55pm`. */
    fun bubbleTime(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        var hour = cal.get(Calendar.HOUR)
        if (hour == 0) hour = 12
        val minute = cal.get(Calendar.MINUTE)
        val suffix = if (cal.get(Calendar.AM_PM) == Calendar.AM) "am" else "pm"
        return "$hour:${minute.toString().padStart(2, '0')}$suffix"
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

    /**
     * Builds one thread row per [SmsRow.threadId] from messages ordered newest-first.
     * Fallback when [android.provider.Telephony.Threads] is unavailable; prefer the threads
     * table over this full SMS scan. Still avoids [Telephony.Sms.Conversations], which omits
     * columns (and fails the whole query) on many devices.
     */
    fun threadsFromSmsRows(
        rows: Sequence<SmsRow>,
        displayNameFor: (String) -> String? = { null },
    ): List<ConversationThread> {
        val byThread = linkedMapOf<Long, ConversationThread>()
        for (row in rows) {
            val address = row.address?.trim().orEmpty()
            if (address.isEmpty()) continue
            val existing = byThread[row.threadId]
            if (existing == null) {
                byThread[row.threadId] = ConversationThread(
                    id = row.threadId,
                    address = address,
                    displayName = displayNameFor(address),
                    preview = previewText(row.body),
                    timestamp = row.timestamp,
                    unreadCount = if (row.read) 0 else 1,
                )
            } else if (!row.read) {
                byThread[row.threadId] = existing.copy(unreadCount = existing.unreadCount + 1)
            }
        }
        return byThread.values.toList()
    }

    data class SmsRow(
        val threadId: Long,
        val address: String?,
        val body: String,
        val timestamp: Long,
        val read: Boolean,
    )

    /**
     * Merges message lists (typically system SMS first, then local overlay).
     * Dedupes by id, then by body/direction/exact timestamp so a local overlay row does not
     * appear beside the same send once it lands in the Telephony provider (different ids).
     * Timestamps must match exactly — [MessagingRepository] persists the pending timestamp
     * into the provider.
     */
    fun mergeMessages(vararg lists: List<MessageItem>): List<MessageItem> {
        val byId = linkedMapOf<Long, MessageItem>()
        lists.flatMap { it }.forEach { message ->
            byId.putIfAbsent(message.id, message)
        }
        val merged = mutableListOf<MessageItem>()
        for (message in byId.values.sortedWith(
            compareBy<MessageItem> { it.timestamp }.thenBy { it.id },
        )) {
            val duplicate = merged.any { existing ->
                existing.threadId == message.threadId &&
                    existing.direction == message.direction &&
                    existing.body == message.body &&
                    existing.timestamp == message.timestamp
            }
            if (!duplicate) merged.add(message)
        }
        return merged
    }

    fun markThreadRead(threads: List<ConversationThread>, threadId: Long): List<ConversationThread> =
        threads.map { thread ->
            if (thread.id == threadId) thread.copy(unreadCount = 0) else thread
        }

    /**
     * Matches contacts by display-name substring or phone-number digits while composing "to".
     */
    fun contactSuggestions(
        query: String,
        contacts: List<ContactSuggestion>,
        limit: Int = 8,
    ): List<ContactSuggestion> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val lower = trimmed.lowercase()
        val digits = normalizeAddress(trimmed)
        return contacts
            .filter { contact ->
                contact.displayName.lowercase().contains(lower) ||
                    (digits.isNotEmpty() && contact.normalizedNumber.contains(digits))
            }
            .sortedWith(
                compareBy(
                    { !it.displayName.lowercase().startsWith(lower) },
                    { it.displayName.lowercase() },
                    { it.normalizedNumber },
                ),
            )
            .take(limit)
    }

    /**
     * Resolves a typed recipient to a dialable address: unique contact name match, else raw input.
     */
    fun resolveRecipientAddress(
        input: String,
        contacts: List<ContactSuggestion>,
    ): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || !trimmed.any { it.isLetter() }) return trimmed
        val lower = trimmed.lowercase()
        val exact = contacts.filter { it.displayName.equals(trimmed, ignoreCase = true) }
        if (exact.size == 1) return exact.first().phoneNumber
        val uniqueByPrefix = contacts.filter { it.displayName.lowercase().startsWith(lower) }
        if (uniqueByPrefix.size == 1) return uniqueByPrefix.first().phoneNumber
        return trimmed
    }
}
