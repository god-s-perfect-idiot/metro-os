package com.metro.clock.data

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class WorldCity(
    val id: String,
    val name: String,
    val zoneId: String,
)

data class WorldClockEntry(
    val city: WorldCity,
)

object WorldClockLogic {
    /** Curated IANA cities for the add picker (WP-style world clock list). */
    val Catalog: List<WorldCity> = listOf(
        WorldCity("london", "London", "Europe/London"),
        WorldCity("paris", "Paris", "Europe/Paris"),
        WorldCity("berlin", "Berlin", "Europe/Berlin"),
        WorldCity("moscow", "Moscow", "Europe/Moscow"),
        WorldCity("dubai", "Dubai", "Asia/Dubai"),
        WorldCity("mumbai", "Mumbai", "Asia/Kolkata"),
        WorldCity("delhi", "New Delhi", "Asia/Kolkata"),
        WorldCity("bangkok", "Bangkok", "Asia/Bangkok"),
        WorldCity("singapore", "Singapore", "Asia/Singapore"),
        WorldCity("hong_kong", "Hong Kong", "Asia/Hong_Kong"),
        WorldCity("tokyo", "Tokyo", "Asia/Tokyo"),
        WorldCity("seoul", "Seoul", "Asia/Seoul"),
        WorldCity("sydney", "Sydney", "Australia/Sydney"),
        WorldCity("auckland", "Auckland", "Pacific/Auckland"),
        WorldCity("honolulu", "Honolulu", "Pacific/Honolulu"),
        WorldCity("anchorage", "Anchorage", "America/Anchorage"),
        WorldCity("los_angeles", "Los Angeles", "America/Los_Angeles"),
        WorldCity("denver", "Denver", "America/Denver"),
        WorldCity("chicago", "Chicago", "America/Chicago"),
        WorldCity("new_york", "New York", "America/New_York"),
        WorldCity("toronto", "Toronto", "America/Toronto"),
        WorldCity("sao_paulo", "São Paulo", "America/Sao_Paulo"),
        WorldCity("buenos_aires", "Buenos Aires", "America/Argentina/Buenos_Aires"),
        WorldCity("cairo", "Cairo", "Africa/Cairo"),
        WorldCity("johannesburg", "Johannesburg", "Africa/Johannesburg"),
        WorldCity("nairobi", "Nairobi", "Africa/Nairobi"),
        WorldCity("istanbul", "Istanbul", "Europe/Istanbul"),
        WorldCity("tehran", "Tehran", "Asia/Tehran"),
        WorldCity("beijing", "Beijing", "Asia/Shanghai"),
        WorldCity("shanghai", "Shanghai", "Asia/Shanghai"),
    ).sortedBy { it.name.lowercase(Locale.US) }

    fun cityById(id: String): WorldCity? = Catalog.firstOrNull { it.id == id }

    fun formatLocalTime(zoneId: String, nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone(zoneId)).apply {
            timeInMillis = nowMillis
        }
        val hour24 = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        return AlarmLogic.formatTime(hour24, minute)
    }

    /**
     * Offset of [zoneId] relative to the device default zone at [nowMillis],
     * e.g. `+5:30 hrs`, `-8 hrs`, `same as local`.
     */
    fun formatOffsetLabel(
        zoneId: String,
        nowMillis: Long = System.currentTimeMillis(),
        localZone: TimeZone = TimeZone.getDefault(),
    ): String {
        val target = TimeZone.getTimeZone(zoneId)
        val localOffset = localZone.getOffset(nowMillis).toLong()
        val targetOffset = target.getOffset(nowMillis).toLong()
        val deltaMs = targetOffset - localOffset
        if (deltaMs == 0L) return "same as local"
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(deltaMs)
        val sign = if (totalMinutes > 0) "+" else "-"
        val absMinutes = kotlin.math.abs(totalMinutes)
        val hours = absMinutes / 60
        val minutes = absMinutes % 60
        return if (minutes == 0L) {
            "$sign$hours hrs"
        } else {
            "$sign$hours:${"%02d".format(Locale.US, minutes)} hrs"
        }
    }
}
