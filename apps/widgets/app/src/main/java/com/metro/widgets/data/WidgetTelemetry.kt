package com.metro.widgets.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import java.io.File

object WidgetTelemetry {
    fun readBattery(context: Context): BatterySnapshot {
        val intent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        )
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) {
            ((level * 100f) / scale).roundToIntSafe()
        } else {
            100
        }
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return BatterySnapshot(percent = percent, charging = charging)
    }

    fun readStorage(): StorageSnapshot {
        val phonePath = Environment.getDataDirectory()
        val phone = volumeSnapshot("phone", phonePath)
        val sd = secondaryStorageSnapshot()
        return StorageSnapshot(phone = phone, sdCard = sd)
    }

    private fun secondaryStorageSnapshot(): StorageVolumeSnapshot? {
        val dirs = arrayOf(
            Environment.getExternalStorageDirectory(),
        )
        for (dir in dirs) {
            if (dir == null || !dir.exists()) continue
            // Skip if it's the same as primary data partition footprint.
            if (dir.absolutePath == Environment.getDataDirectory().absolutePath) continue
            if (!Environment.isExternalStorageRemovable(dir) &&
                Environment.getExternalStorageState(dir) != Environment.MEDIA_MOUNTED
            ) {
                continue
            }
            val state = Environment.getExternalStorageState(dir)
            if (state != Environment.MEDIA_MOUNTED &&
                state != Environment.MEDIA_MOUNTED_READ_ONLY
            ) {
                continue
            }
            // Prefer reporting SD only when removable.
            if (!Environment.isExternalStorageRemovable(dir)) continue
            return volumeSnapshot("SD card", dir)
        }
        return null
    }

    private fun volumeSnapshot(label: String, path: File): StorageVolumeSnapshot {
        val stat = StatFs(path.absolutePath)
        val total = stat.blockCountLong * stat.blockSizeLong
        val free = stat.availableBlocksLong * stat.blockSizeLong
        return StorageVolumeSnapshot(label = label, freeBytes = free, totalBytes = total)
    }

    private fun Float.roundToIntSafe(): Int = (this + 0.5f).toInt().coerceIn(0, 100)
}
