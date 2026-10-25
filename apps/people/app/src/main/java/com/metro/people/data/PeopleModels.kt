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
    val namePrefix: String = "",
    val givenName: String = "",
    val middleName: String = "",
    val familyName: String = "",
    val nameSuffix: String = "",
    val phoneticName: String = "",
    val nickname: String = "",
    val company: String = "",
    val jobTitle: String = "",
    val department: String = "",
    val postal: PostalAddress? = null,
    val website: String = "",
    val imHandle: String = "",
    val imProtocolLabel: String = "",
    val birthday: String = "",
    val anniversary: String = "",
    val notes: String = "",
    val relation: String = "",
    val relationLabel: String = "",
    val whatsApp: WhatsAppLink? = null,
) {
    /** True when the contact has fields beyond primary phone/email actions. */
    val hasMoreInfo: Boolean
        get() = listOf(
            namePrefix,
            middleName,
            familyName,
            nameSuffix,
            phoneticName,
            nickname,
            company,
            jobTitle,
            department,
            website,
            imHandle,
            birthday,
            anniversary,
            notes,
            relation,
        ).any { it.isNotBlank() } ||
            postal?.hasContent == true ||
            phones.size > 1 ||
            emails.size > 1
}

data class PostalAddress(
    val typeLabel: String = "home",
    val street: String = "",
    val city: String = "",
    val region: String = "",
    val postcode: String = "",
    val country: String = "",
) {
    val hasContent: Boolean
        get() = listOf(street, city, region, postcode, country).any { it.isNotBlank() }

    fun formattedLines(): List<String> = buildList {
        street.trim().takeIf { it.isNotEmpty() }?.let(::add)
        val cityLine = listOf(city, region, postcode)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ")
        if (cityLine.isNotEmpty()) add(cityLine)
        country.trim().takeIf { it.isNotEmpty() }?.let(::add)
    }
}

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

data class AccountOption(
    val id: String,
    val label: String,
    val subtitle: String? = null,
)

enum class CallDirection {
    Incoming,
    Outgoing,
    Missed,
}

enum class RecentActivityKind {
    Call,
    Message,
}

/** Raw call-log row used when building the recent grid. */
data class PeopleCallEntry(
    val id: Long,
    val phoneNumber: String,
    val normalizedNumber: String,
    val type: CallDirection,
    val timestamp: Long,
    val contactId: Long?,
    val contactName: String?,
)

/** One SMS/MMS touch used for the recent grid (saved contacts only). */
data class PeopleMessageTouch(
    val contactId: Long,
    val displayName: String,
    val phoneNumber: String,
    val timestamp: Long,
)

/**
 * One tile on the People hub **recent** grid — saved contacts only, max 10,
 * newest call or message first.
 */
data class RecentPerson(
    val contactId: Long,
    val displayName: String,
    val phoneNumber: String?,
    val photoUri: String? = null,
    val lastKind: RecentActivityKind,
    val lastTimestamp: Long,
)

/** One what's-new feed row (conversation shade post). */
data class WhatsNewItem(
    val key: String,
    val authorName: String,
    val body: String,
    val sourceLabel: String,
    val postTimeMs: Long,
    val badgeCount: Int,
    val packageName: String,
)

enum class PeoplePane {
    WhatsNew,
    Recent,
    All,
}
