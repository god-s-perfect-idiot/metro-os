package com.metro.people.data

object PeopleContactsLogic {
    fun sortKeyFor(name: String, sortByLastName: Boolean): Char {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return '#'
        val keyName = if (sortByLastName) {
            trimmed.substringAfterLast(' ', trimmed)
        } else {
            trimmed.substringBefore(' ')
        }
        val first = keyName.firstOrNull()?.uppercaseChar() ?: '#'
        return if (first in 'A'..'Z') first else '#'
    }

    fun groupBySortKey(
        people: List<PersonSummary>,
        sortByLastName: Boolean = false,
    ): Map<Char, List<PersonSummary>> =
        people
            .sortedWith(
                compareBy<PersonSummary> { sortKeyFor(it.displayName, sortByLastName) }
                    .thenBy { it.displayName.lowercase() },
            )
            .groupBy { sortKeyFor(it.displayName, sortByLastName) }
            .toSortedMap()

    fun applyFilter(
        people: List<PersonSummary>,
        filter: PeopleFilter,
        allAccounts: Set<String>,
    ): List<PersonSummary> {
        val accounts = filter.visibleAccounts.ifEmpty { allAccounts }
        return people.filter { person ->
            val accountOk = person.sourceLabel in accounts
            val phoneOk = !filter.hideWithoutPhone || person.hasPhone
            accountOk && phoneOk
        }
    }

    fun filterLabel(filter: PeopleFilter): String =
        if (filter.hideWithoutPhone) {
            "showing only contacts with phone numbers"
        } else {
            "showing all contacts"
        }

    fun jumpLetters(people: List<PersonSummary>, sortByLastName: Boolean = false): List<Char> =
        groupBySortKey(people, sortByLastName).keys.toList()
}
