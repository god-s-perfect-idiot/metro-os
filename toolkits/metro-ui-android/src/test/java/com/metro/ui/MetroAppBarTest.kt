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
        assertEquals(5, MetroAppBarDefaults.MaxMenuItems)
        assertEquals(52f, MetroAppBarDefaults.BarHeight.value, 0.01f)
        assertEquals(42f, MetroAppBarDefaults.GlyphSize.value, 0.01f)
    }
}
