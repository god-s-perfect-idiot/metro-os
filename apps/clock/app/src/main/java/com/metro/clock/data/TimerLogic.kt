package com.metro.clock.data

import java.util.Locale

object TimerLogic {
    fun totalSeconds(hours: Int, minutes: Int, seconds: Int): Int =
        hours.coerceAtLeast(0) * 3600 +
            minutes.coerceIn(0, 59) * 60 +
            seconds.coerceIn(0, 59)

    fun partsFromTotalSeconds(total: Int): Triple<Int, Int, Int> {
        val clamped = total.coerceAtLeast(0)
        val hours = clamped / 3600
        val minutes = (clamped % 3600) / 60
        val seconds = clamped % 60
        return Triple(hours, minutes, seconds)
    }

    /** Display `H:MM:SS` when hours > 0, else `M:SS`. */
    fun formatDuration(totalSeconds: Int): String {
        val (h, m, s) = partsFromTotalSeconds(totalSeconds)
        return if (h > 0) {
            "%d:%02d:%02d".format(Locale.US, h, m, s)
        } else {
            "%d:%02d".format(Locale.US, m, s)
        }
    }

    fun formatStopwatch(elapsedMs: Long): String {
        val clamped = elapsedMs.coerceAtLeast(0L)
        val totalCentis = clamped / 10L
        val minutes = totalCentis / 6000L
        val seconds = (totalCentis % 6000L) / 100L
        val centis = totalCentis % 100L
        return "%d:%02d.%02d".format(Locale.US, minutes, seconds, centis)
    }
}
