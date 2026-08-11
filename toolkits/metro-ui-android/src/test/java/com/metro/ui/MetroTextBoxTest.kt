package com.metro.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class MetroTextBoxTest {
    @Test
    fun tokens_matchUxSpec() {
        assertEquals(3f, MetroTextBoxDefaults.BorderWidth.value, 0.01f)
        assertEquals(44f, MetroTextBoxDefaults.MinHeight.value, 0.01f)
        assertEquals(10f, MetroTextBoxDefaults.HorizontalPadding.value, 0.01f)
    }

    @Test
    fun fill_isWhiteWhenFocused_grayWhenRest() {
        assertEquals(MetroColors.LightBackground, MetroTextBoxDefaults.fill(focused = true))
        assertEquals(MetroColors.LightSecondarySurface, MetroTextBoxDefaults.fill(focused = false))
    }

    @Test
    fun border_isAccentWhenFocused() {
        val accent = MetroColors.AccentTeal
        assertEquals(accent, MetroTextBoxDefaults.borderColor(focused = true, accent = accent))
        assertEquals(
            MetroTextBoxDefaults.RestBorder,
            MetroTextBoxDefaults.borderColor(focused = false, accent = accent),
        )
    }

    @Test
    fun text_isAlwaysBlack() {
        assertEquals(MetroColors.LightPrimaryText, MetroTextBoxDefaults.TextColor)
        assertEquals(Color(0xFF000000), MetroTextBoxDefaults.TextColor)
    }

    @Test
    fun placeholder_isSixtyPercentBlack() {
        assertEquals(0.6f, MetroTextBoxDefaults.PlaceholderColor.alpha, 0.01f)
        assertEquals(MetroColors.LightPrimaryText.red, MetroTextBoxDefaults.PlaceholderColor.red, 0.01f)
    }
}
