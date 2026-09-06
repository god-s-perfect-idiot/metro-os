package com.metro.statusbar

import androidx.compose.ui.graphics.Color
import com.metro.system.MetroPreferences
import com.metro.system.MetroStatusBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TrayShellFillTest {
    private lateinit var trayState: TrayState

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        MetroPreferences(context).themeMode = com.metro.system.MetroThemeMode.Dark
        trayState = TrayState(context)
    }

    @Test
    fun volumeOutranksNotifications() {
        trayState.applyShellFill(MetroStatusBar.OWNER_NOTIFICATIONS, Color(0xFF1BA1E2))
        assertEquals(Color(0xFF1BA1E2), trayState.theme.backgroundColor)

        trayState.applyShellFill(MetroStatusBar.OWNER_VOLUME, Color(0xFF252525))
        assertEquals(Color(0xFF252525), trayState.theme.backgroundColor)

        trayState.applyShellFill(MetroStatusBar.OWNER_VOLUME, null)
        assertEquals(Color(0xFF1BA1E2), trayState.theme.backgroundColor)

        trayState.applyShellFill(MetroStatusBar.OWNER_NOTIFICATIONS, null)
        assertNull(trayState.notificationsShellFill)
        assertNull(trayState.volumeShellFill)
    }

    @Test
    fun unknownOwner_isIgnored() {
        trayState.applyShellFill("unknown", Color(0xFFFF0000))
        assertNull(trayState.notificationsShellFill)
        assertNull(trayState.volumeShellFill)
    }
}
