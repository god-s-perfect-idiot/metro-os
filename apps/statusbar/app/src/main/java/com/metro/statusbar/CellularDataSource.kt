package com.metro.statusbar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/**
 * Maps Android radio network types to WP8.1-style data connection labels shown after the signal bars.
 */
object DataConnectionLabels {
    /** [TelephonyManager.NETWORK_TYPE_LTE_CA] — not exposed on all compile SDKs. */
    private const val NETWORK_TYPE_LTE_CA = 19

    fun fromDisplayInfo(
        networkType: Int,
        overrideNetworkType: Int,
    ): String? = when (overrideNetworkType) {
        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA,
        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> "5G"
        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA,
        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO -> "4G"
        else -> fromNetworkType(networkType)
    }

    fun fromNetworkType(networkType: Int): String? = when (networkType) {
        TelephonyManager.NETWORK_TYPE_NR -> "5G"
        TelephonyManager.NETWORK_TYPE_LTE -> "4G"
        NETWORK_TYPE_LTE_CA -> "4G"
        TelephonyManager.NETWORK_TYPE_HSPAP -> "4G"
        TelephonyManager.NETWORK_TYPE_HSPA,
        TelephonyManager.NETWORK_TYPE_HSDPA,
        TelephonyManager.NETWORK_TYPE_HSUPA,
        TelephonyManager.NETWORK_TYPE_UMTS,
        TelephonyManager.NETWORK_TYPE_EVDO_0,
        TelephonyManager.NETWORK_TYPE_EVDO_A,
        TelephonyManager.NETWORK_TYPE_EVDO_B,
        TelephonyManager.NETWORK_TYPE_EHRPD,
        TelephonyManager.NETWORK_TYPE_1xRTT -> "3G"
        TelephonyManager.NETWORK_TYPE_EDGE -> "2G"
        TelephonyManager.NETWORK_TYPE_GPRS -> "G"
        TelephonyManager.NETWORK_TYPE_CDMA,
        TelephonyManager.NETWORK_TYPE_IDEN -> "2G"
        TelephonyManager.NETWORK_TYPE_UNKNOWN,
        TelephonyManager.NETWORK_TYPE_IWLAN -> null
        else -> null
    }
}

object CellularDataSource {
    fun canRead(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    fun current(context: Context): String? {
        if (!canRead(context)) return null
        val telephony = context.getSystemService(TelephonyManager::class.java) ?: return null
        return runCatching {
            if (telephony.simState != TelephonyManager.SIM_STATE_READY) return null

            @Suppress("DEPRECATION")
            val networkType = telephony.dataNetworkType
            DataConnectionLabels.fromNetworkType(networkType)
        }.getOrNull()
    }
}
