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
    fun snapFontScale_usesSevenSteps() {
        assertEquals(1.0f, SettingsLogic.snapFontScale(1.01f), 0.001f)
        assertEquals(1.6f, SettingsLogic.snapFontScale(1.55f), 0.001f)
        assertEquals(0, SettingsLogic.fontScaleIndex(0.85f))
        assertEquals(6, SettingsLogic.fontScaleIndex(1.6f))
    }
}
