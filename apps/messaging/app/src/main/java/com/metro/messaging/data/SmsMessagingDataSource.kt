package com.metro.messaging.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony

class SmsMessagingDataSource(
    private val context: Context,
) {
    fun loadThreads(): List<ConversationThread> {
        val uri = Telephony.Sms.Conversations.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.Conversations.THREAD_ID,
            Telephony.Sms.Conversations.SNIPPET,
            Telephony.Sms.Conversations.DATE,
            Telephony.Sms.Conversations.MESSAGE_COUNT,
            Telephony.Sms.Conversations.READ,
        )
        val threads = mutableListOf<ConversationThread>()
        context.contentResolver.query(uri, projection, null, null, "${Telephony.Sms.DATE} DESC")?.use { cursor ->
            while (cursor.moveToNext()) {
                val threadId = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.Conversations.THREAD_ID))
                val address = resolveAddress(threadId) ?: continue
                val preview = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.Conversations.SNIPPET))
                    .orEmpty()
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.Conversations.DATE))
                val read = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.Conversations.READ)) == 1
                threads.add(
                    ConversationThread(
                        id = threadId,
                        address = address,
                        displayName = lookupContactName(address),
                        preview = MessagingLogic.previewText(preview),
                        timestamp = timestamp,
                        unreadCount = if (read) 0 else 1,
                    ),
                )
            }
        }
        return threads
    }

    fun loadMessages(threadId: Long): List<MessageItem> {
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
        )
        val selection = "${Telephony.Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())
        val messages = mutableListOf<MessageItem>()
        context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            "${Telephony.Sms.DATE} ASC",
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE))
                val direction = when (type) {
                    Telephony.Sms.MESSAGE_TYPE_INBOX -> MessageDirection.Incoming
                    Telephony.Sms.MESSAGE_TYPE_SENT -> MessageDirection.Outgoing
                    else -> MessageDirection.Incoming
                }
                messages.add(
                    MessageItem(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID)),
                        threadId = threadId,
                        body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)).orEmpty(),
                        timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)),
                        direction = direction,
                        sendState = SendState.Sent,
                    ),
                )
            }
        }
        return messages
    }

    fun threadForAddress(address: String): ConversationThread? {
        val normalized = MessagingLogic.normalizeAddress(address)
        if (normalized.isEmpty()) return null
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(Telephony.Sms.THREAD_ID, Telephony.Sms.ADDRESS, Telephony.Sms.DATE)
        context.contentResolver.query(
            uri,
            projection,
            "${Telephony.Sms.ADDRESS} LIKE ?",
            arrayOf("%$normalized%"),
            "${Telephony.Sms.DATE} DESC LIMIT 1",
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val threadId = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID))
                return ConversationThread(
                    id = threadId,
                    address = normalized,
                    displayName = lookupContactName(normalized),
                    preview = "",
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)),
                    unreadCount = 0,
                )
            }
        }
        return StubMessagingDataSource.threadForAddress(normalized, lookupContactName(normalized))
    }

    private fun resolveAddress(threadId: Long): String? {
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(Telephony.Sms.ADDRESS)
        context.contentResolver.query(
            uri,
            projection,
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} DESC LIMIT 1",
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
            }
        }
        return null
    }

    private fun lookupContactName(address: String): String? {
        if (!hasContactsPermission()) return null
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(address),
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
            }
        }
        return null
    }

    private fun hasContactsPermission(): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun Cursor.getColumnIndexOrThrow(column: String): Int {
        val index = getColumnIndex(column)
        require(index >= 0) { "Column $column not found" }
        return index
    }
}
