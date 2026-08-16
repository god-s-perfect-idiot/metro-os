package com.metro.statusbar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/**
 * Decoupled radio-strength snapshot so cellular / Wi-Fi telemetry can swap without touching
 * glyph rendering (README § Data and state model).
 *
 * @param cellularBars filled cellular bars in `0..CELLULAR_BAR_COUNT`
 * @param wifiBands filled Wi-Fi arcs in `0..WIFI_BAND_COUNT`, or `null` when Wi-Fi is off /
 *   disconnected (icon hidden)
 */
data class SignalBarsStatus(
    val cellularBars: Int,
    val wifiBands: Int?,
) {
    companion object {
        const val CELLULAR_BAR_COUNT = 4
        const val WIFI_BAND_COUNT = 3

        /** Neutral full-strength fallback used before the first telemetry read. */
        val Unknown = SignalBarsStatus(
            cellularBars = CELLULAR_BAR_COUNT,
            wifiBands = WIFI_BAND_COUNT,
        )

        fun fromLevels(cellularLevel: Int, wifiBands: Int?): SignalBarsStatus = SignalBarsStatus(
            cellularBars = cellularLevel.coerceIn(0, CELLULAR_BAR_COUNT),
            wifiBands = wifiBands?.coerceIn(0, WIFI_BAND_COUNT),
        )
    }
}

/**
 * Maps Android [SignalStrength.getLevel] (`0..4`) onto the WP tray's four cellular bars.
 */
object CellularSignalLevels {
    fun fromSignalStrength(signalStrength: SignalStrength?): Int {
        if (signalStrength == null) return 0
        return signalStrength.level.coerceIn(0, SignalBarsStatus.CELLULAR_BAR_COUNT)
    }
}

/**
 * Maps Wi-Fi RSSI onto the WP tray's three quarter-band arcs (`0..3`).
 *
 * Uses the same thresholds as [WifiManager.calculateSignalLevel] (`MIN_RSSI=-100`,
 * `MAX_RSSI=-55`) so unit tests do not depend on the Android framework stub.
 */
object WifiSignalLevels {
    /** Matches [WifiInfo.INVALID_RSSI] / undocumented sentinel used when RSSI is unavailable. */
    const val INVALID_RSSI = -127

    private const val MIN_RSSI = -100
    private const val MAX_RSSI = -55

    fun fromRssi(rssi: Int): Int {
        val numLevels = SignalBarsStatus.WIFI_BAND_COUNT + 1
        return when {
            rssi <= MIN_RSSI -> 0
            rssi >= MAX_RSSI -> numLevels - 1
            else -> {
                val inputRange = (MAX_RSSI - MIN_RSSI).toFloat()
                val outputRange = (numLevels - 1).toFloat()
                ((rssi - MIN_RSSI) * outputRange / inputRange).toInt()
                    .coerceIn(0, SignalBarsStatus.WIFI_BAND_COUNT)
            }
        }
    }
}

object CellularSignalSource {
    fun canRead(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    fun currentBars(context: Context): Int {
        if (!canRead(context)) return 0
        val telephony = context.getSystemService(TelephonyManager::class.java) ?: return 0
        return runCatching {
            if (telephony.simState != TelephonyManager.SIM_STATE_READY) return 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                CellularSignalLevels.fromSignalStrength(telephony.signalStrength)
            } else {
                0
            }
        }.getOrDefault(0)
    }
}

object WifiSignalSource {
    /**
     * Current Wi-Fi band count, or `null` when Wi-Fi is disabled / not associated.
     * Uses `ACCESS_WIFI_STATE` (normal permission) — no location grant required for RSSI of the
     * active connection.
     */
    fun currentBands(context: Context): Int? {
        val appContext = context.applicationContext
        val wifi = appContext.getSystemService(WifiManager::class.java) ?: return null
        if (!wifi.isWifiEnabled) return null

        val info = connectedWifiInfo(appContext, wifi) ?: return null
        val rssi = info.rssi
        if (rssi <= WifiSignalLevels.INVALID_RSSI) return null
        return WifiSignalLevels.fromRssi(rssi)
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

object RadioSignalSource {
    fun current(context: Context): SignalBarsStatus = SignalBarsStatus.fromLevels(
        cellularLevel = CellularSignalSource.currentBars(context),
        wifiBands = WifiSignalSource.currentBands(context),
    )
}
