package com.metro.statusbar

import com.metro.system.MetroStatusBar
import org.junit.Assert.assertEquals
import org.junit.Test

class TrayVisibilityModeTest {
    @Test
    fun fromContract_mapsKnownModes() {
        assertEquals(TrayVisibilityMode.Opaque, TrayVisibilityMode.fromContract(MetroStatusBar.MODE_OPAQUE))
        assertEquals(TrayVisibilityMode.Translucent, TrayVisibilityMode.fromContract(MetroStatusBar.MODE_TRANSLUCENT))
        assertEquals(TrayVisibilityMode.Hidden, TrayVisibilityMode.fromContract(MetroStatusBar.MODE_HIDDEN))
    }

    @Test
    fun fromContract_unknownOrNull_defaultsToOpaque() {
        assertEquals(TrayVisibilityMode.Opaque, TrayVisibilityMode.fromContract(null))
        assertEquals(TrayVisibilityMode.Opaque, TrayVisibilityMode.fromContract("bogus"))
    }
}
