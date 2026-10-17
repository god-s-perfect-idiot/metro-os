package com.metro.people.data

import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Im
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Relation
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal

object ContactEditorLogic {
    data class LabeledType(val type: Int, val label: String)

    val phoneTypes: List<LabeledType> = listOf(
        LabeledType(Phone.TYPE_MOBILE, "mobile"),
        LabeledType(Phone.TYPE_HOME, "home"),
        LabeledType(Phone.TYPE_WORK, "work"),
        LabeledType(Phone.TYPE_MAIN, "main"),
        LabeledType(Phone.TYPE_WORK_MOBILE, "work mobile"),
        LabeledType(Phone.TYPE_FAX_HOME, "home fax"),
        LabeledType(Phone.TYPE_FAX_WORK, "work fax"),
        LabeledType(Phone.TYPE_PAGER, "pager"),
        LabeledType(Phone.TYPE_OTHER, "other"),
        LabeledType(Phone.TYPE_CUSTOM, "custom"),
    )

    val emailTypes: List<LabeledType> = listOf(
        LabeledType(Email.TYPE_HOME, "home"),
        LabeledType(Email.TYPE_WORK, "work"),
        LabeledType(Email.TYPE_MOBILE, "mobile"),
        LabeledType(Email.TYPE_OTHER, "other"),
        LabeledType(Email.TYPE_CUSTOM, "custom"),
    )

    val postalTypes: List<LabeledType> = listOf(
        LabeledType(StructuredPostal.TYPE_HOME, "home"),
        LabeledType(StructuredPostal.TYPE_WORK, "work"),
        LabeledType(StructuredPostal.TYPE_OTHER, "other"),
    )

    val imProtocols: List<LabeledType> = listOf(
        LabeledType(Im.PROTOCOL_SKYPE, "Skype"),
        LabeledType(Im.PROTOCOL_AIM, "AIM"),
        LabeledType(Im.PROTOCOL_MSN, "MSN"),
        LabeledType(Im.PROTOCOL_YAHOO, "Yahoo"),
        LabeledType(Im.PROTOCOL_QQ, "QQ"),
        LabeledType(Im.PROTOCOL_GOOGLE_TALK, "Google Talk"),
        LabeledType(Im.PROTOCOL_ICQ, "ICQ"),
        LabeledType(Im.PROTOCOL_JABBER, "Jabber"),
        LabeledType(Im.PROTOCOL_CUSTOM, "custom"),
    )

    val relationTypes: List<LabeledType> = listOf(
        LabeledType(Relation.TYPE_FRIEND, "friend"),
        LabeledType(Relation.TYPE_SPOUSE, "spouse"),
        LabeledType(Relation.TYPE_CHILD, "child"),
        LabeledType(Relation.TYPE_PARENT, "parent"),
        LabeledType(Relation.TYPE_BROTHER, "brother"),
        LabeledType(Relation.TYPE_SISTER, "sister"),
        LabeledType(Relation.TYPE_DOMESTIC_PARTNER, "partner"),
        LabeledType(Relation.TYPE_ASSISTANT, "assistant"),
        LabeledType(Relation.TYPE_MANAGER, "manager"),
        LabeledType(Relation.TYPE_REFERRED_BY, "referred by"),
        LabeledType(Relation.TYPE_RELATIVE, "relative"),
        LabeledType(Relation.TYPE_FATHER, "father"),
        LabeledType(Relation.TYPE_MOTHER, "mother"),
        LabeledType(Relation.TYPE_PARTNER, "partner"),
    )

    fun applyPrefill(prefill: ContactPrefill?): ContactDraft {
        if (prefill == null) return ContactDraft()
        val nameParts = splitDisplayName(prefill.name)
        val phones = if (!prefill.phone.isNullOrBlank()) {
            listOf(TypedContactValue(value = prefill.phone.trim(), type = Phone.TYPE_MOBILE))
        } else {
            listOf(TypedContactValue(type = Phone.TYPE_MOBILE))
        }
        val emails = if (!prefill.email.isNullOrBlank()) {
            listOf(TypedContactValue(value = prefill.email.trim(), type = Email.TYPE_HOME))
        } else {
            listOf(TypedContactValue(type = Email.TYPE_HOME))
        }
        return ContactDraft(
            givenName = nameParts.first,
            familyName = nameParts.second,
            phones = phones,
            emails = emails,
            company = prefill.company.orEmpty().trim(),
            jobTitle = prefill.jobTitle.orEmpty().trim(),
            notes = prefill.notes.orEmpty().trim(),
            postalStreet = prefill.postal.orEmpty().trim(),
        )
    }

    fun splitDisplayName(name: String?): Pair<String, String> {
        val trimmed = name?.trim().orEmpty()
        if (trimmed.isEmpty()) return "" to ""
        val space = trimmed.indexOf(' ')
        if (space < 0) return trimmed to ""
        return trimmed.substring(0, space).trim() to trimmed.substring(space + 1).trim()
    }

    fun composedDisplayName(draft: ContactDraft): String {
        val parts = listOf(
            draft.namePrefix,
            draft.givenName,
            draft.middleName,
            draft.familyName,
            draft.nameSuffix,
        ).map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isNotEmpty()) return parts.joinToString(" ")
        draft.nickname.trim().takeIf { it.isNotEmpty() }?.let { return it }
        draft.company.trim().takeIf { it.isNotEmpty() }?.let { return it }
        draft.phones.firstOrNull { it.value.isNotBlank() }?.value?.trim()?.let { return it }
        draft.emails.firstOrNull { it.value.isNotBlank() }?.value?.trim()?.let { return it }
        return ""
    }

    fun canSave(draft: ContactDraft): Boolean = composedDisplayName(draft).isNotBlank()

    fun normalizeBirthday(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        // Accept yyyy-MM-dd or MM/dd/yyyy loosely; Contacts Event.START_DATE prefers --MM-dd or yyyy-MM-dd.
        val iso = Regex("""^(\d{4})-(\d{2})-(\d{2})$""").matchEntire(trimmed)
        if (iso != null) return trimmed
        val slash = Regex("""^(\d{1,2})/(\d{1,2})/(\d{4})$""").matchEntire(trimmed)
        if (slash != null) {
            val month = slash.groupValues[1].padStart(2, '0')
            val day = slash.groupValues[2].padStart(2, '0')
            val year = slash.groupValues[3]
            return "$year-$month-$day"
        }
        return trimmed
    }

    fun eventTypeBirthday(): Int = Event.TYPE_BIRTHDAY

    fun eventTypeAnniversary(): Int = Event.TYPE_ANNIVERSARY
}
