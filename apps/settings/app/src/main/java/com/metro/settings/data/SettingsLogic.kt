package com.metro.settings.data

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.telephony.TelephonyManager
import com.metro.system.MetroFontScale
import com.metro.system.MetroThemeMode
import kotlin.math.roundToInt

/**
 * Pure helpers for settings preference validation (unit-tested).
 */
object SettingsLogic {
    /** Suite release tag shown on extras+info (Software). */
    const val METRO_OS_VERSION = "alpha-3"

    fun normalizeTheme(storage: String?): MetroThemeMode =
        MetroThemeMode.fromStorage(storage)

    fun snapFontScale(value: Float): Float =
        MetroFontScale.coerceToStep(value)

    fun fontScaleIndex(value: Float): Int =
        MetroFontScale.indexOf(value)

    fun brightnessToSystem(fraction: Float): Int =
        (fraction.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(1, 255)

    fun brightnessFromSystem(value: Int): Float =
        (value.coerceIn(0, 255) / 255f).coerceIn(0f, 1f)

    fun formatBytes(bytes: Long): String {
        if (bytes < 0L) return "—"
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        return when {
            bytes >= gb -> String.format("%.1f GB", bytes / gb)
            bytes >= mb -> String.format("%.0f MB", bytes / mb)
            bytes >= kb -> String.format("%.0f KB", bytes / kb)
            else -> "$bytes B"
        }
    }

    fun displayOrDash(value: String?): String {
        val trimmed = value?.trim().orEmpty()
        return if (trimmed.isEmpty() || trimmed.equals("unknown", ignoreCase = true)) {
            "—"
        } else {
            trimmed
        }
    }
}

data class StorageSnapshot(
    val totalBytes: Long,
    val freeBytes: Long,
) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0L)
}

/**
 * WP8.1 About → more info fields, mapped onto Android device properties.
 */
data class DeviceInfoSnapshot(
    val name: String,
    val model: String,
    val manufacturer: String,
    val carrier: String,
    val software: String,
    val totalStorage: String,
    val availableStorage: String,
    val osVersion: String,
    val firmwareRevision: String,
    val hardwareRevision: String,
    val radioSoftware: String,
    val bootloader: String,
    val chipSoc: String,
    val buildId: String,
    val board: String,
    val abi: String,
)

/**
 * Direct read/write of Android system settings used by Metro Settings.
 * Never launches the Android Settings app.
 */
class SystemSettingsBridge(context: Context) {
    private val appContext = context.applicationContext

    fun canWriteSystemSettings(): Boolean =
        Settings.System.canWrite(appContext)

    fun brightnessFraction(): Float {
        val raw = Settings.System.getInt(
            appContext.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            128,
        )
        return SettingsLogic.brightnessFromSystem(raw)
    }

    fun setBrightnessFraction(fraction: Float): Boolean {
        if (!canWriteSystemSettings()) return false
        return runCatching {
            Settings.System.putInt(
                appContext.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
            )
            Settings.System.putInt(
                appContext.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                SettingsLogic.brightnessToSystem(fraction),
            )
        }.getOrDefault(false)
    }

    fun storageSnapshot(): StorageSnapshot? {
        return runCatching {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val total = stat.blockCountLong * stat.blockSizeLong
            val free = stat.availableBlocksLong * stat.blockSizeLong
            StorageSnapshot(totalBytes = total, freeBytes = free)
        }.getOrNull()
    }

    fun deviceInfoSnapshot(): DeviceInfoSnapshot {
        val storage = storageSnapshot()
        val deviceName = SettingsLogic.displayOrDash(
            Settings.Global.getString(appContext.contentResolver, Settings.Global.DEVICE_NAME)
                ?: Build.MODEL,
        )
        val carrier = SettingsLogic.displayOrDash(
            runCatching {
                val tm = appContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                tm?.networkOperatorName
            }.getOrNull(),
        )
        val abi = if (Build.SUPPORTED_ABIS.isNotEmpty()) {
            Build.SUPPORTED_ABIS.joinToString(", ")
        } else {
            @Suppress("DEPRECATION")
            Build.CPU_ABI
        }
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOfNotNull(Build.SOC_MANUFACTURER, Build.SOC_MODEL)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { Build.HARDWARE }
        } else {
            Build.HARDWARE
        }

        return DeviceInfoSnapshot(
            name = deviceName,
            model = SettingsLogic.displayOrDash(Build.MODEL),
            manufacturer = SettingsLogic.displayOrDash(Build.MANUFACTURER),
            carrier = carrier,
            software = "metro-os ${SettingsLogic.METRO_OS_VERSION}",
            totalStorage = storage?.let { SettingsLogic.formatBytes(it.totalBytes) } ?: "—",
            availableStorage = storage?.let { SettingsLogic.formatBytes(it.freeBytes) } ?: "—",
            osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            firmwareRevision = SettingsLogic.displayOrDash(Build.DISPLAY),
            hardwareRevision = SettingsLogic.displayOrDash(Build.HARDWARE),
            radioSoftware = SettingsLogic.displayOrDash(Build.getRadioVersion()),
            bootloader = SettingsLogic.displayOrDash(Build.BOOTLOADER),
            chipSoc = SettingsLogic.displayOrDash(soc),
            buildId = SettingsLogic.displayOrDash(Build.ID),
            board = SettingsLogic.displayOrDash(Build.BOARD),
            abi = SettingsLogic.displayOrDash(abi),
        )
    }
}
