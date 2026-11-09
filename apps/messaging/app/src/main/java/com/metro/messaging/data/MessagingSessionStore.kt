package com.metro.messaging.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Persists pivot page, last conversation, and UI snapshots for instant cold start. */
class MessagingSessionStore(
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun lastOpenedThreadId(): Long? {
        if (!prefs.contains(KEY_LAST_THREAD_ID)) return null
        val id = prefs.getLong(KEY_LAST_THREAD_ID, -1L)
        return id.takeIf { it >= 0L }
    }

    fun saveLastOpenedThreadId(threadId: Long?) {
        prefs.edit().apply {
            if (threadId == null || threadId < 0L) {
                remove(KEY_LAST_THREAD_ID)
            } else {
                putLong(KEY_LAST_THREAD_ID, threadId)
            }
        }.apply()
    }

    fun lastPivotPage(): Int = prefs.getInt(KEY_PIVOT_PAGE, 0).coerceIn(0, 1)

    fun savePivotPage(page: Int) {
        prefs.edit().putInt(KEY_PIVOT_PAGE, page.coerceIn(0, 1)).apply()
    }

    fun loadThreadSnapshot(): List<ConversationThread> {
        val raw = prefs.getString(KEY_THREAD_SNAPSHOT, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    add(
                        ConversationThread(
                            id = json.getLong("id"),
                            address = json.getString("address"),
                            displayName = if (json.isNull("displayName")) {
                                null
                            } else {
                                json.optString("displayName").takeIf { it.isNotBlank() }
                            },
                            preview = json.optString("preview", ""),
                            timestamp = json.optLong("timestamp", 0L),
                            unreadCount = json.optInt("unreadCount", 0),
                        ),
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    fun saveThreadSnapshot(threads: List<ConversationThread>) {
        val array = JSONArray()
        threads.take(MAX_THREAD_SNAPSHOT).forEach { thread ->
            array.put(
                JSONObject()
                    .put("id", thread.id)
                    .put("address", thread.address)
                    .put("displayName", thread.displayName)
                    .put("preview", thread.preview)
                    .put("timestamp", thread.timestamp)
                    .put("unreadCount", thread.unreadCount),
            )
        }
        prefs.edit().putString(KEY_THREAD_SNAPSHOT, array.toString()).apply()
    }

    fun loadContactNames(): Map<String, String> {
        val raw = prefs.getString(KEY_CONTACT_NAMES, null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                json.keys().forEach { key ->
                    if (json.isNull(key)) return@forEach
                    val name = json.optString(key).takeIf { it.isNotBlank() } ?: return@forEach
                    put(key, name)
                }
            }
        }.getOrElse { emptyMap() }
    }

    fun saveContactNames(names: Map<String, String>) {
        if (names.isEmpty()) return
        val json = JSONObject()
        names.entries.take(MAX_CONTACT_NAMES).forEach { (key, value) ->
            json.put(key, value)
        }
        prefs.edit().putString(KEY_CONTACT_NAMES, json.toString()).apply()
    }

    fun loadMessagesSnapshot(threadId: Long): List<MessageItem> {
        if (prefs.getLong(KEY_MESSAGES_THREAD_ID, -1L) != threadId) return emptyList()
        val raw = prefs.getString(KEY_MESSAGES_SNAPSHOT, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    val images = json.optJSONArray("imageUris")
                    val imageUris = buildList {
                        if (images != null) {
                            for (i in 0 until images.length()) {
                                add(images.getString(i))
                            }
                        }
                    }
                    add(
                        MessageItem(
                            id = json.getLong("id"),
                            threadId = threadId,
                            body = json.getString("body"),
                            timestamp = json.getLong("timestamp"),
                            direction = MessageDirection.valueOf(json.getString("direction")),
                            sendState = SendState.valueOf(json.optString("sendState", SendState.Sent.name)),
                            imageUris = imageUris,
                            isMms = json.optBoolean("isMms", imageUris.isNotEmpty()),
                        ),
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    fun saveMessagesSnapshot(threadId: Long, messages: List<MessageItem>) {
        val array = JSONArray()
        messages.takeLast(MAX_MESSAGE_SNAPSHOT).forEach { message ->
            val images = JSONArray()
            message.imageUris.forEach { images.put(it) }
            array.put(
                JSONObject()
                    .put("id", message.id)
                    .put("body", message.body)
                    .put("timestamp", message.timestamp)
                    .put("direction", message.direction.name)
                    .put("sendState", message.sendState.name)
                    .put("imageUris", images)
                    .put("isMms", message.isMms),
            )
        }
        prefs.edit()
            .putLong(KEY_MESSAGES_THREAD_ID, threadId)
            .putString(KEY_MESSAGES_SNAPSHOT, array.toString())
            .apply()
    }

    /** threadId → epoch millis when the user opened the conversation. */
    fun loadLocalReadAt(): Map<Long, Long> {
        val raw = prefs.getString(KEY_LOCAL_READ_AT, null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                json.keys().forEach { key ->
                    val threadId = key.toLongOrNull() ?: return@forEach
                    put(threadId, json.optLong(key, 0L))
                }
            }
        }.getOrElse { emptyMap() }
    }

    fun saveLocalReadAt(threadId: Long, readAt: Long) {
        val current = loadLocalReadAt().toMutableMap()
        current[threadId] = readAt
        val json = JSONObject()
        current.entries
            .sortedByDescending { it.value }
            .take(MAX_LOCAL_READ)
            .forEach { (id, at) -> json.put(id.toString(), at) }
        prefs.edit().putString(KEY_LOCAL_READ_AT, json.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "messaging_session"
        private const val KEY_LAST_THREAD_ID = "last_thread_id"
        private const val KEY_PIVOT_PAGE = "pivot_page"
        private const val KEY_THREAD_SNAPSHOT = "thread_snapshot"
        private const val KEY_CONTACT_NAMES = "contact_names"
        private const val KEY_MESSAGES_THREAD_ID = "messages_thread_id"
        private const val KEY_MESSAGES_SNAPSHOT = "messages_snapshot"
        private const val KEY_LOCAL_READ_AT = "local_read_at"
        private const val MAX_THREAD_SNAPSHOT = 200
        private const val MAX_CONTACT_NAMES = 500
        private const val MAX_MESSAGE_SNAPSHOT = 300
        private const val MAX_LOCAL_READ = 200
    }
}
