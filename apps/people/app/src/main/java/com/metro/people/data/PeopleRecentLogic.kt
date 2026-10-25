package com.metro.people.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object PeopleRecentLogic {
    const val MaxRecentPeople = 10

    /**
     * Merge call-log + message touches into unique **saved** contacts (newest first),
     * capped at [MaxRecentPeople]. Unsaved numbers are dropped.
     */
    fun uniqueRecentPeople(
        calls: List<PeopleCallEntry>,
        messages: List<PeopleMessageTouch>,
        limit: Int = MaxRecentPeople,
    ): List<RecentPerson> {
        if (limit <= 0) return emptyList()
        data class Candidate(
            val contactId: Long,
            val displayName: String,
            val phoneNumber: String?,
            val photoUri: String?,
            val kind: RecentActivityKind,
            val timestamp: Long,
        )
        val candidates = mutableListOf<Candidate>()
        for (call in calls) {
            val contactId = call.contactId?.takeIf { it > 0L } ?: continue
            val name = call.contactName?.trim().orEmpty()
            if (name.isEmpty()) continue
            candidates += Candidate(
                contactId = contactId,
                displayName = name,
                phoneNumber = call.phoneNumber,
                photoUri = null,
                kind = RecentActivityKind.Call,
                timestamp = call.timestamp,
            )
        }
        for (message in messages) {
            if (message.contactId <= 0L) continue
            val name = message.displayName.trim()
            if (name.isEmpty()) continue
            candidates += Candidate(
                contactId = message.contactId,
                displayName = name,
                phoneNumber = message.phoneNumber,
                photoUri = null,
                kind = RecentActivityKind.Message,
                timestamp = message.timestamp,
            )
        }
        if (candidates.isEmpty()) return emptyList()
        val bestByContact = LinkedHashMap<Long, Candidate>()
        for (candidate in candidates.sortedByDescending { it.timestamp }) {
            if (candidate.contactId in bestByContact) continue
            bestByContact[candidate.contactId] = candidate
            if (bestByContact.size >= limit) break
        }
        return bestByContact.values.map { candidate ->
            RecentPerson(
                contactId = candidate.contactId,
                displayName = candidate.displayName,
                phoneNumber = candidate.phoneNumber,
                photoUri = candidate.photoUri,
                lastKind = candidate.kind,
                lastTimestamp = candidate.timestamp,
            )
        }
    }

    /** Attach photo URIs from the loaded address book onto recent tiles. */
    fun withPhotos(
        recent: List<RecentPerson>,
        contacts: List<PersonSummary>,
    ): List<RecentPerson> {
        if (recent.isEmpty() || contacts.isEmpty()) return recent
        val byId = contacts.associateBy { it.id }
        return recent.map { person ->
            val match = byId[person.contactId] ?: return@map person
            person.copy(
                photoUri = match.photoUri ?: person.photoUri,
                displayName = match.displayName.ifBlank { person.displayName },
            )
        }
    }

    /**
     * Ring-buffer merge: keep the newest [limit] unique contact ids from [existing]
     * plus [incoming], dropping the oldest when over capacity.
     */
    fun mergeMessageLog(
        existing: List<PeopleMessageTouch>,
        incoming: List<PeopleMessageTouch>,
        limit: Int = MaxRecentPeople,
    ): List<PeopleMessageTouch> {
        val byContact = LinkedHashMap<Long, PeopleMessageTouch>()
        for (touch in (existing + incoming).sortedByDescending { it.timestamp }) {
            if (touch.contactId <= 0L) continue
            if (touch.contactId in byContact) {
                val prior = byContact.getValue(touch.contactId)
                if (touch.timestamp > prior.timestamp) {
                    byContact[touch.contactId] = touch
                }
            } else {
                byContact[touch.contactId] = touch
            }
        }
        return byContact.values
            .sortedByDescending { it.timestamp }
            .take(limit)
    }

    fun normalizeNumber(raw: String): String {
        val trimmed = raw.trim()
        val builder = StringBuilder()
        trimmed.forEachIndexed { index, char ->
            when {
                char.isDigit() -> builder.append(char)
                char == '+' && index == 0 -> builder.append(char)
            }
        }
        return builder.toString()
    }

    fun formatDisplayNumber(number: String): String {
        val digits = normalizeNumber(number).removePrefix("+")
        if (digits.length == 10) {
            return "(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
        }
        return number.trim().ifEmpty { number }
    }

    fun relativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val delta = (now - timestamp).coerceAtLeast(0)
        return when {
            delta < TimeUnit.MINUTES.toMillis(1) -> "just now"
            delta < TimeUnit.SECONDS.toMillis(90) -> "just now"
            delta < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
                if (minutes <= 1) "1 minute ago" else "$minutes minutes ago"
            }
            delta < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(delta)
                if (hours <= 1) "1 hour ago" else "$hours hours ago"
            }
            delta < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(delta)
                if (days <= 1) "1 day ago" else "$days days ago"
            }
            else -> {
                val formatter = SimpleDateFormat("MMM d", Locale.getDefault())
                formatter.format(Date(timestamp))
            }
        }
    }

    fun whatsNewMeta(sourceLabel: String, postTimeMs: Long, now: Long = System.currentTimeMillis()): String =
        "$sourceLabel ${relativeTime(postTimeMs, now)}"

    fun mapCallType(androidType: Int): CallDirection = when (androidType) {
        android.provider.CallLog.Calls.INCOMING_TYPE -> CallDirection.Incoming
        android.provider.CallLog.Calls.MISSED_TYPE -> CallDirection.Missed
        else -> CallDirection.Outgoing
    }

    fun dialingCandidates(normalized: String): List<String> {
        val trimmed = normalized.trim()
        if (trimmed.isEmpty()) return emptyList()
        val digits = trimmed.removePrefix("+")
        val candidates = linkedSetOf(trimmed)
        if (digits.isNotEmpty()) {
            candidates += digits
            if (digits.length >= 10) {
                candidates += digits.takeLast(10)
            }
            if (!trimmed.startsWith("+") && digits.length >= 10) {
                candidates += "+$digits"
            }
            if (digits.startsWith("0") && digits.length > 1) {
                val withoutTrunk = digits.drop(1)
                candidates += withoutTrunk
                candidates += "+$withoutTrunk"
            }
        }
        return candidates.toList()
    }
}
