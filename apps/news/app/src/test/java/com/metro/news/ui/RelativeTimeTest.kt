package com.metro.news.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {
    @Test
    fun format_hoursAgo() {
        val now = 1_000_000L
        assertEquals("3 HOURS AGO", RelativeTime.format(now - 3 * 3_600_000L, now))
    }

    @Test
    fun format_justNow() {
        val now = 1_000_000L
        assertEquals("JUST NOW", RelativeTime.format(now - 10_000L, now))
    }
}
