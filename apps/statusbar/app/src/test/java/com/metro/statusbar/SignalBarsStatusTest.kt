package com.metro.statusbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SignalBarsStatusTest {
    @Test
    fun fromLevels_clampsCellularAndWifi() {
        val status = SignalBarsStatus.fromLevels(cellularLevel = 9, wifiBands = 5)
        assertEquals(SignalBarsStatus.CELLULAR_BAR_COUNT, status.cellularBars)
        assertEquals(SignalBarsStatus.WIFI_BAND_COUNT, status.wifiBands)
    }

    @Test
    fun fromLevels_preservesNullWifi() {
        val status = SignalBarsStatus.fromLevels(cellularLevel = 2, wifiBands = null)
        assertEquals(2, status.cellularBars)
        assertNull(status.wifiBands)
    }

    @Test
    fun wifiFromRssi_mapsStrongSignalToFullBands() {
        assertEquals(3, WifiSignalLevels.fromRssi(-40))
    }

    @Test
    fun wifiFromRssi_mapsWeakSignalToFewerBands() {
        assertEquals(0, WifiSignalLevels.fromRssi(-100))
        assertEquals(1, WifiSignalLevels.fromRssi(-85))
    }

    @Test
    fun visibleLeft_hidesWifiWhenDisconnected() {
        assertEquals(
            listOf(TrayIndicator.Cellular, TrayIndicator.DataConnection),
            TrayIndicatorOrder.visibleLeft(dataConnectionLabel = "4G", wifiConnected = false),
        )
    }

    @Test
    fun visibleLeft_includesWifiWhenConnected() {
        assertEquals(
            listOf(TrayIndicator.Cellular, TrayIndicator.DataConnection, TrayIndicator.Wifi),
            TrayIndicatorOrder.visibleLeft(dataConnectionLabel = "4G", wifiConnected = true),
        )
    }

    @Test
    fun visibleLeft_skipsDataLabelWhenAbsent() {
        assertEquals(
            listOf(TrayIndicator.Cellular, TrayIndicator.Wifi),
            TrayIndicatorOrder.visibleLeft(dataConnectionLabel = null, wifiConnected = true),
        )
    }
}
