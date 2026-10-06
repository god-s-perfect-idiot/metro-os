package com.metro.news.ui

import java.util.Locale
import java.util.concurrent.TimeUnit

object RelativeTime {
    fun format(publishedAtMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        val delta = (nowMs - publishedAtMs).coerceAtLeast(0L)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
        val hours = TimeUnit.MILLISECONDS.toHours(delta)
        val days = TimeUnit.MILLISECONDS.toDays(delta)
        return when {
            minutes < 1 -> "JUST NOW"
            minutes < 60 -> "$minutes ${if (minutes == 1L) "MINUTE" else "MINUTES"} AGO"
            hours < 24 -> "$hours ${if (hours == 1L) "HOUR" else "HOURS"} AGO"
            days < 7 -> "$days ${if (days == 1L) "DAY" else "DAYS"} AGO"
            else -> String.format(Locale.US, "%d DAYS AGO", days)
        }
    }
}
