package com.metro.statusbar

import android.telephony.TelephonyManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(org.robolectric.RobolectricTestRunner::class)
class DataConnectionLabelsTest {
    @Test
    fun maps5gNr() {
        assertEquals("5G", DataConnectionLabels.fromNetworkType(TelephonyManager.NETWORK_TYPE_NR))
    }

    @Test
    fun mapsLte() {
        assertEquals("4G", DataConnectionLabels.fromNetworkType(TelephonyManager.NETWORK_TYPE_LTE))
    }

    @Test
    fun mapsHspaPlusTo4g() {
        assertEquals("4G", DataConnectionLabels.fromNetworkType(TelephonyManager.NETWORK_TYPE_HSPAP))
    }

    @Test
    fun mapsUmtsTo3g() {
        assertEquals("3G", DataConnectionLabels.fromNetworkType(TelephonyManager.NETWORK_TYPE_UMTS))
    }

    @Test
    fun mapsEdgeTo2g() {
        assertEquals("2G", DataConnectionLabels.fromNetworkType(TelephonyManager.NETWORK_TYPE_EDGE))
    }

    @Test
    fun mapsGprsToG() {
        assertEquals("G", DataConnectionLabels.fromNetworkType(TelephonyManager.NETWORK_TYPE_GPRS))
    }

    @Test
    fun unknownNetworkType_isHidden() {
        assertNull(DataConnectionLabels.fromNetworkType(TelephonyManager.NETWORK_TYPE_UNKNOWN))
    }

    @Test
    fun mapsNrOverrideTo5g() {
        assertEquals(
            "5G",
            DataConnectionLabels.fromDisplayInfo(
                networkType = TelephonyManager.NETWORK_TYPE_LTE,
                overrideNetworkType = android.telephony.TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA,
            ),
        )
    }

    @Test
    fun mapsLteAdvancedOverrideTo4g() {
        assertEquals(
            "4G",
            DataConnectionLabels.fromDisplayInfo(
                networkType = TelephonyManager.NETWORK_TYPE_LTE,
                overrideNetworkType = android.telephony.TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO,
            ),
        )
    }
}
