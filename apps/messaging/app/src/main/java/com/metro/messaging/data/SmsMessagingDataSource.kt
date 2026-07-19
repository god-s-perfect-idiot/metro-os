package com.metro.messaging.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony

class SmsMessagingDataSource(
    private val context: Context,
) {
    private val contactNameCache = HashMap<String, String?>()
    private val canonicalAddressCache = HashMap<Long, String?>()

    /**
     * Loads conversation rows from [Telephony.Threads] (one row per thread). Falls back to
     * scanning [Telephony.Sms.CONTENT_URI] only when the threads table is unavailable — the
     * full SMS scan is too expensive for large inboxes and must never run on the main thread.
     *
     * Avoids [Telephony.Sms.Conversations], which is incomplete or throws on many OEMs when
     * projecting DATE/READ/SNIPPET.
     */
    fun loadThreads(): List<ConversationThread> {
        contactNameCache.clear()
        canonicalAddressCache.clear()
        return loadThreadsFromThreadsTable() ?: loadThreadsByScanningSms()
    }

    private fun loadThreadsFromThreadsTable(): List<ConversationThread>? {
        val uri = Telephony.Threads.CONTENT_URI.buildUpon()
            .appendQueryParameter("simple", "true")
            .build()
        val projection = arrayOf(
            Telephony.Threads._ID,
            Telephony.Threads.DATE,
            Telephony.Threads.RECIPIENT_IDS,
            Telephony.Threads.SNIPPET,
            Telephony.Threads.READ,
            Telephony.Threads.MESSAGE_COUNT,
        )
        val cursor = runCatching {
            context.contentResolver.query(
                uri,
                projection,
                "${Telephony.Threads.MESSAGE_COUNT} > 0",
                null,
                "${Telephony.Threads.DATE} DESC",
            )
        }.getOrNull() ?: return null

        return cursor.use { c ->
            val idIdx = c.columnIndexOrNull(Telephony.Threads._ID) ?: return null
            val dateIdx = c.columnIndexOrNull(Telephony.Threads.DATE) ?: return null
            val recipientsIdx = c.columnIndexOrNull(Telephony.Threads.RECIPIENT_IDS)
            val snippetIdx = c.columnIndexOrNull(Telephony.Threads.SNIPPET)
            val readIdx = c.columnIndexOrNull(Telephony.Threads.READ)

            val unreadByThread = loadUnreadCountsByThread()
            val threads = ArrayList<ConversationThread>(c.count.coerceAtLeast(0))
            while (c.moveToNext()) {
                val threadId = c.getLong(idIdx)
                val recipientIds = recipientsIdx?.let { c.getString(it) }.orEmpty()
                val address = addressForRecipientIds(recipientIds)
                    ?: latestAddressForThread(threadId)
                    ?: continue
                val read = readIdx == null || c.getInt(readIdx) == 1
                val unreadFromSms = unreadByThread[threadId] ?: 0
                threads.add(
                    ConversationThread(
                        id = threadId,
                        address = address,
                        displayName = cachedContactName(address),
                        preview = MessagingLogic.previewText(
                            snippetIdx?.let { c.getString(it) }.orEmpty(),
                        ),
                        timestamp = c.getLong(dateIdx),
                        unreadCount = when {
                            unreadFromSms > 0 -> unreadFromSms
                            read -> 0
                            else -> 1
                        },
                    ),
                )
            }
            threads
        }
    }

    /**
     * Fallback for devices where the threads table query fails. Scans SMS newest-first and
     * groups by thread id; contact lookups are memoized so each address hits Contacts once.
     */
    private fun loadThreadsByScanningSms(): List<ConversationThread> {
        val projection = arrayOf(
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
        )
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} DESC",
        ) ?: return emptyList()

        return cursor.use { c ->
            val threadIdIdx = c.columnIndexOrNull(Telephony.Sms.THREAD_ID) ?: return emptyList()
            val addressIdx = c.columnIndexOrNull(Telephony.Sms.ADDRESS) ?: return emptyList()
            val bodyIdx = c.columnIndexOrNull(Telephony.Sms.BODY) ?: return emptyList()
            val dateIdx = c.columnIndexOrNull(Telephony.Sms.DATE) ?: return emptyList()
            val readIdx = c.columnIndexOrNull(Telephony.Sms.READ)

            val rows = sequence {
                while (c.moveToNext()) {
                    yield(
                        MessagingLogic.SmsRow(
                            threadId = c.getLong(threadIdIdx),
                            address = c.getString(addressIdx),
                            body = c.getString(bodyIdx).orEmpty(),
                            timestamp = c.getLong(dateIdx),
                            read = readIdx == null || c.getInt(readIdx) == 1,
                        ),
                    )
                }
            }
            MessagingLogic.threadsFromSmsRows(rows, displayNameFor = ::cachedContactName)
        }
    }

    /** One pass over unread inbox rows — typically tiny compared to the full SMS table. */
    private fun loadUnreadCountsByThread(): Map<Long, Int> {
        val counts = HashMap<Long, Int>()
        val cursor = runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.THREAD_ID),
                "${Telephony.Sms.READ} = 0",
                null,
                null,
            )
        }.getOrNull() ?: return emptyMap()

        cursor.use { c ->
            val threadIdIdx = c.columnIndexOrNull(Telephony.Sms.THREAD_ID) ?: return emptyMap()
            while (c.moveToNext()) {
                val threadId = c.getLong(threadIdIdx)
                counts[threadId] = (counts[threadId] ?: 0) + 1
            }
        }
        return counts
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
            val idIdx = cursor.columnIndexOrNull(Telephony.Sms._ID) ?: return emptyList()
            val bodyIdx = cursor.columnIndexOrNull(Telephony.Sms.BODY) ?: return emptyList()
            val dateIdx = cursor.columnIndexOrNull(Telephony.Sms.DATE) ?: return emptyList()
            val typeIdx = cursor.columnIndexOrNull(Telephony.Sms.TYPE) ?: return emptyList()
            while (cursor.moveToNext()) {
                val type = cursor.getInt(typeIdx)
                val direction = when (type) {
                    Telephony.Sms.MESSAGE_TYPE_INBOX -> MessageDirection.Incoming
                    Telephony.Sms.MESSAGE_TYPE_SENT -> MessageDirection.Outgoing
                    else -> MessageDirection.Incoming
                }
                messages.add(
                    MessageItem(
                        id = cursor.getLong(idIdx),
                        threadId = threadId,
                        body = cursor.getString(bodyIdx).orEmpty(),
                        timestamp = cursor.getLong(dateIdx),
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
        val projection = arrayOf(
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
        )
        // Match exact or suffix (local 10-digit vs E.164) without LIMIT in sortOrder —
        // many providers reject "LIMIT n" in the order clause.
        context.contentResolver.query(
            uri,
            projection,
            "${Telephony.Sms.ADDRESS} LIKE ?",
            arrayOf("%$normalized"),
            "${Telephony.Sms.DATE} DESC",
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val threadIdIdx = cursor.columnIndexOrNull(Telephony.Sms.THREAD_ID) ?: return null
                val addressIdx = cursor.columnIndexOrNull(Telephony.Sms.ADDRESS) ?: return null
                val bodyIdx = cursor.columnIndexOrNull(Telephony.Sms.BODY)
                val dateIdx = cursor.columnIndexOrNull(Telephony.Sms.DATE) ?: return null
                val readIdx = cursor.columnIndexOrNull(Telephony.Sms.READ)
                val resolvedAddress = cursor.getString(addressIdx)?.trim().orEmpty()
                    .ifEmpty { normalized }
                return ConversationThread(
                    id = cursor.getLong(threadIdIdx),
                    address = resolvedAddress,
                    displayName = cachedContactName(resolvedAddress),
                    preview = MessagingLogic.previewText(bodyIdx?.let { cursor.getString(it) }.orEmpty()),
                    timestamp = cursor.getLong(dateIdx),
                    unreadCount = if (readIdx == null || cursor.getInt(readIdx) == 1) 0 else 1,
                )
            }
        }
        return null
    }

    /**
     * Resolves or creates the system thread id for [address] so new sends land in the same
     * conversation the provider already uses.
     */
    fun threadIdForAddress(address: String): Long {
        val normalized = MessagingLogic.normalizeAddress(address).ifEmpty { address.trim() }
        threadForAddress(normalized)?.let { return it.id }
        return runCatching {
            Telephony.Threads.getOrCreateThreadId(context, normalized)
        }.getOrElse {
            MessagingLogic.threadIdForAddress(normalized)
        }
    }

    private fun addressForRecipientIds(recipientIds: String): String? {
        val firstId = recipientIds.trim().split(Regex("\\s+"))
            .firstOrNull()
            ?.toLongOrNull()
            ?: return null
        return canonicalAddressCache.getOrPut(firstId) {
            val uri = Uri.parse("content://mms-sms/canonical-address/$firstId")
            runCatching {
                context.contentResolver.query(uri, arrayOf("address"), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getString(0)?.trim()?.takeIf { it.isNotEmpty() }
                    } else {
                        null
                    }
                }
            }.getOrNull()
        }
    }

    private fun latestAddressForThread(threadId: Long): String? {
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS),
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} DESC",
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)?.trim()?.takeIf { it.isNotEmpty() }
            }
        }
        return null
    }

    private fun cachedContactName(address: String): String? {
        val key = MessagingLogic.normalizeAddress(address).ifEmpty { address.trim() }
        if (key.isEmpty()) return null
        return contactNameCache.getOrPut(key) { lookupContactName(address) }
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
                val idx = cursor.columnIndexOrNull(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    ?: return null
                return cursor.getString(idx)
            }
        }
        return null
    }

    private fun hasContactsPermission(): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun Cursor.columnIndexOrNull(column: String): Int? {
        val index = getColumnIndex(column)
        return if (index >= 0) index else null
    }
}
