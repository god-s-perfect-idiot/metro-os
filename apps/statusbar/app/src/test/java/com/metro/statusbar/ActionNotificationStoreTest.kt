package com.metro.statusbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ActionNotificationStoreTest {
    @Test
    fun formatTime_sameDayUsesCompactAmpm() {
        val now = ZonedDateTime.of(2026, 5, 25, 13, 0, 0, 0, ZoneId.systemDefault())
        val posted = now.withHour(12).withMinute(49).toInstant().toEpochMilli()
        val text = ActionNotificationStore.formatTime(posted, now)
        assertTrue(text.endsWith("a") || text.endsWith("p"))
        assertFalse(text.contains("AM") || text.contains("PM"))
    }

    @Test
    fun formatTime_otherDayUsesMonthDay() {
        val now = ZonedDateTime.of(2026, 5, 25, 13, 0, 0, 0, ZoneId.systemDefault())
        val posted = now.minusDays(2).toInstant().toEpochMilli()
        val text = ActionNotificationStore.formatTime(posted, now)
        assertTrue(text.contains("/"))
    }

    @Test
    fun ignoredPackages_includeShellOverlays() {
        assertTrue("com.metro.statusbar" in ActionNotificationStore.IgnoredPackages)
        assertTrue("com.metro.navbar" in ActionNotificationStore.IgnoredPackages)
        assertTrue("com.metro.launcher" in ActionNotificationStore.IgnoredPackages)
    }

    @Test
    fun quickActionLabels_matchWpDefaults() {
        assertEquals("WI-FI", QuickActionController.labelFor(QuickActionType.Wifi))
        assertEquals("BLUETOOTH", QuickActionController.labelFor(QuickActionType.Bluetooth))
        assertEquals("AIRPLANE MODE", QuickActionController.labelFor(QuickActionType.Airplane))
        assertEquals("INTERNET SHARING", QuickActionController.labelFor(QuickActionType.InternetSharing))
    }
}
