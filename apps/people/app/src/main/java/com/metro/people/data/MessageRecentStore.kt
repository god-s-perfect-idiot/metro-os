package com.metro.people.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistent ring of up to [PeopleRecentLogic.MaxRecentPeople] saved contacts who messaged.
 * Oldest (by timestamp) drops when a new unique contact arrives past capacity.
 */
class MessageRecentStore(
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): List<PeopleMessageTouch> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val contactId = obj.optLong("contactId", 0L)
                    if (contactId <= 0L) continue
                    add(
                        PeopleMessageTouch(
                            contactId = contactId,
                            displayName = obj.optString("displayName").orEmpty(),
                            phoneNumber = obj.optString("phoneNumber").orEmpty(),
                            timestamp = obj.optLong("timestamp", 0L),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(touches: List<PeopleMessageTouch>) {
        val capped = PeopleRecentLogic.mergeMessageLog(emptyList(), touches)
        val array = JSONArray()
        for (touch in capped) {
            array.put(
                JSONObject()
                    .put("contactId", touch.contactId)
                    .put("displayName", touch.displayName)
                    .put("phoneNumber", touch.phoneNumber)
                    .put("timestamp", touch.timestamp),
            )
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    fun recordAll(incoming: List<PeopleMessageTouch>): List<PeopleMessageTouch> {
        val merged = PeopleRecentLogic.mergeMessageLog(load(), incoming)
        save(merged)
        return merged
    }

    companion object {
        private const val PREFS = "people_message_recent"
        private const val KEY = "entries"
    }
}
