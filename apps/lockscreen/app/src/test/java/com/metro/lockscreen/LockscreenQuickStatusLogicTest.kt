package com.metro.lockscreen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockscreenQuickStatusLogicTest {
    @Test
    fun formatCount_capsAt99Plus() {
        assertEquals("", LockscreenQuickStatusLogic.formatCount(0))
        assertEquals("1", LockscreenQuickStatusLogic.formatCount(1))
        assertEquals("99", LockscreenQuickStatusLogic.formatCount(99))
        assertEquals("99+", LockscreenQuickStatusLogic.formatCount(100))
        assertEquals("99+", LockscreenQuickStatusLogic.formatCount(500))
    }

    @Test
    fun shouldShowQuickStatus_requiresPositiveCount() {
        assertFalse(LockscreenQuickStatusLogic.shouldShowQuickStatus(0))
        assertTrue(LockscreenQuickStatusLogic.shouldShowQuickStatus(1))
    }

    @Test
    fun notificationIconRes_resolvesSuiteApps() {
        assertEquals(
            com.metro.ui.MetroAppGlyphs.NotificationPhone,
            LockscreenQuickStatusLogic.notificationIconRes("com.metro.dialer"),
        )
        assertEquals(
            com.metro.ui.MetroAppGlyphs.Messaging,
            LockscreenQuickStatusLogic.notificationIconRes("com.metro.messaging"),
        )
    }

    @Test
    fun quickStatusIcon_hasIconWhenGlyphResolved() {
        val icon = QuickStatusIcon(
            glyphResId = com.metro.ui.MetroAppGlyphs.Calendar,
        )
        assertTrue(icon.hasIcon)
    }
}
