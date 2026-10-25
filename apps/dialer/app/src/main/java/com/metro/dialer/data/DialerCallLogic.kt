package com.metro.dialer.data

import java.text.Normalizer
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

    /**
     * Collapses **consecutive** log entries for the same number into one history row.
     * An intervening call from another number ends the run, so the same contact can
     * appear as multiple groups (WP 8.1 Phone history behavior).
     */
    fun groupCalls(entries: List<CallEntry>): List<CallGroup> {
        if (entries.isEmpty()) return emptyList()
        val sorted = entries.sortedByDescending { it.timestamp }
        val runs = mutableListOf<MutableList<CallEntry>>()
        for (entry in sorted) {
            val current = runs.lastOrNull()
            if (current != null && numbersEquivalent(current.first().normalizedNumber, entry.normalizedNumber)) {
                current.add(entry)
            } else {
                runs.add(mutableListOf(entry))
            }
        }
        return runs.map { calls ->
            val latest = calls.first()
            val displayName = calls.firstNotNullOfOrNull { it.contactName?.takeIf(String::isNotBlank) }
                ?: formatDisplayNumber(latest.phoneNumber)
            CallGroup(
                normalizedNumber = latest.normalizedNumber,
                phoneNumber = latest.phoneNumber,
                displayName = displayName,
                latestType = latest.type,
                latestTimestamp = latest.timestamp,
                callCount = calls.size,
                calls = calls,
            )
        }
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

    /**
     * WP-style smart dial: consecutive T9 letters from the start of the display name,
     * from the start of any word (so `76` matches "John Smith"), initials across words
     * (`57` → John Smith), or the compacted name without spaces.
     */
    fun matchesT9(name: String, digits: String): Boolean {
        val t9Digits = digits.filter { it in '2'..'9' }
        if (t9Digits.isEmpty()) return false
        val words = t9Words(name)
        if (words.isEmpty()) return false
        if (words.any { wordMatchesT9Prefix(it, t9Digits) }) return true
        if (matchesT9Initials(words, t9Digits)) return true
        val compacted = words.joinToString("")
        return wordMatchesT9Prefix(compacted, t9Digits)
    }

    /** Lowercase ASCII letters only, diacritics stripped — "Zakku" / "Zákkú" → zakku. */
    internal fun t9Words(name: String): List<String> {
        val folded = Normalizer.normalize(name.lowercase(Locale.US), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        return folded.split(Regex("[^a-z]+")).filter { it.isNotEmpty() }
    }

    private fun wordMatchesT9Prefix(letters: String, digits: String): Boolean {
        if (digits.length > letters.length) return false
        digits.forEachIndexed { index, digit ->
            val keyLetters = t9Key(digit)
            if (keyLetters.isEmpty() || !keyLetters.contains(letters[index])) return false
        }
        return true
    }

    /** Match digit sequence to first letters of consecutive words (WP smart-dial initials). */
    private fun matchesT9Initials(words: List<String>, digits: String): Boolean {
        if (digits.length < 2 || digits.length > words.size) return false
        digits.forEachIndexed { index, digit ->
            val keyLetters = t9Key(digit)
            if (keyLetters.isEmpty() || !keyLetters.contains(words[index].first())) return false
        }
        return true
    }

    /**
     * Ranked contact suggestions for the dial pad.
     * Number-prefix hits rank above T9 name hits; within each tier, longer / earlier matches win.
     * Default [limit] is high so the dial-pad list can scroll through many matches.
     */
    fun contactSuggestions(
        queryDigits: String,
        contacts: List<ContactSuggestion>,
        limit: Int = 50,
    ): List<ContactSuggestion> {
        if (queryDigits.isEmpty()) return emptyList()
        val t9Digits = queryDigits.filter { it in '2'..'9' }
        val queryLen = digitsOnly(queryDigits).length
        return contacts.mapNotNull { contact ->
            val numberMatch = matchesNumberPrefix(contact.normalizedNumber, queryDigits) ||
                matchesNumberPrefix(contact.phoneNumber, queryDigits)
            val nameMatch = t9Digits.isNotEmpty() && matchesT9(contact.displayName, t9Digits)
            when {
                numberMatch -> contact to SuggestionRank(
                    tier = 0,
                    // Prefer contacts whose national number is closer in length to the query.
                    specificity = -queryLen,
                )
                nameMatch -> contact to SuggestionRank(
                    tier = 1,
                    specificity = t9NameSpecificity(contact.displayName, t9Digits),
                )
                else -> null
            }
        }
            .sortedWith(
                compareBy<Pair<ContactSuggestion, SuggestionRank>> { it.second.tier }
                    .thenBy { it.second.specificity }
                    .thenBy { it.first.displayName.lowercase(Locale.US) }
                    .thenBy { it.first.normalizedNumber }
                    .thenBy { it.first.contactId },
            )
            .map { it.first }
            .take(limit)
    }

    private data class SuggestionRank(val tier: Int, val specificity: Int)

    /** Lower is better — first-word / initials matches outrank later-word-only hits. */
    private fun t9NameSpecificity(name: String, t9Digits: String): Int {
        val words = t9Words(name)
        if (words.isEmpty()) return 100
        if (wordMatchesT9Prefix(words.first(), t9Digits)) return 0
        if (matchesT9Initials(words, t9Digits)) return 1
        if (wordMatchesT9Prefix(words.joinToString(""), t9Digits)) return 2
        val laterIndex = words.indexOfFirst { wordMatchesT9Prefix(it, t9Digits) }
        return if (laterIndex >= 0) 3 + laterIndex else 50
    }

    fun mergeContactSuggestions(
        primary: List<ContactSuggestion>,
        secondary: List<ContactSuggestion>,
        limit: Int = 50,
    ): List<ContactSuggestion> {
        val seen = linkedSetOf<String>()
        val merged = mutableListOf<ContactSuggestion>()
        for (contact in primary + secondary) {
            // Keep distinct contacts even when display names match.
            if (seen.add(contact.identityKey)) {
                merged.add(contact)
            }
            if (merged.size >= limit) break
        }
        return merged
    }

    /** Keep PhoneLookup hits that are real dial-prefix matches (not fuzzy name noise). */
    fun numberPrefixMatches(
        queryDigits: String,
        contacts: List<ContactSuggestion>,
    ): List<ContactSuggestion> {
        if (queryDigits.isEmpty()) return emptyList()
        return contacts.filter { contact ->
            matchesNumberPrefix(contact.normalizedNumber, queryDigits) ||
                matchesNumberPrefix(contact.phoneNumber, queryDigits)
        }
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
        val queryCandidates = dialingCandidates(query)
        if (queryCandidates.isEmpty()) return false
        val numberCandidates = dialingCandidates(number)
        // Compare every dialing shape of the stored number against every shape of the
        // query so +91 / 91 / 0 / 00 prefixes all resolve to the same contact.
        return numberCandidates.any { numCand ->
            queryCandidates.any { qCand -> numCand.startsWith(qCand) }
        }
    }

    /**
     * True when two dial strings refer to the same line after stripping trunk / IDD /
     * country-code prefixes (e.g. `+9198…`, `9198…`, `0987…`).
     */
    fun numbersEquivalent(a: String, b: String): Boolean {
        val aDigits = digitsOnly(a)
        val bDigits = digitsOnly(b)
        if (aDigits.isEmpty() || bDigits.isEmpty()) return false
        if (aDigits == bDigits) return true
        val aCands = dialingCandidates(a)
        val bCands = dialingCandidates(b)
        // Require a shared national-length candidate so short prefixes ("91") don't
        // collapse unrelated numbers.
        return aCands.any { ac ->
            ac.length >= MIN_EQUIVALENT_DIGITS && bCands.contains(ac)
        }
    }

    /**
     * Find a contact whose stored number matches [phoneNumber] across +CC / trunk / national
     * forms. Used when placing a call from the dial pad and for in-call name resolution.
     */
    fun findContactByNumber(
        phoneNumber: String,
        contacts: List<ContactSuggestion>,
    ): ContactSuggestion? {
        if (phoneNumber.isBlank() || contacts.isEmpty()) return null
        return contacts.firstOrNull { contact ->
            numbersEquivalent(contact.phoneNumber, phoneNumber) ||
                numbersEquivalent(contact.normalizedNumber, phoneNumber)
        }
    }

    /** Contact display name for [phoneNumber], or null when no address-book hit. */
    fun resolveContactDisplayName(
        phoneNumber: String,
        contacts: List<ContactSuggestion>,
    ): String? {
        val name = findContactByNumber(phoneNumber, contacts)?.displayName?.trim().orEmpty()
        if (name.isEmpty() || isPhoneNumberLabel(name, phoneNumber)) return null
        return name
    }

    /**
     * Prefer a real contact name over a formatted number when binding in-call UI.
     * [preferred] is typically the label already shown from the dial pad / T9 path;
     * [fallback] is a later lookup (call log / PhoneLookup).
     */
    fun preferContactDisplayName(
        preferred: String?,
        fallback: String?,
        phoneNumber: String,
    ): String {
        preferred?.takeIf { it.isNotBlank() && !isPhoneNumberLabel(it, phoneNumber) }?.let {
            return it
        }
        fallback?.takeIf { it.isNotBlank() && !isPhoneNumberLabel(it, phoneNumber) }?.let {
            return it
        }
        return formatDisplayNumber(phoneNumber)
    }

    /**
     * True when [label] is essentially the dialed number (raw or formatted), not a
     * contact display name. Digit-only labels that do not match [phoneNumber] stay names
     * (e.g. a contact literally named "911").
     */
    fun isPhoneNumberLabel(label: String, phoneNumber: String): Boolean {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return true
        if (trimmed.any { it.isLetter() }) return false
        val labelDigits = digitsOnly(trimmed)
        if (labelDigits.isEmpty()) return false
        return numbersEquivalent(trimmed, phoneNumber) ||
            trimmed == formatDisplayNumber(phoneNumber)
    }

    /**
     * Dialing shapes for a number: full digits, IDD/trunk-stripped forms, country-code
     * stripped national numbers, and a trailing 10-digit suffix.
     */
    internal fun dialingCandidates(number: String): List<String> {
        val digits = digitsOnly(number)
        if (digits.isEmpty()) return emptyList()
        val candidates = linkedSetOf(digits)

        fun add(value: String) {
            if (value.isNotEmpty()) candidates += value
        }

        // Walk current candidates and peel common outer prefixes.
        var expanded = true
        while (expanded) {
            expanded = false
            val snapshot = candidates.toList()
            for (base in snapshot) {
                // International access code (00…)
                if (base.startsWith("00") && base.length > 2) {
                    val stripped = base.drop(2)
                    if (candidates.add(stripped)) expanded = true
                }
                // Trunk prefix (0…) — not "00" (handled above)
                if (base.startsWith('0') && !base.startsWith("00") && base.length > 1) {
                    val stripped = base.drop(1)
                    if (candidates.add(stripped)) expanded = true
                }
                // Country codes (longest first) when the string is longer than a
                // typical national number — avoids treating 98… mobiles as +98.
                if (base.length > 10) {
                    for (cc in COUNTRY_CODES) {
                        if (!base.startsWith(cc) || base.length <= cc.length) continue
                        val rest = base.drop(cc.length)
                        if (rest.length in MIN_NATIONAL_DIGITS..MAX_NATIONAL_DIGITS) {
                            if (candidates.add(rest)) expanded = true
                        }
                    }
                }
            }
        }

        // Loose national suffix for contacts stored with an unrecognized country code.
        for (base in candidates.toList()) {
            if (base.length > 10) add(base.takeLast(10))
        }
        return candidates.toList()
    }

    /** Longer codes first so e.g. 971 is not misread as 9 + 71. */
    private val COUNTRY_CODES = listOf(
        "971", "966", "965", "964", "963", "962", "961", "968", "974", "973",
        "880", "886", "852", "853", "855", "856",
        "234", "233", "254", "255", "256",
        "44", "49", "33", "39", "34", "31", "32", "36", "40", "41", "43", "46", "47", "48",
        "61", "62", "63", "64", "65", "66", "81", "82", "84", "86", "90", "91", "92", "93",
        "94", "95", "98", "20", "27", "30", "52", "53", "54", "55", "56", "57", "58",
        "1",
    )

    private const val MIN_NATIONAL_DIGITS = 7
    private const val MAX_NATIONAL_DIGITS = 12
    private const val MIN_EQUIVALENT_DIGITS = 7

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
