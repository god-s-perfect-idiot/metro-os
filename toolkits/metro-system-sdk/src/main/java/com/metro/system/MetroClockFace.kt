package com.metro.system

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Shared digital / analog clock parts for Start widget faces and the Widgets catalog. */
data class MetroClockFaceParts(
    val hour: String,
    val minute: String,
    val period: String,
    /** Degrees clockwise from 12 for the hour hand (includes minute sweep). */
    val hourHandDegrees: Float,
    /** Degrees clockwise from 12 for the minute hand. */
    val minuteHandDegrees: Float,
)

object MetroClockFace {
    private val hourFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("h", Locale.US)
    private val minuteFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("mm", Locale.US)
    private val periodFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("a", Locale.US)

    fun parts(now: LocalDateTime = LocalDateTime.now()): MetroClockFaceParts {
        val minute = now.minute
        val hour12 = now.hour % 12
        return MetroClockFaceParts(
            hour = now.format(hourFormatter),
            minute = now.format(minuteFormatter),
            period = now.format(periodFormatter).uppercase(Locale.US),
            hourHandDegrees = hour12 * 30f + minute * 0.5f,
            minuteHandDegrees = minute * 6f,
        )
    }
}
