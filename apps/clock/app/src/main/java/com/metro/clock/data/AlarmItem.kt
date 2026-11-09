package com.metro.clock.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

/**
 * One user alarm. [repeatDays] uses [Calendar] weekday constants
 * ([Calendar.SUNDAY]…[Calendar.SATURDAY]). Empty = fires once.
 */
data class AlarmItem(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
    val name: String = "Alarm",
    val repeatDays: Set<Int> = emptySet(),
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("hour", hour)
        .put("minute", minute)
        .put("enabled", enabled)
        .put("name", name)
        .put(
            "repeatDays",
            JSONArray().also { arr ->
                repeatDays.sorted().forEach { arr.put(it) }
            },
        )

    companion object {
        fun fromJson(obj: JSONObject): AlarmItem {
            val days = mutableSetOf<Int>()
            val arr = obj.optJSONArray("repeatDays")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    days += arr.getInt(i)
                }
            }
            return AlarmItem(
                id = obj.getString("id"),
                hour = obj.getInt("hour"),
                minute = obj.getInt("minute"),
                enabled = obj.optBoolean("enabled", true),
                name = obj.optString("name", "Alarm"),
                repeatDays = days,
            )
        }
    }
}
