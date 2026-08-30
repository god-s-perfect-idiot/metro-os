package com.metro.lockscreen

import androidx.compose.ui.graphics.Color
import com.metro.ui.MetroColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockscreenBackgroundModeTest {
    @Test
    fun fromStorage_defaultsToAccent() {
        assertEquals(LockscreenBackgroundMode.Accent, LockscreenBackgroundMode.fromStorage(null))
        assertEquals(LockscreenBackgroundMode.Accent, LockscreenBackgroundMode.fromStorage(""))
        assertEquals(LockscreenBackgroundMode.Accent, LockscreenBackgroundMode.fromStorage("accent"))
        assertEquals(LockscreenBackgroundMode.Accent, LockscreenBackgroundMode.fromStorage("unknown"))
    }

    @Test
    fun fromStorage_parsesCustomAndBing() {
        assertEquals(LockscreenBackgroundMode.Custom, LockscreenBackgroundMode.fromStorage("custom"))
        assertEquals(LockscreenBackgroundMode.Custom, LockscreenBackgroundMode.fromStorage("CUSTOM"))
        assertEquals(LockscreenBackgroundMode.Bing, LockscreenBackgroundMode.fromStorage("bing"))
    }

    @Test
    fun toStorage_roundTrips() {
        for (mode in LockscreenBackgroundMode.entries) {
            assertEquals(mode, LockscreenBackgroundMode.fromStorage(mode.toStorage()))
        }
    }

    @Test
    fun fill_withoutBitmap_usesAccentContrast() {
        val accent = Color(0xFF1BA1E2)
        val fill = LockscreenFill(
            mode = LockscreenBackgroundMode.Accent,
            accentColor = accent,
            bitmap = null,
        )
        assertFalse(fill.usesPhoto)
        assertEquals(MetroColors.tileContentColor(accent), fill.contentColor)
        assertTrue(fill.contentColor == Color.White)
    }
}
