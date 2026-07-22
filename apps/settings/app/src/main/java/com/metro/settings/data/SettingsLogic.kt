package com.metro.settings.data

import com.metro.system.MetroFontScale
import com.metro.system.MetroThemeMode

/**
 * Pure helpers for settings preference validation (unit-tested).
 */
object SettingsLogic {
    fun normalizeTheme(storage: String?): MetroThemeMode =
        MetroThemeMode.fromStorage(storage)

    fun snapFontScale(value: Float): Float =
        MetroFontScale.coerceToStep(value)

    fun fontScaleIndex(value: Float): Int =
        MetroFontScale.indexOf(value)
}
