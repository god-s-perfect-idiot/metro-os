package com.metro.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetroColorsTest {
    @Test
    fun accentBlue_isDefault() {
        assertEquals(Color(0xFF1BA1E2), MetroColors.AccentBlue)
    }

    @Test
    fun accentPalette_hasNineColors() {
        assertEquals(9, MetroColors.AccentPalette.size)
    }

    @Test
    fun darkBackground_isBlack() {
        assertEquals(Color(0xFF000000), MetroColors.DarkBackground)
    }

    @Test
    fun tileContentColor_usesBlackOnLightBackground() {
        assertEquals(MetroColors.LightPrimaryText, MetroColors.tileContentColor(Color.White))
        assertEquals(MetroColors.LightPrimaryText, MetroColors.tileContentColor(Color(0xFFF2F2F2)))
    }

    @Test
    fun tileContentColor_usesWhiteOnDarkBackground() {
        assertEquals(MetroColors.TileContentOnAccent, MetroColors.tileContentColor(Color.Black))
        assertEquals(MetroColors.TileContentOnAccent, MetroColors.tileContentColor(MetroColors.AccentBlue))
    }
}

class MetroTextStyleTest {
    @Test
    fun pageTitle_is64sp() {
        assertEquals(64f, MetroTextStyle.PageTitle.toTextStyle().fontSize.value, 0.01f)
    }

    @Test
    fun body_meetsMinimum15sp() {
        assertTrue(MetroTextStyle.Body.toTextStyle().fontSize.value >= 15f)
    }

    @Test
    fun hubTitle_is56sp() {
        assertEquals(56f, MetroTextStyle.HubTitle.toTextStyle().fontSize.value, 0.01f)
    }
}

class MetroTransitionsTest {
    @Test
    fun pageTransition_is300ms() {
        assertEquals(300, MetroTransitions.PageTransitionMs)
    }

    @Test
    fun tileFlip_is600ms() {
        assertEquals(600, MetroTransitions.TileFlipMs)
    }

    @Test
    fun jumpListFlip_is300msWith40msStagger() {
        assertEquals(300, MetroTransitions.JumpListFlipMs)
        assertEquals(40, MetroTransitions.JumpListFlipStaggerMs)
    }
}
