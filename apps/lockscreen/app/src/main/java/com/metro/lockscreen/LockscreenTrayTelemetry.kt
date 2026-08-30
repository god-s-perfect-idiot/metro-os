package com.metro.lockscreen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.metro.ui.MetroWifiBandCount

/**
 * Live lock-tray telemetry (battery + radio). Kept out of Compose so unit tests can cover
 * parsing without Robolectric. Mirrors the statusbar app's mappings without importing it.
 */
data class LockscreenBatteryStatus(
    val fraction: Float,
    val charging: Boolean,
    val present: Boolean = true,
) {
    val percent: Int get() = (fraction.coerceIn(0f, 1f) * 100f).toInt()
    val isLow: Boolean get() = percent <= LOW_PERCENT_THRESHOLD

    companion object {
        const val LOW_PERCENT_THRESHOLD = 20

        val Unknown = LockscreenBatteryStatus(fraction = 1f, charging = false, present = true)

        fun fromLevel(level: Int, scale: Int, charging: Boolean): LockscreenBatteryStatus {
            if (scale <= 0 || level < 0) return Unknown.copy(charging = charging)
            return LockscreenBatteryStatus(
                fraction = (level.toFloat() / scale.toFloat()).coerceIn(0f, 1f),
                charging = charging,
            )
        }
    }
}

data class LockscreenSignalStatus(
    val cellularBars: Int,
    val wifiBands: Int?,
) {
    companion object {
        const val CELLULAR_BAR_COUNT = 4
        const val WIFI_BAND_COUNT = MetroWifiBandCount

        val Unknown = LockscreenSignalStatus(
            cellularBars = CELLULAR_BAR_COUNT,
            wifiBands = WIFI_BAND_COUNT,
        )

        fun fromLevels(cellularLevel: Int, wifiBands: Int?): LockscreenSignalStatus =
            LockscreenSignalStatus(
                cellularBars = cellularLevel.coerceIn(0, CELLULAR_BAR_COUNT),
                wifiBands = wifiBands?.coerceIn(0, WIFI_BAND_COUNT),
            )
    }
}

object LockscreenBatterySource {
    fun parse(intent: Intent?): LockscreenBatteryStatus {
        if (intent == null) return LockscreenBatteryStatus.Unknown
        val present = intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true)
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(
            BatteryManager.EXTRA_STATUS,
            BatteryManager.BATTERY_STATUS_UNKNOWN,
        )
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val charging = plugged != 0 && status != BatteryManager.BATTERY_STATUS_DISCHARGING
        return LockscreenBatteryStatus.fromLevel(level, scale, charging).copy(present = present)
    }

    fun current(context: Context): LockscreenBatteryStatus {
        val sticky = context.applicationContext.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        )
        return parse(sticky)
    }
}

object LockscreenSignalSource {
    private const val MIN_WIFI_RSSI = -100
    private const val MAX_WIFI_RSSI = -55
    private const val INVALID_RSSI = -127

    fun canReadCellular(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    fun current(context: Context): LockscreenSignalStatus =
        LockscreenSignalStatus.fromLevels(
            cellularLevel = cellularBars(context),
            wifiBands = wifiBands(context),
        )

    fun barsFromSignalStrength(signalStrength: SignalStrength?): Int {
        if (signalStrength == null) return 0
        return signalStrength.level.coerceIn(0, LockscreenSignalStatus.CELLULAR_BAR_COUNT)
    }

    /**
     * Live cellular updates via [TelephonyCallback] / [PhoneStateListener].
     * Returns null when [READ_PHONE_STATE] is missing so the caller can skip unregister.
     */
    fun registerCellularListener(
        context: Context,
        onBars: (Int) -> Unit,
    ): CellularListenerHandle? {
        if (!canReadCellular(context)) return null
        val manager = context.getSystemService(TelephonyManager::class.java) ?: return null
        onBars(cellularBars(context))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object :
                TelephonyCallback(),
                TelephonyCallback.SignalStrengthsListener {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    onBars(barsFromSignalStrength(signalStrength))
                }
            }
            return runCatching {
                manager.registerTelephonyCallback(context.mainExecutor, callback)
                CellularListenerHandle {
                    runCatching { manager.unregisterTelephonyCallback(callback) }
                }
            }.getOrNull()
        }

        @Suppress("DEPRECATION")
        val listener = object : PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                onBars(barsFromSignalStrength(signalStrength))
            }
        }
        @Suppress("DEPRECATION")
        return runCatching {
            manager.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
            CellularListenerHandle {
                @Suppress("DEPRECATION")
                runCatching { manager.listen(listener, PhoneStateListener.LISTEN_NONE) }
            }
        }.getOrNull()
    }

    private fun cellularBars(context: Context): Int {
        if (!canReadCellular(context)) return 0
        val telephony = context.getSystemService(TelephonyManager::class.java) ?: return 0
        return runCatching {
            if (telephony.simState != TelephonyManager.SIM_STATE_READY) return 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                barsFromSignalStrength(telephony.signalStrength)
            } else {
                0
            }
        }.getOrDefault(0)
    }

    private fun wifiBands(context: Context): Int? {
        val appContext = context.applicationContext
        val wifi = appContext.getSystemService(WifiManager::class.java) ?: return null
        if (!wifi.isWifiEnabled) return null
        val info = connectedWifiInfo(appContext, wifi) ?: return null
        val rssi = info.rssi
        if (rssi <= INVALID_RSSI) return null
        return bandsFromRssi(rssi)
    }

    private fun bandsFromRssi(rssi: Int): Int {
        val numLevels = LockscreenSignalStatus.WIFI_BAND_COUNT + 1
        return when {
            rssi <= MIN_WIFI_RSSI -> 0
            rssi >= MAX_WIFI_RSSI -> numLevels - 1
            else -> {
                val inputRange = (MAX_WIFI_RSSI - MIN_WIFI_RSSI).toFloat()
                val outputRange = (numLevels - 1).toFloat()
                ((rssi - MIN_WIFI_RSSI) * outputRange / inputRange).toInt()
                    .coerceIn(0, LockscreenSignalStatus.WIFI_BAND_COUNT)
            }
        }
    }

    private fun connectedWifiInfo(context: Context, wifi: WifiManager): WifiInfo? {
        val fromCapabilities = wifiInfoFromCapabilities(context)
        if (fromCapabilities != null) return fromCapabilities
        @Suppress("DEPRECATION")
        val legacy = wifi.connectionInfo ?: return null
        @Suppress("DEPRECATION")
        if (legacy.networkId == -1) return null
        return legacy
    }

    private fun wifiInfoFromCapabilities(context: Context): WifiInfo? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return null
        for (network in connectivity.allNetworks) {
            val caps = connectivity.getNetworkCapabilities(network) ?: continue
            if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) continue
            val transportInfo = caps.transportInfo
            if (transportInfo is WifiInfo) return transportInfo
        }
        return null
    }
}

fun interface CellularListenerHandle {
    fun unregister()
}
