package com.metro.ui

/**
 * Pure helpers for WP8.1 find-by-letter (jump list) grouping and activation.
 *
 * Jump keys are `#` plus lowercase `a`–`z`. Non-letter starters map to `#`.
 */
object MetroJumpListLogic {
    /** Standard grid order: `#`, then `a`–`z`. */
    val LetterKeys: List<Char> = listOf('#') + ('a'..'z').toList()

    /** Total cells in the default jump grid including the locale (globe) tile. */
    const val GridCellCount: Int = 28

    /** Columns in the WP8.1 jump list overlay. */
    const val GridColumns: Int = 4

    /**
     * Sort key for a display label: `#` for empty / non A–Z starters, else lowercase letter.
     */
    fun sortKey(label: String): Char {
        val first = label.trim().firstOrNull() ?: return '#'
        val lower = first.lowercaseChar()
        return if (lower in 'a'..'z') lower else '#'
    }

    /** Normalizes any letter char to a jump key (`#` or `a`–`z`). */
    fun normalize(letter: Char): Char {
        val lower = letter.lowercaseChar()
        return if (lower in 'a'..'z') lower else '#'
    }

    /** Active jump keys present in [labels]. */
    fun activeLetters(labels: Iterable<String>): Set<Char> =
        labels.map { sortKey(it) }.toSet()

    /** Active jump keys from an existing grouped map (any casing). */
    fun activeLetters(groupedKeys: Set<Char>): Set<Char> =
        groupedKeys.map { normalize(it) }.toSet()

    fun isActive(letter: Char, active: Set<Char>): Boolean =
        normalize(letter) in activeLetters(active)

    /**
     * Whether alphabet section markers (and jump-list entry) should be shown.
     *
     * WP8.1 hides letter markers while a list search field is active — the list
     * becomes a flat filtered result with no section chrome.
     */
    fun showSectionMarkers(searchActive: Boolean): Boolean = !searchActive
}
