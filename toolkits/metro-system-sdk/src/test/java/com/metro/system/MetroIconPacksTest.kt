package com.metro.system

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MetroIconPacksTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        MetroIconPacks.clearCache()
        MetroPreferences(context).iconPackPackage = null
    }

    @Test
    fun listInstalled_returnsEmptyWithoutThemePacks() {
        assertTrue(MetroIconPacks.listInstalled(context).isEmpty())
    }

    @Test
    fun loadIconForPackage_returnsNullWhenNoPackSelected() {
        assertEquals(null, MetroIconPacks.loadIconForPackage(context, "com.metro.settings"))
    }

    @Test
    fun loadIconForPackage_returnsNullForMissingPack() {
        assertEquals(
            null,
            MetroIconPacks.loadIconForPackage(
                context,
                "com.example.missing.iconpack",
                "com.metro.settings",
            ),
        )
    }
}
