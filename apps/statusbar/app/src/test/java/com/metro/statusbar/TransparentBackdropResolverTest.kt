package com.metro.statusbar

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TransparentBackdropResolverTest {
    @Test
    fun fromAppTheme_nullPackage_followsSystemTheme() {
        val context = RuntimeEnvironment.getApplication()
        assertEquals(
            Color.Black,
            TransparentBackdropResolver.fromAppTheme(
                context = context,
                packageName = null,
                systemDarkTheme = true,
            ),
        )
        assertEquals(
            Color.White,
            TransparentBackdropResolver.fromAppTheme(
                context = context,
                packageName = null,
                systemDarkTheme = false,
            ),
        )
    }

    @Test
    fun fromAppTheme_ignoredShellPackage_followsSystemTheme() {
        val context = RuntimeEnvironment.getApplication()
        assertEquals(
            Color.Black,
            TransparentBackdropResolver.fromAppTheme(
                context = context,
                packageName = "com.android.systemui",
                systemDarkTheme = true,
            ),
        )
    }
}
