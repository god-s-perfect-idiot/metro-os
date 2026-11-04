package com.metro.feedback.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogcatCollectorTest {
    @Test
    fun filterMetroLines_keepsPackageAndTagHits() {
        val raw = """
            01-01 00:00:00.000  1000 1000 I ActivityManager: Start proc com.metro.launcher
            01-01 00:00:01.000  1001 1001 D Chrome: unrelated
            01-01 00:00:02.000  1002 1002 E MetroTheme: accent changed
            01-01 00:00:03.000  1003 1003 W System: com.metro.feedback died
        """.trimIndent()

        val filtered = LogcatCollector.filterMetroLines(
            raw = raw,
            metroPackages = setOf("com.metro.launcher", "com.metro.feedback"),
        )

        assertEquals(3, filtered.size)
        assertTrue(filtered[0].contains("com.metro.launcher"))
        assertTrue(filtered[1].contains("MetroTheme"))
        assertTrue(filtered[2].contains("com.metro.feedback"))
    }

    @Test
    fun formatBytes_scales() {
        assertEquals("512 B", GitHubIssueLauncher.formatBytes(512))
        assertEquals("1.5 KB", GitHubIssueLauncher.formatBytes(1536))
        assertEquals("1.0 MB", GitHubIssueLauncher.formatBytes(1024L * 1024L))
    }
}
