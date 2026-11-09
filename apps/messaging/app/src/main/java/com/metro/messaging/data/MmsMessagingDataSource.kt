package com.metro.messaging.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Reads downloaded MMS from the Telephony provider (inbox/sent parts already on device).
 * Does not download pending WAP-push notifications from the carrier MMSC.
 */
class MmsMessagingDataSource(
    private val context: Context,
) {
    fun loadMessages(threadId: Long): List<MessageItem> {
        val projection = arrayOf(
            Telephony.Mms._ID,
            Telephony.Mms.DATE,
            Telephony.Mms.MESSAGE_BOX,
            Telephony.Mms.READ,
        )
        val cursor = runCatching {
            context.contentResolver.query(
                Telephony.Mms.CONTENT_URI,
                projection,
                "${Telephony.Mms.THREAD_ID} = ?",
                arrayOf(threadId.toString()),
                "${Telephony.Mms.DATE} ASC",
            )
        }.getOrNull() ?: return emptyList()

        return cursor.use { c ->
            val idIdx = c.columnIndexOrNull(Telephony.Mms._ID) ?: return emptyList()
            val dateIdx = c.columnIndexOrNull(Telephony.Mms.DATE) ?: return emptyList()
            val boxIdx = c.columnIndexOrNull(Telephony.Mms.MESSAGE_BOX) ?: return emptyList()
            buildList {
                while (c.moveToNext()) {
                    val mmsId = c.getLong(idIdx)
                    val parts = loadParts(mmsId)
                    val body = parts.text.ifBlank {
                        if (parts.imageUris.isNotEmpty()) "" else "Multimedia message"
                    }
                    val direction = when (c.getInt(boxIdx)) {
                        Telephony.Mms.MESSAGE_BOX_SENT,
                        Telephony.Mms.MESSAGE_BOX_OUTBOX,
                        Telephony.Mms.MESSAGE_BOX_FAILED,
                        -> MessageDirection.Outgoing
                        else -> MessageDirection.Incoming
                    }
                    // MMS DATE is seconds; SMS DATE is milliseconds.
                    val timestampMs = c.getLong(dateIdx).let { raw ->
                        if (raw < 100_000_000_000L) raw * 1000L else raw
                    }
                    add(
                        MessageItem(
                            id = mmsMessageId(mmsId),
                            threadId = threadId,
                            body = body,
                            timestamp = timestampMs,
                            direction = direction,
                            sendState = SendState.Sent,
                            imageUris = parts.imageUris,
                            isMms = true,
                        ),
                    )
                }
            }
        }
    }

    fun markThreadRead(threadId: Long): Boolean {
        val values = android.content.ContentValues().apply {
            put(Telephony.Mms.READ, 1)
            put(Telephony.Mms.SEEN, 1)
        }
        val updated = runCatching {
            context.contentResolver.update(
                Telephony.Mms.CONTENT_URI,
                values,
                "${Telephony.Mms.THREAD_ID} = ? AND ${Telephony.Mms.READ} = 0",
                arrayOf(threadId.toString()),
            )
        }.getOrDefault(0)
        return updated > 0
    }

    private data class MmsParts(
        val text: String,
        val imageUris: List<String>,
    )

    private fun loadParts(mmsId: Long): MmsParts {
        val uri = Uri.parse("content://mms/$mmsId/part")
        val cursor = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(
                    Telephony.Mms.Part._ID,
                    Telephony.Mms.Part.CONTENT_TYPE,
                    Telephony.Mms.Part.TEXT,
                    Telephony.Mms.Part._DATA,
                ),
                null,
                null,
                null,
            )
        }.getOrNull() ?: return MmsParts("", emptyList())

        val textParts = mutableListOf<String>()
        val imageUris = mutableListOf<String>()
        cursor.use { c ->
            val idIdx = c.columnIndexOrNull(Telephony.Mms.Part._ID) ?: return MmsParts("", emptyList())
            val typeIdx = c.columnIndexOrNull(Telephony.Mms.Part.CONTENT_TYPE) ?: return MmsParts("", emptyList())
            val textIdx = c.columnIndexOrNull(Telephony.Mms.Part.TEXT)
            while (c.moveToNext()) {
                val contentType = c.getString(typeIdx)?.lowercase().orEmpty()
                val partId = c.getLong(idIdx)
                when {
                    contentType.startsWith("text/plain") -> {
                        val inline = textIdx?.let { c.getString(it) }
                        val text = inline?.takeIf { it.isNotBlank() }
                            ?: readPartStreamAsText(partId)
                        if (text.isNotBlank()) textParts.add(text.trim())
                    }
                    contentType.startsWith("image/") -> {
                        imageUris.add(
                            ContentUris.withAppendedId(
                                Uri.parse("content://mms/part"),
                                partId,
                            ).toString(),
                        )
                    }
                }
            }
        }
        return MmsParts(
            text = textParts.joinToString("\n").trim(),
            imageUris = imageUris,
        )
    }

    private fun readPartStreamAsText(partId: Long): String {
        val partUri = ContentUris.withAppendedId(Uri.parse("content://mms/part"), partId)
        return runCatching {
            context.contentResolver.openInputStream(partUri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            }.orEmpty()
        }.getOrDefault("")
    }

    private fun android.database.Cursor.columnIndexOrNull(column: String): Int? {
        val index = getColumnIndex(column)
        return if (index >= 0) index else null
    }

    companion object {
        /** Negative ids keep MMS rows distinct from SMS `_ID` values in [MessageItem.id]. */
        fun mmsMessageId(mmsId: Long): Long = -1L - mmsId
    }
}
