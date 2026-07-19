package com.metro.system

import android.graphics.drawable.ColorDrawable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MetroAppBrandingTest {

    @Test
    fun metroGlyphDrawable_passesThroughNonAdaptive() {
        val drawable = ColorDrawable(android.graphics.Color.RED)
        assertSame(drawable, MetroAppBranding.metroGlyphDrawable(drawable))
    }

    @Test
    fun adaptiveSafeZoneScale_matchesAndroidSpec() {
        assertEquals(1.5f, MetroAppBranding.ADAPTIVE_SAFE_ZONE_SCALE, 0.001f)
    }
}
