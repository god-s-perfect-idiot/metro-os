package com.metro.messaging.data

import android.content.Context
import org.json.JSONObject

class DraftStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(threadId: Long): DraftState? {
        val raw = prefs.getString(key(threadId), null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            DraftState(
                threadId = threadId,
                text = json.getString("text"),
            )
        }.getOrNull()
    }

    fun save(draft: DraftState) {
        if (draft.text.isBlank()) {
            clear(draft.threadId)
            return
        }
        val json = JSONObject()
            .put("text", draft.text)
        prefs.edit().putString(key(draft.threadId), json.toString()).apply()
    }

    fun clear(threadId: Long) {
        prefs.edit().remove(key(threadId)).apply()
    }

    private fun key(threadId: Long) = "draft_$threadId"

    companion object {
        private const val PREFS_NAME = "messaging_drafts"
    }
}
