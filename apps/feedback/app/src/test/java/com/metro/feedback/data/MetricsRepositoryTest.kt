package com.metro.feedback.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricsRepositoryTest {
    @Test
    fun parseMeminfoPss_readsModernRssAndPssLines() {
        val dump = """
            Applications Memory Usage (in Kilobytes):

            Total RSS by process:
                364,460K: com.metro.launcher (pid 1255 / activities)
                 90,560K: com.metro.notifications (pid 20787)
                 1,024K: com.android.systemui (pid 999)

            Total PSS by process:
                277,623K: com.metro.launcher (pid 1255 / activities)
                 45,231 kB: com.metro.statusbar (pid 1235)
                 4,096 kB: com.metro.launcher:tile (pid 1236)
        """.trimIndent()

        val parsed = MetricsRepository.parseMeminfoPss(
            dump = dump,
            metroPackages = setOf(
                "com.metro.launcher",
                "com.metro.statusbar",
                "com.metro.notifications",
                "com.metro.feedback",
            ),
        )

        // Prefers PSS section (skips RSS) and sums :tile into launcher.
        assertEquals((277623L + 4096L) * 1024L, parsed["com.metro.launcher"])
        assertEquals(45231L * 1024L, parsed["com.metro.statusbar"])
        // notifications only appeared under RSS — ignored when PSS section exists.
        assertTrue(!parsed.containsKey("com.metro.notifications"))
        assertTrue(!parsed.containsKey("com.metro.feedback"))
    }

    @Test
    fun parsePackageTotalPss_readsTotalPssLine() {
        val dump = """
            App Summary
                       Pss(KB)                        Rss(KB)
                        ------                         ------
           Java Heap:    26040                          37040
           TOTAL PSS:   277623            TOTAL RSS:   398420       TOTAL SWAP PSS:      167
        """.trimIndent()

        assertEquals(277623L * 1024L, MetricsRepository.parsePackageTotalPss(dump))
    }

    @Test
    fun buildIssueUrl_includesTitleAndBody() {
        val url = GitHubIssueLauncher.buildIssueUrl(
            title = "tiles crash",
            body = "## Feedback\n\ntiles crash\n\n```text\nlog line\n```\n",
        )
        assertTrue(url.startsWith("https://github.com/god-s-perfect-idiot/metro-os/issues/new?"))
        assertTrue(url.contains("title=tiles%20crash"))
        assertTrue(url.contains("body="))
        assertTrue(url.contains("log%20line") || url.contains("log+line") || url.contains("Feedback"))
    }

    @Test
    fun appMetric_usageBytes_sumsRamAndStorage() {
        val metric = AppMetric(
            packageName = "com.metro.launcher",
            label = "Launcher",
            ramBytes = 10_000L,
            cacheBytes = 2_000L,
            dataBytes = 3_000L,
            codeBytes = 4_000L,
        )
        assertEquals(19_000L, metric.usageBytes)
    }
}
