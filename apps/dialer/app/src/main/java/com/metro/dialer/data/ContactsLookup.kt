package com.metro.dialer.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract

class ContactsLookup(
    private val context: Context,
) {
    fun loadPhoneContacts(limit: Int = 2000): List<ContactSuggestion> {
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
            val results = linkedSetOf<String>()
            val contacts = mutableListOf<ContactSuggestion>()
            while (cursor.moveToNext() && contacts.size < limit) {
                val name = cursor.getString(0)?.trim().orEmpty()
                val number = cursor.getString(1)?.trim().orEmpty()
                if (number.isEmpty()) continue
                val normalized = DialerCallLogic.normalizeNumber(number)
                val displayName = name.ifEmpty { DialerCallLogic.formatDisplayNumber(number) }
                val key = "$normalized|$displayName"
                if (!results.add(key)) continue
                contacts.add(
                    ContactSuggestion(
                        displayName = displayName,
                        phoneNumber = number,
                        normalizedNumber = normalized,
                    ),
                )
            }
            return contacts
        }
    }

    /** Live lookup via Android PhoneLookup — matches partial dialed numbers reliably. */
    fun findMatchingContacts(query: String, limit: Int = 10): List<ContactSuggestion> {
        val normalized = DialerCallLogic.normalizeNumber(query)
        if (normalized.none { it.isDigit() }) return emptyList()

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(normalized),
        )
        val projection = arrayOf(
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.NUMBER,
        )
        val cursor = context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            ContactsContract.PhoneLookup.DISPLAY_NAME,
        ) ?: return emptyList()

        cursor.use {
            val seen = linkedSetOf<String>()
            val contacts = mutableListOf<ContactSuggestion>()
            while (cursor.moveToNext() && contacts.size < limit) {
                val name = cursor.getString(0)?.trim().orEmpty()
                val number = cursor.getString(1)?.trim().orEmpty()
                if (number.isEmpty()) continue
                val normalizedNumber = DialerCallLogic.normalizeNumber(number)
                val displayName = name.ifEmpty { DialerCallLogic.formatDisplayNumber(number) }
                val key = "$normalizedNumber|$displayName"
                if (!seen.add(key)) continue
                contacts.add(
                    ContactSuggestion(
                        displayName = displayName,
                        phoneNumber = number,
                        normalizedNumber = normalizedNumber,
                    ),
                )
            }
            return contacts
        }
    }

    /** Resolve a contacts provider id for pin-to-Start secondary tiles. */
    fun resolveContactId(phoneNumber: String): Long? {
        val normalized = DialerCallLogic.normalizeNumber(phoneNumber)
        if (normalized.isEmpty()) return null
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(normalized),
        )
        val projection = arrayOf(ContactsContract.PhoneLookup._ID)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        return null
    }

    /** Contact photo URI for [phoneNumber], or null when unresolved / no photo. */
    fun resolvePhotoUri(phoneNumber: String): Uri? {
        val normalized = DialerCallLogic.normalizeNumber(phoneNumber)
        if (normalized.isEmpty()) return null
        val lookupUri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(normalized),
        )
        val photoUriString = context.contentResolver.query(
            lookupUri,
            arrayOf(ContactsContract.PhoneLookup.PHOTO_URI),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: return null
        return runCatching { Uri.parse(photoUriString) }.getOrNull()
    }

    /** Decode the contact photo for [phoneNumber], or null when missing. */
    fun loadContactPhoto(phoneNumber: String): Bitmap? {
        val photoUri = resolvePhotoUri(phoneNumber) ?: return null
        return runCatching {
            context.contentResolver.openInputStream(photoUri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
    }
}
