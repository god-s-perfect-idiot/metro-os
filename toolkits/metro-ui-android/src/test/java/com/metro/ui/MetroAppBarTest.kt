package com.metro.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetroAppBarTest {
    @Test
    fun iconFactory_defaultsContentDescriptionToLabel() {
        val icon = MetroAppBarIcon(
            type = MetroSystemIconType.Add,
            label = "new",
            onClick = {},
        )
        assertEquals("new", icon.label)
        assertEquals("new", icon.contentDescription)
        assertTrue(icon.enabled)
    }

    @Test
    fun iconFactory_keepsExplicitContentDescriptionAndEnabled() {
        val icon = MetroAppBarIcon(
            type = MetroSystemIconType.Search,
            label = "search",
            onClick = {},
            contentDescription = "find messages",
            enabled = false,
        )
        assertEquals("find messages", icon.contentDescription)
        assertFalse(icon.enabled)
    }

    @Test
    fun menuItem_defaultsToEnabled() {
        val item = MetroAppBarMenuItem("settings") {}
        assertEquals("settings", item.text)
        assertTrue(item.enabled)
    }

    @Test
    fun defaults_matchUxSpecLimits() {
        assertEquals(4, MetroAppBarDefaults.MaxIcons)
        assertEquals(3, MetroAppBarDefaults.MaxTextButtons)
        assertEquals(5, MetroAppBarDefaults.MaxMenuItems)
        assertEquals(52f, MetroAppBarDefaults.BarHeight.value, 0.01f)
        assertEquals(42f, MetroAppBarDefaults.GlyphSize.value, 0.01f)
        assertEquals(MetroColors.DarkSecondarySurface, MetroAppBarDefaults.ChromeBackground)
        assertEquals(6f, MetroAppBarDefaults.EllipsisTopPadding.value, 0.01f)
        assertEquals(9f, MetroAppBarDefaults.EllipsisDotSpacing.value, 0.01f)
        assertEquals(12f, MetroAppBarDefaults.TextButtonSpacing.value, 0.01f)
        assertEquals(6f, MetroAppBarDefaults.TextButtonVerticalInset.value, 0.01f)
        assertEquals(48f, MetroAppBarDefaults.TextButtonRowEndInset.value, 0.01f)
    }

    @Test
    fun textButton_defaultsToEnabled() {
        val button = MetroAppBarTextButton("download") {}
        assertEquals("download", button.text)
        assertTrue(button.enabled)
    }

    @Test
    fun textButton_keepsExplicitEnabled() {
        val button = MetroAppBarTextButton("share", enabled = false) {}
        assertFalse(button.enabled)
    }

    @Test
    fun appBarMotion_matchesReferenceCss() {
        assertEquals(200, MetroTransitions.AppBarSlideMs)
        assertEquals(500, MetroTransitions.AppBarButtonOvershootMs)
        assertEquals(1.2f, MetroTransitions.AppBarButtonStartOffsetFraction)
        assertEquals(-0.2f, MetroTransitions.AppBarButtonOvershootPeakOffsetFraction)
    }
}
