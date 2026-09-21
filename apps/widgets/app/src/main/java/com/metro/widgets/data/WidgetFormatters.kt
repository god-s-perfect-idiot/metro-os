package com.metro.widgets.data

data class BatterySnapshot(
    val percent: Int,
    val charging: Boolean,
) {
    val isLow: Boolean get() = percent <= 20
    val fraction: Float get() = (percent.coerceIn(0, 100)) / 100f
}

object WidgetFormatters {
    /** Digits only for the 1×1 Battery live tile (no `%` suffix). */
    fun batteryDigitsLabel(percent: Int): String =
        percent.coerceIn(0, 100).toString()

    fun batteryPercentLabel(percent: Int): String =
        "${batteryDigitsLabel(percent)}%"
}
