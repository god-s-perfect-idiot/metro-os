package com.metro.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Test

class MetroColorsTest {
    @Test
    fun accentBlue_isDefault() {
        assertEquals(Color(0xFF1BA1E2), MetroColors.AccentBlue)
    }

    @Test
    fun accentPalette_hasTwentyColors() {
        assertEquals(20, MetroColors.AccentPalette.size)
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
    fun pageTitle_is64spLight() {
        val style = MetroTextStyle.PageTitle.toTextStyle()
        assertEquals(64f, style.fontSize.value, 0.01f)
        assertEquals(FontWeight.Light, style.fontWeight)
    }

    @Test
    fun body_is18sp() {
        assertEquals(18f, MetroTextStyle.Body.toTextStyle().fontSize.value, 0.01f)
    }

    @Test
    fun listItemSubtitle_is18sp() {
        assertEquals(18f, MetroTextStyle.ListItemSubtitle.toTextStyle().fontSize.value, 0.01f)
    }

    @Test
    fun hubTitle_is56sp() {
        assertEquals(56f, MetroTextStyle.HubTitle.toTextStyle().fontSize.value, 0.01f)
    }

    @Test
    fun chromeTitles_overflowAtScreenEdge() {
        assertEquals(true, MetroTextStyle.PageTitle.overflowsAtScreenEdge())
        assertEquals(true, MetroTextStyle.HubTitle.overflowsAtScreenEdge())
        assertEquals(true, MetroTextStyle.PivotTab.overflowsAtScreenEdge())
        assertEquals(false, MetroTextStyle.Body.overflowsAtScreenEdge())
        assertEquals(false, MetroTextStyle.ListItemTitle.overflowsAtScreenEdge())
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

    @Test
    fun pagePivotLoad_is200msWithHalfOpenSwing() {
        assertEquals(200, MetroTransitions.PagePivotLoadMs)
        assertEquals(22.5f, MetroTransitions.PagePivotLoadStartDegrees, 0f)
        assertEquals(0f, MetroTransitions.PagePivotLoadOriginX, 0f)
        assertEquals(0.15f, MetroTransitions.PagePivotLoadStartTranslationXFraction, 0f)
    }

    @Test
    fun pagePivotExit_tiltsBackIntoScreen() {
        assertEquals(-28f, MetroTransitions.PagePivotExitEndDegrees, 0f)
        assertEquals(0.15f, MetroTransitions.PagePivotExitOriginX, 0f)
        assertEquals(1.45f, MetroTransitions.PagePivotExitCameraWidthFactor, 0f)
        assertEquals(-0.15f, MetroTransitions.PagePivotExitTranslationXFraction, 0f)
    }

    @Test
    fun pagePivotCameraDistance_scalesWithWidth() {
        assertEquals(13.5f, metroPagePivotCameraDistance(1080f), 0.01f)
        assertEquals(9f, metroPagePivotCameraDistance(720f), 0.01f)
        assertEquals(8f, metroPagePivotCameraDistance(0f), 0f)
    }
}
