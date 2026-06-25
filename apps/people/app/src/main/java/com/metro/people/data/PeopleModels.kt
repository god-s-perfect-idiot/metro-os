package com.metro.people.data

data class PersonSummary(
    val id: Long,
    val displayName: String,
    val photoUri: String?,
    val hasPhone: Boolean,
    val defaultPhone: String?,
    val defaultEmail: String?,
    val sourceLabel: String,
    val sortKey: Char,
)

data class PersonDetail(
    val summary: PersonSummary,
    val phones: List<ContactMethod>,
    val emails: List<ContactMethod>,
)

data class ContactMethod(
    val type: ContactMethodType,
    val label: String,
    val value: String,
)

enum class ContactMethodType {
    Phone,
    Email,
}

data class PeopleFilter(
    val hideWithoutPhone: Boolean = true,
    val visibleAccounts: Set<String> = emptySet(),
)

data class SocialPost(
    val id: String,
    val authorName: String,
    val body: String,
    val source: String,
    val relativeTime: String,
    val commentCount: Int = 0,
)

data class AccountOption(
    val id: String,
    val label: String,
    val subtitle: String? = null,
)
