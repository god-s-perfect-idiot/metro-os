package com.metro.system

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MetroPreferencesTest {
    private lateinit var prefs: MetroPreferences

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences(MetroPreferenceKeys.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        prefs = MetroPreferences(context)
    }

    @Test
    fun defaults_areDarkAndBlue() {
        assertTrue(prefs.isDark)
        assertEquals("#1BA1E2", prefs.accentColorHex)
    }

    @Test
    fun themeMode_roundTrip() {
        prefs.themeMode = MetroThemeMode.Light
        assertFalse(prefs.isDark)
        prefs.themeMode = MetroThemeMode.Dark
        assertTrue(prefs.isDark)
    }

    @Test
    fun accentColor_roundTrip() {
        prefs.accentColorHex = "#339933"
        assertEquals("#339933", prefs.accentColorHex)
    }

    @Test
    fun cacheThemeSnapshot_updatesLocalWithoutRequiringProvider() {
        prefs.cacheThemeSnapshot(
            themeMode = MetroThemeMode.Light,
            accentColorHex = "#E51400",
            fontScale = 1.15f,
        )
        assertFalse(prefs.isDark)
        assertEquals("#E51400", prefs.accentColorHex)
        assertEquals(1.15f, prefs.fontScale)
    }

    @Test
    fun fontScale_roundTrip() {
        prefs.fontScale = 1.3f
        assertEquals(1.3f, prefs.fontScale)
    }
}
