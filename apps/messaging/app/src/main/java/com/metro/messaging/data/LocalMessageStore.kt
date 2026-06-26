package com.metro.messaging.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local overlay for messages sent in demo mode or while SMS send is in flight.
 */
class LocalMessageStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun messagesForThread(threadId: Long): List<MessageItem> {
        val raw = prefs.getString(key(threadId), null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    add(
                        MessageItem(
                            id = json.getLong("id"),
                            threadId = threadId,
                            body = json.getString("body"),
                            timestamp = json.getLong("timestamp"),
                            direction = MessageDirection.valueOf(json.getString("direction")),
                            sendState = SendState.valueOf(json.getString("sendState")),
                        ),
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    fun allMessages(): List<MessageItem> {
        return prefs.all.keys
            .filter { it.startsWith(PREFIX) }
            .flatMap { key ->
                val threadId = key.removePrefix(PREFIX).toLongOrNull() ?: return@flatMap emptyList()
                messagesForThread(threadId)
            }
    }

    fun threadAddress(threadId: Long): String? =
        prefs.getString(threadKey(threadId), null)

    fun rememberThread(threadId: Long, address: String) {
        prefs.edit().putString(threadKey(threadId), address).apply()
    }

    fun append(message: MessageItem, address: String? = null) {
        address?.let { rememberThread(message.threadId, it) }
        val existing = messagesForThread(message.threadId).toMutableList()
        existing.removeAll { it.id == message.id }
        existing.add(message)
        persist(message.threadId, existing)
    }

    fun update(message: MessageItem) {
        append(message)
    }

    private fun persist(threadId: Long, messages: List<MessageItem>) {
        val array = JSONArray()
        messages.forEach { message ->
            array.put(
                JSONObject()
                    .put("id", message.id)
                    .put("body", message.body)
                    .put("timestamp", message.timestamp)
                    .put("direction", message.direction.name)
                    .put("sendState", message.sendState.name),
            )
        }
        prefs.edit().putString(key(threadId), array.toString()).apply()
    }

    private fun key(threadId: Long) = "$PREFIX$threadId"

    private fun threadKey(threadId: Long) = "address_$threadId"

    fun localThreads(): List<ConversationThread> {
        return prefs.all.keys
            .filter { it.startsWith(PREFIX) }
            .mapNotNull { key ->
                val threadId = key.removePrefix(PREFIX).toLongOrNull() ?: return@mapNotNull null
                val messages = messagesForThread(threadId)
                if (messages.isEmpty()) return@mapNotNull null
                val latest = messages.maxBy { it.timestamp }
                ConversationThread(
                    id = threadId,
                    address = threadAddress(threadId).orEmpty(),
                    displayName = null,
                    preview = MessagingLogic.previewText(latest.body),
                    timestamp = latest.timestamp,
                    unreadCount = 0,
                )
            }
    }

    companion object {
        private const val PREFS_NAME = "messaging_local_messages"
        private const val PREFIX = "thread_"
    }
}
