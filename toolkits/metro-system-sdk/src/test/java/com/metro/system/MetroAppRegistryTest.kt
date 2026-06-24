package com.metro.system

import org.junit.Assert.assertEquals
import org.junit.Test

class MetroAppRegistryTest {
    @Test
    fun knownApps_haveLabelsAndBrandColors() {
        assertEquals("Internet Explorer", MetroAppRegistry.label("com.metro.browser"))
        assertEquals("#1BA1E2", MetroAppRegistry.brandHex("com.metro.browser"))
        assertEquals("Music", MetroAppRegistry.label("com.metro.music"))
    }

    @Test
    fun unknownPackage_returnsNull() {
        assertEquals(null, MetroAppRegistry.label("com.example.unknown"))
    }
}
