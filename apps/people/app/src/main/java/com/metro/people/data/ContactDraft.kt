package com.metro.people.data

import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Im
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Relation
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal

/** Writable local account target for a new contact. */
data class ContactAccountTarget(
    val accountName: String?,
    val accountType: String?,
    val label: String,
) {
    companion object {
        val Device = ContactAccountTarget(
            accountName = null,
            accountType = null,
            label = "Device",
        )
    }
}

/** One typed phone/email/etc. row on the new-contact form. */
data class TypedContactValue(
    val value: String = "",
    val type: Int,
    val customLabel: String = "",
)

/**
 * Form state for creating a contact via [ContactsContract].
 * Covers the common Android contact data kinds (name, photo, phone, email,
 * organization, postal, website, IM, events, notes, relation).
 */
data class ContactDraft(
    val photoUri: Uri? = null,
    val namePrefix: String = "",
    val givenName: String = "",
    val middleName: String = "",
    val familyName: String = "",
    val nameSuffix: String = "",
    val phoneticName: String = "",
    val nickname: String = "",
    val phones: List<TypedContactValue> = listOf(
        TypedContactValue(type = Phone.TYPE_MOBILE),
    ),
    val emails: List<TypedContactValue> = listOf(
        TypedContactValue(type = Email.TYPE_HOME),
    ),
    val company: String = "",
    val jobTitle: String = "",
    val department: String = "",
    val postalStreet: String = "",
    val postalCity: String = "",
    val postalRegion: String = "",
    val postalPostcode: String = "",
    val postalCountry: String = "",
    val postalType: Int = StructuredPostal.TYPE_HOME,
    val website: String = "",
    val imHandle: String = "",
    val imProtocol: Int = Im.PROTOCOL_SKYPE,
    val birthday: String = "",
    val anniversary: String = "",
    val notes: String = "",
    val relation: String = "",
    val relationType: Int = Relation.TYPE_FRIEND,
    val account: ContactAccountTarget = ContactAccountTarget.Device,
)

/** Prefill extras from Dialer / ACTION_INSERT. */
data class ContactPrefill(
    val phone: String? = null,
    val email: String? = null,
    val name: String? = null,
    val company: String? = null,
    val jobTitle: String? = null,
    val notes: String? = null,
    val postal: String? = null,
)
