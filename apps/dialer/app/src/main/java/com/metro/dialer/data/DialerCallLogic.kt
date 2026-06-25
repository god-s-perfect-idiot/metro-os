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
        return groups.filter { group ->
            group.displayName.lowercase(Locale.US).contains(lower) ||
                group.phoneNumber.contains(trimmed) ||
                group.normalizedNumber.contains(trimmed)
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
        return contacts.filter { contact ->
            contact.normalizedNumber.startsWith(normalizeNumber(queryDigits)) ||
                matchesT9(contact.displayName, queryDigits.filter { it.isDigit() })
        }.take(limit)
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
}
