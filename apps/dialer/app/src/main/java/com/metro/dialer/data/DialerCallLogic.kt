package com.metro.dialer.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DialerCallLogic {
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

    fun groupCalls(entries: List<CallEntry>): List<CallGroup> {
        if (entries.isEmpty()) return emptyList()
        val sorted = entries.sortedByDescending { it.timestamp }
        val grouped = linkedMapOf<String, MutableList<CallEntry>>()
        sorted.forEach { entry ->
            grouped.getOrPut(entry.normalizedNumber) { mutableListOf() }.add(entry)
        }
        return grouped.map { (normalized, calls) ->
            val latest = calls.maxBy { it.timestamp }
            val displayName = calls.firstNotNullOfOrNull { it.contactName?.takeIf(String::isNotBlank) }
                ?: formatDisplayNumber(latest.phoneNumber)
            CallGroup(
                normalizedNumber = normalized,
                phoneNumber = latest.phoneNumber,
                displayName = displayName,
                latestType = latest.type,
                latestTimestamp = latest.timestamp,
                callCount = calls.size,
                calls = calls.sortedByDescending { it.timestamp },
            )
        }.sortedByDescending { it.latestTimestamp }
    }

    fun formatDisplayNumber(number: String): String {
        val digits = normalizeNumber(number)
        if (digits.length == 10) {
            return "(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
        }
        return number.trim()
    }

    /** Sentence caps for contact names on the number view header. */
    fun formatContactDisplayName(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return trimmed
        return trimmed.lowercase(Locale.US).replaceFirstChar { it.titlecase(Locale.US) }
    }

    fun primaryLabel(group: CallGroup): String {
        return if (group.callCount > 1) {
            "${group.displayName} (${group.callCount})"
        } else {
            group.displayName
        }
    }

    fun filterGroups(groups: List<CallGroup>, query: String): List<CallGroup> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return groups
        val lower = trimmed.lowercase(Locale.US)
        val queryDigits = digitsOnly(trimmed)
        return groups.filter { group ->
            group.displayName.lowercase(Locale.US).contains(lower) ||
                (queryDigits.isNotEmpty() && (
                    matchesNumberSubstring(group.normalizedNumber, trimmed) ||
                        matchesNumberSubstring(group.phoneNumber, trimmed)
                ))
        }
    }

    fun t9Key(digit: Char): String = when (digit) {
        '2' -> "abc"
        '3' -> "def"
        '4' -> "ghi"
        '5' -> "jkl"
        '6' -> "mno"
        '7' -> "pqrs"
        '8' -> "tuv"
        '9' -> "wxyz"
        else -> ""
    }

    fun matchesT9(name: String, digits: String): Boolean {
        if (digits.isEmpty()) return true
        val letters = name.lowercase(Locale.US).filter { it.isLetter() }
        if (letters.isEmpty()) return false
        var letterIndex = 0
        digits.forEach { digit ->
            val keyLetters = t9Key(digit)
            if (keyLetters.isEmpty()) return@forEach
            var matched = false
            while (letterIndex < letters.length) {
                if (keyLetters.contains(letters[letterIndex])) {
                    matched = true
                    letterIndex++
                    break
                }
                letterIndex++
            }
            if (!matched) return false
        }
        return true
    }

    fun contactSuggestions(
        queryDigits: String,
        contacts: List<ContactSuggestion>,
        limit: Int = 3,
    ): List<ContactSuggestion> {
        if (queryDigits.isEmpty()) return emptyList()
        val t9Digits = queryDigits.filter { it.isDigit() }
        return contacts.mapNotNull { contact ->
            val numberMatch = matchesNumberPrefix(contact.normalizedNumber, queryDigits) ||
                matchesNumberPrefix(contact.phoneNumber, queryDigits)
            val nameMatch = t9Digits.isNotEmpty() && matchesT9(contact.displayName, t9Digits)
            when {
                numberMatch -> contact to 0
                nameMatch -> contact to 1
                else -> null
            }
        }
            .sortedWith(
                compareBy<Pair<ContactSuggestion, Int>> { it.second }
                    .thenBy { it.first.displayName.lowercase(Locale.US) },
            )
            .map { it.first }
            .take(limit)
    }

    fun mergeContactSuggestions(
        primary: List<ContactSuggestion>,
        secondary: List<ContactSuggestion>,
        limit: Int = 3,
    ): List<ContactSuggestion> {
        val seen = linkedSetOf<String>()
        val merged = mutableListOf<ContactSuggestion>()
        for (contact in primary + secondary) {
            val key = "${digitsOnly(contact.normalizedNumber)}|${contact.displayName.lowercase(Locale.US)}"
            if (seen.add(key)) {
                merged.add(contact)
            }
            if (merged.size >= limit) break
        }
        return merged
    }

    private fun digitsOnly(raw: String): String = normalizeNumber(raw).filter { it.isDigit() }

    /** Substring match on digit sequences — used for history search. */
    fun matchesNumberSubstring(number: String, query: String): Boolean {
        val queryDigits = digitsOnly(query)
        if (queryDigits.isEmpty()) return false
        return digitsOnly(number).contains(queryDigits)
    }

    /** Prefix match on digit sequences — used for dial-pad contact suggestions. */
    fun matchesNumberPrefix(number: String, query: String): Boolean {
        val queryDigits = digitsOnly(query)
        if (queryDigits.isEmpty()) return false
        return dialingCandidates(number).any { candidate ->
            candidate.startsWith(queryDigits)
        }
    }

    /** Common stored-number shapes to compare against locally dialed prefixes. */
    internal fun dialingCandidates(number: String): List<String> {
        val digits = digitsOnly(number)
        if (digits.isEmpty()) return emptyList()
        val candidates = linkedSetOf(digits)
        if (digits.length > 10) {
            candidates += digits.takeLast(10)
        }
        if (digits.startsWith('0') && digits.length > 1) {
            candidates += digits.drop(1)
        }
        if (digits.length == 11 && digits.startsWith('1')) {
            candidates += digits.drop(1)
        }
        if (digits.length >= 12 && digits.startsWith("91")) {
            candidates += digits.drop(2)
        }
        return candidates.toList()
    }

    fun relativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val delta = (now - timestamp).coerceAtLeast(0)
        return when {
            delta < TimeUnit.MINUTES.toMillis(1) -> "just now"
            delta < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
                "$minutes m ago"
            }
            delta < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(delta)
                "$hours h ago"
            }
            delta < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(delta)
                "$days d ago"
            }
            else -> {
                val formatter = SimpleDateFormat("MMM d", Locale.getDefault())
                formatter.format(Date(timestamp))
            }
        }
    }

    fun formatDuration(seconds: Int): String {
        if (seconds <= 0) return "0:00"
        val minutes = seconds / 60
        val remainder = seconds % 60
        return "$minutes:${remainder.toString().padStart(2, '0')}"
    }

    fun formatTimestamp(timestamp: Long): String {
        val formatter = SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    fun mapCallType(androidType: Int): CallDirection = when (androidType) {
        android.provider.CallLog.Calls.INCOMING_TYPE -> CallDirection.Incoming
        android.provider.CallLog.Calls.MISSED_TYPE -> CallDirection.Missed
        else -> CallDirection.Outgoing
    }

    fun isIncomingRinging(call: ActiveCall): Boolean =
        call.direction == CallDirection.Incoming && !call.connected
}
