package com.metro.launcher

import com.metro.launcher.data.AppLauncherOption
import com.metro.launcher.data.AppLauncherOptions
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLauncherOptionsTest {
    @Test
    fun dedupeByShortcutId_keepsFirstEntry() {
        val first = AppLauncherOption("com.example", "a", "First")
        val duplicate = AppLauncherOption("com.example", "a", "Duplicate")

        assertEquals(listOf(first), AppLauncherOptions.dedupeByShortcutId(listOf(first, duplicate)))
    }
}
