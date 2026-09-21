package com.metro.widgets.data

import kotlin.math.max
import kotlin.math.roundToInt

data class BatterySnapshot(
    val percent: Int,
    val charging: Boolean,
) {
    val isLow: Boolean get() = percent <= 20
    val fraction: Float get() = (percent.coerceIn(0, 100)) / 100f
}

data class StorageVolumeSnapshot(
    val label: String,
    val freeBytes: Long,
    val totalBytes: Long,
) {
    val usedBytes: Long get() = max(0L, totalBytes - freeBytes)
}

data class StorageSnapshot(
    val phone: StorageVolumeSnapshot,
    val sdCard: StorageVolumeSnapshot? = null,
)

object WidgetFormatters {
    /** Digits only for the 1×1 Battery live tile (no `%` suffix). */
    fun batteryDigitsLabel(percent: Int): String =
        percent.coerceIn(0, 100).toString()

    fun batteryPercentLabel(percent: Int): String =
        "${batteryDigitsLabel(percent)}%"

    /** Whole GB with one decimal when under 10 GB, else integer GB. */
    fun bytesToGbLabel(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return if (gb < 10.0) {
            String.format("%.1f GB", gb)
        } else {
            "${gb.roundToInt()} GB"
        }
    }

    fun storageFreeLine(volume: StorageVolumeSnapshot): String =
        "${bytesToGbLabel(volume.freeBytes)} free"

    fun storageUsedLine(volume: StorageVolumeSnapshot): String =
        "${bytesToGbLabel(volume.usedBytes)} used"
}
