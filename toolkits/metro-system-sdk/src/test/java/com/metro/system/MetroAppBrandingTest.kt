package com.metro.system

import android.content.Context
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MetroAppBrandingTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        MetroPreferences(context).accentColorHex = MetroPreferences.DEFAULT_ACCENT_HEX
    }

    @Test
    fun metroGlyphDrawable_passesThroughNonAdaptive() {
        val drawable = ColorDrawable(android.graphics.Color.RED)
        assertSame(drawable, MetroAppBranding.metroGlyphDrawable(drawable))
    }

    @Test
    fun adaptiveSafeZoneScale_matchesAndroidSpec() {
        assertEquals(1.5f, MetroAppBranding.ADAPTIVE_SAFE_ZONE_SCALE, 0.001f)
    }

    @Test
    fun metroSuiteTile_usesSystemAccentNotBrandCatalog() {
        val accent = MetroPreferences(context).accentColor
        val fill = MetroAppBranding.resolveTileBackgroundColor(
            context = context,
            packageName = "com.metro.calculator",
            providerBackgroundHex = "#007500",
        )
        assertEquals(accent, fill)
    }

    @Test
    fun androidSystemStylePackage_usesAccentWhenUninstalledPlaceholder() {
        // Known suite treated as system even without FLAG_SYSTEM install.
        val accent = Color(0xFFE51400)
        MetroPreferences(context).accentColorHex = "#E51400"
        val fill = MetroAppBranding.resolveTileBackgroundColor(
            context = context,
            packageName = "com.metro.settings",
        )
        assertEquals(accent, fill)
    }

    @Test
    fun thirdPartyWithoutIcon_fallsBackToAccent() {
        val accent = MetroPreferences(context).accentColor
        val fill = MetroAppBranding.resolveTileBackgroundColor(
            context = context,
            packageName = "com.example.thirdparty.app",
        )
        assertEquals(accent, fill)
    }
}
