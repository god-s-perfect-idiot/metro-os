package com.metro.people.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract

/**
 * People-local call-log reader for the hub **recent** pane.
 * Does not import Dialer types (no cross-app dependency).
 */
class CallLogRepository(
    private val context: Context,
) {
    fun loadRecentCalls(limit: Int = 200): List<PeopleCallEntry> {
        val resolver = context.contentResolver
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
        )
        val sort = "${CallLog.Calls.DATE} DESC"
        val cursor = resolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            sort,
        ) ?: return emptyList()

        cursor.use {
            val entries = mutableListOf<PeopleCallEntry>()
            while (cursor.moveToNext() && entries.size < limit) {
                entries.add(cursor.toPeopleCallEntry(resolver))
            }
            return entries
        }
    }

    private fun Cursor.toPeopleCallEntry(resolver: ContentResolver): PeopleCallEntry {
        val id = getLong(getColumnIndexOrThrow(CallLog.Calls._ID))
        val number = getString(getColumnIndexOrThrow(CallLog.Calls.NUMBER)) ?: ""
        val cachedName = getString(getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME))
        val type = getInt(getColumnIndexOrThrow(CallLog.Calls.TYPE))
        val date = getLong(getColumnIndexOrThrow(CallLog.Calls.DATE))
        val normalized = PeopleRecentLogic.normalizeNumber(number)
        val lookup = lookupContact(resolver, normalized)
        val resolvedName = cachedName?.takeIf { it.isNotBlank() } ?: lookup?.displayName
        return PeopleCallEntry(
            id = id,
            phoneNumber = number,
            normalizedNumber = normalized,
            type = PeopleRecentLogic.mapCallType(type),
            timestamp = date,
            contactId = lookup?.contactId,
            contactName = resolvedName,
        )
    }

    private fun lookupContact(
        resolver: ContentResolver,
        normalized: String,
    ): ContactLookup? {
        if (normalized.isBlank()) return null
        for (candidate in PeopleRecentLogic.dialingCandidates(normalized)) {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(candidate),
            )
            val projection = arrayOf(
                ContactsContract.PhoneLookup._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME,
            )
            resolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val contactId = cursor.getLong(0)
                    val name = cursor.getString(1)?.trim().orEmpty()
                    if (contactId > 0L) {
                        return ContactLookup(
                            contactId = contactId,
                            displayName = name.takeIf { it.isNotEmpty() },
                        )
                    }
                }
            }
        }
        return null
    }

    private data class ContactLookup(
        val contactId: Long,
        val displayName: String?,
    )
}
