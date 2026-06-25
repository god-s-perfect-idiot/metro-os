package com.metro.dialer.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract

class CallLogRepository(
    private val context: Context,
) {
    fun loadRecentCalls(limit: Int = 200): List<CallEntry> {
        val resolver = context.contentResolver
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
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
            val entries = mutableListOf<CallEntry>()
            while (cursor.moveToNext() && entries.size < limit) {
                entries.add(cursor.toCallEntry(resolver))
            }
            return entries
        }
    }

    private fun Cursor.toCallEntry(resolver: ContentResolver): CallEntry {
        val id = getLong(getColumnIndexOrThrow(CallLog.Calls._ID))
        val number = getString(getColumnIndexOrThrow(CallLog.Calls.NUMBER)) ?: ""
        val cachedName = getString(getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME))
        val type = getInt(getColumnIndexOrThrow(CallLog.Calls.TYPE))
        val date = getLong(getColumnIndexOrThrow(CallLog.Calls.DATE))
        val duration = getInt(getColumnIndexOrThrow(CallLog.Calls.DURATION))
        val normalized = DialerCallLogic.normalizeNumber(number)
        val resolvedName = cachedName?.takeIf { it.isNotBlank() }
            ?: lookupContactName(resolver, normalized)
        return CallEntry(
            id = id,
            phoneNumber = number,
            normalizedNumber = normalized,
            type = DialerCallLogic.mapCallType(type),
            timestamp = date,
            durationSeconds = duration,
            contactName = resolvedName,
        )
    }

    private fun lookupContactName(resolver: ContentResolver, normalized: String): String? {
        if (normalized.isBlank()) return null
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(normalized),
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        resolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        return null
    }
}
