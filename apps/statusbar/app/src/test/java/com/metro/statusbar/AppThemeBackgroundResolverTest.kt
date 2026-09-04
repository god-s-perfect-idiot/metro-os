package com.metro.statusbar

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppThemeBackgroundResolverTest {
    @Test
    fun fromArgb_preservesPackedColor() {
        assertEquals(0xFFF2F2F2.toInt(), AppThemeBackgroundResolver.fromArgb(0xFFF2F2F2.toInt()).let {
            val a = (it.alpha * 255f + 0.5f).toInt()
            val r = (it.red * 255f + 0.5f).toInt()
            val g = (it.green * 255f + 0.5f).toInt()
            val b = (it.blue * 255f + 0.5f).toInt()
            (a shl 24) or (r shl 16) or (g shl 8) or b
        })
    }

    @Test
    fun isMateriallyDifferent_ignoresTinyNoise() {
        val base = Color(0.1f, 0.2f, 0.3f, 1f)
        assertFalse(
            AppThemeBackgroundResolver.isMateriallyDifferent(
                base,
                Color(0.11f, 0.20f, 0.30f, 1f),
            ),
        )
        assertTrue(
            AppThemeBackgroundResolver.isMateriallyDifferent(
                base,
                Color(0.5f, 0.2f, 0.3f, 1f),
            ),
        )
        assertTrue(AppThemeBackgroundResolver.isMateriallyDifferent(null, base))
    }

    @Test
    fun isLight_threshold() {
        assertTrue(AppThemeBackgroundResolver.isLight(Color.White))
        assertFalse(AppThemeBackgroundResolver.isLight(Color.Black))
    }
}
