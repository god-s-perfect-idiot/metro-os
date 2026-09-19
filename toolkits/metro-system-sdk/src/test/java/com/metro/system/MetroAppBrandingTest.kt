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

    @Test
    fun resolveIconForegroundColor_usesDrawableColorWhenNonAdaptive() {
        val icon = ColorDrawable(android.graphics.Color.rgb(0xE5, 0x14, 0x00))
        val fill = MetroAppBranding.resolveIconForegroundColor(
            context = context,
            packageName = "com.example.thirdparty.app",
            drawable = icon,
        )
        assertEquals(Color(0xFFE51400), fill)
    }

    @Test
    fun resolveIconForegroundColor_missingIcon_fallsBackToBlackNotAccent() {
        val fill = MetroAppBranding.resolveIconForegroundColor(
            context = context,
            packageName = "com.example.missing.icon.app",
        )
        assertEquals(Color.Black, fill)
    }

    @Test
    fun resolveIconForegroundColor_colorDrawableBackgroundWinsOverGlyph() {
        // Non-adaptive solid brand (same path as adaptive background ColorDrawable).
        val brand = ColorDrawable(android.graphics.Color.rgb(0x25, 0xD3, 0x66))
        val fill = MetroAppBranding.resolveIconForegroundColor(
            context = context,
            packageName = "com.whatsapp",
            drawable = brand,
        )
        assertEquals(Color(0xFF25D366), fill)
    }
}
