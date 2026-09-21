package com.metro.widgets.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ClockFaceParts(
    val hour: String,
    val minute: String,
    val period: String,
)

object TimeFaceLogic {
    private val hourFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("h", Locale.US)
    private val minuteFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("mm", Locale.US)
    private val periodFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("a", Locale.US)

    fun parts(now: LocalDateTime = LocalDateTime.now()): ClockFaceParts = ClockFaceParts(
        hour = now.format(hourFormatter),
        minute = now.format(minuteFormatter),
        period = now.format(periodFormatter).uppercase(Locale.US),
    )
}
