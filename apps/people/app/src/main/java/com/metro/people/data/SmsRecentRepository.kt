package com.metro.people.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony

/**
 * Recent SMS/MMS touches matched to saved contacts for the People hub recent grid.
 */
class SmsRecentRepository(
    private val context: Context,
) {
    fun loadRecentMessageTouches(limit: Int = 40): List<PeopleMessageTouch> {
        val resolver = context.contentResolver
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.DATE,
            Telephony.Sms.PERSON,
        )
        val cursor = runCatching {
            resolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC",
            )
        }.getOrNull() ?: return emptyList()

        cursor.use {
            val results = mutableListOf<PeopleMessageTouch>()
            val seen = HashSet<Long>()
            while (cursor.moveToNext() && results.size < limit) {
                val address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                    ?.trim().orEmpty()
                if (address.isEmpty()) continue
                val date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val personCol = cursor.getColumnIndex(Telephony.Sms.PERSON)
                val personId = if (personCol >= 0 && !cursor.isNull(personCol)) {
                    cursor.getLong(personCol).takeIf { it > 0L }
                } else {
                    null
                }
                val lookup = personId?.let { id ->
                    ContactHit(id, lookupDisplayName(resolver, id) ?: address)
                } ?: lookupContact(resolver, address)
                if (lookup == null) continue
                if (!seen.add(lookup.contactId)) continue
                results += PeopleMessageTouch(
                    contactId = lookup.contactId,
                    displayName = lookup.displayName,
                    phoneNumber = address,
                    timestamp = date,
                )
            }
            return results
        }
    }

    private fun lookupDisplayName(resolver: ContentResolver, contactId: Long): String? {
        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId.toString())
        resolver.query(
            uri,
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)?.trim()?.takeIf { it.isNotEmpty() }
            }
        }
        return null
    }

    private fun lookupContact(resolver: ContentResolver, address: String): ContactHit? {
        val normalized = PeopleRecentLogic.normalizeNumber(address)
        for (candidate in PeopleRecentLogic.dialingCandidates(normalized)) {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(candidate),
            )
            resolver.query(
                uri,
                arrayOf(
                    ContactsContract.PhoneLookup._ID,
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(0)
                    val name = cursor.getString(1)?.trim().orEmpty()
                    if (id > 0L && name.isNotEmpty()) {
                        return ContactHit(id, name)
                    }
                }
            }
        }
        return null
    }

    private data class ContactHit(
        val contactId: Long,
        val displayName: String,
    )
}
