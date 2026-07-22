package com.metro.system

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MetroAppDiscoveryTest {
    private val apps = listOf(
        MetroAppInfo("com.metro.browser", "browser"),
        MetroAppInfo("com.metro.music", "music"),
        MetroAppInfo("com.metro.notes", "notes"),
    )

    @Test
    fun filterApps_matchesLabelSubstring() {
        val filtered = MetroAppDiscovery.filterApps(apps, "not")
        assertEquals(1, filtered.size)
        assertEquals("notes", filtered.first().label)
    }

    @Test
    fun filterApps_emptyQueryReturnsAll() {
        assertEquals(apps, MetroAppDiscovery.filterApps(apps, ""))
        assertEquals(apps, MetroAppDiscovery.filterApps(apps, "   "))
    }

    @Test
    fun isSystemApp_knownPlaceholderTreatedAsSystem() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageManager = context.packageManager
        assertTrue(MetroAppDiscovery.isSystemApp(packageManager, "com.metro.settings"))
    }

    @Test
    fun isSystemApp_metroSuitePrefixTreatedAsSystem() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageManager = context.packageManager
        assertTrue(MetroAppDiscovery.isSystemApp(packageManager, "com.metro.futureapp"))
    }

    @Test
    fun strongBrandHex_defaultsUnset() {
        assertEquals(null, MetroAppRegistry.strongBrandHex("com.metro.calculator"))
    }

    @Test
    fun discoverInstalledApps_doesNotInjectUninstalledPinnedPackages() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pinned = setOf("com.metro.settings", "com.metro.store")
        val discovered = MetroAppDiscovery.discoverInstalledApps(context, pinned)
        assertTrue(discovered.none { it.packageName == "com.metro.settings" })
        assertTrue(discovered.none { it.packageName == "com.metro.store" })
    }

    @Test
    fun tileData_hasFlipFaceWhenBackContentPresent() {
        val withBack = MetroTileData(
            title = "music",
            backgroundColorHex = "#A200FF",
            backFaceTitle = "now playing",
        )
        assertTrue(withBack.hasFlipFace)

        val static = MetroTileData(
            title = "settings",
            backgroundColorHex = "#F09609",
        )
        assertFalse(static.hasFlipFace)
    }
}
