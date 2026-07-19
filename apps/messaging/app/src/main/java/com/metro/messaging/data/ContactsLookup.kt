package com.metro.messaging.data

import android.content.Context
import android.provider.ContactsContract

class ContactsLookup(
    private val context: Context,
) {
    fun loadPhoneContacts(limit: Int = 500): List<ContactSuggestion> {
        val resolver = context.contentResolver
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        )
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ) ?: return emptyList()

        cursor.use {
            val seen = linkedSetOf<String>()
            val contacts = mutableListOf<ContactSuggestion>()
            while (cursor.moveToNext() && contacts.size < limit) {
                val name = cursor.getString(0)?.trim().orEmpty()
                val number = cursor.getString(1)?.trim().orEmpty()
                if (name.isEmpty() || number.isEmpty()) continue
                val normalized = MessagingLogic.normalizeAddress(number)
                if (normalized.isEmpty()) continue
                val key = "$normalized|$name"
                if (!seen.add(key)) continue
                contacts.add(
                    ContactSuggestion(
                        displayName = name,
                        phoneNumber = number,
                        normalizedNumber = normalized,
                    ),
                )
            }
            return contacts
        }
    }
}
