package com.metro.settings.data

import com.metro.system.MetroThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsLogicTest {
    @Test
    fun normalizeTheme_defaultsDark() {
        assertEquals(MetroThemeMode.Dark, SettingsLogic.normalizeTheme(null))
        assertEquals(MetroThemeMode.Light, SettingsLogic.normalizeTheme("light"))
    }

    @Test
    fun snapFontScale_usesTenSteps() {
        assertEquals(1.0f, SettingsLogic.snapFontScale(1.01f), 0.001f)
        assertEquals(1.6f, SettingsLogic.snapFontScale(1.55f), 0.001f)
        assertEquals(0.625f, SettingsLogic.snapFontScale(0.6f), 0.001f)
        assertEquals(0, SettingsLogic.fontScaleIndex(0.625f))
        assertEquals(3, SettingsLogic.fontScaleIndex(0.85f))
        assertEquals(5, SettingsLogic.fontScaleIndex(1.0f))
        assertEquals(9, SettingsLogic.fontScaleIndex(1.6f))
    }

    @Test
    fun brightness_roundTrips() {
        assertEquals(128, SettingsLogic.brightnessToSystem(0.5f))
        assertEquals(1, SettingsLogic.brightnessToSystem(0f))
        assertEquals(255, SettingsLogic.brightnessToSystem(1f))
        assertEquals(0.5f, SettingsLogic.brightnessFromSystem(128), 0.01f)
    }

    @Test
    fun formatBytes_usesUnits() {
        assertEquals("512 B", SettingsLogic.formatBytes(512))
        assertEquals("1.0 GB", SettingsLogic.formatBytes(1_073_741_824L))
    }

    @Test
    fun displayOrDash_blankBecomesDash() {
        assertEquals("—", SettingsLogic.displayOrDash(null))
        assertEquals("—", SettingsLogic.displayOrDash("  "))
        assertEquals("—", SettingsLogic.displayOrDash("unknown"))
        assertEquals("Lumia", SettingsLogic.displayOrDash("Lumia"))
    }

    @Test
    fun metroOsVersion_isLatestRelease() {
        assertEquals("alpha-3", SettingsLogic.METRO_OS_VERSION)
    }
}
