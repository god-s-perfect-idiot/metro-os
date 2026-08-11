package com.metro.launcher

import com.metro.launcher.data.NotificationProgressFields
import com.metro.launcher.data.TileNotificationInfo
import com.metro.launcher.data.TileNotificationStore
import com.metro.launcher.data.TileProgressInfo
import com.metro.launcher.data.formatRemainingMs
import com.metro.launcher.data.parseRemainingPhrase
import com.metro.launcher.data.resolveTileProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TileProgressTest {
    @Test
    fun parseRemaining_plainMinutes() {
        assertEquals("45 min remaining", parseRemainingPhrase("45 min remaining"))
        assertEquals("45 minutes remaining", parseRemainingPhrase("45 minutes remaining"))
        assertEquals("12 min remaining", parseRemainingPhrase("12 min left"))
    }

    @Test
    fun parseRemaining_hoursAndMinutes() {
        assertEquals("1h 23m remaining", parseRemainingPhrase("1h 23m remaining"))
        assertEquals("1 hr 12 min remaining", parseRemainingPhrase("1 hr 12 min left"))
        assertEquals("1 hour, 5 minutes remaining", parseRemainingPhrase("1 hour, 5 minutes remaining"))
    }

    @Test
    fun parseRemaining_embeddedInChargingCopy() {
        assertEquals(
            "45 min remaining",
            parseRemainingPhrase("Charging · 67% · 45 min remaining"),
        )
        assertEquals(
            "1h 12m remaining",
            parseRemainingPhrase("Session in progress 1h 12m remaining"),
        )
    }

    @Test
    fun parseRemaining_etaAndTimeRemainingPrefix() {
        assertEquals("12 min remaining", parseRemainingPhrase("ETA: 12 min"))
        assertEquals("45 min remaining", parseRemainingPhrase("Time remaining: 45 min"))
        assertEquals("1h 5m remaining", parseRemainingPhrase("Remaining 1h 5m"))
    }

    @Test
    fun parseRemaining_clockForm() {
        assertEquals("1:23 remaining", parseRemainingPhrase("1:23 remaining"))
        assertEquals("01:23:45 remaining", parseRemainingPhrase("01:23:45 left"))
    }

    @Test
    fun parseRemaining_ignoresUnrelatedCopy() {
        assertNull(parseRemainingPhrase("I'll be remaining in the office"))
        assertNull(parseRemainingPhrase("3 new messages"))
        assertNull(parseRemainingPhrase(null))
        assertNull(parseRemainingPhrase(""))
    }

    @Test
    fun formatRemainingMs_usesCompactWpCaptions() {
        assertEquals("45 min remaining", formatRemainingMs(45 * 60 * 1000L))
        assertEquals("1h 23m remaining", formatRemainingMs((1 * 60 + 23) * 60 * 1000L))
        assertEquals("2h remaining", formatRemainingMs(2 * 60 * 60 * 1000L))
        assertEquals("1d 4h remaining", formatRemainingMs((24 + 4) * 60 * 60 * 1000L))
        assertEquals("less than a minute", formatRemainingMs(30_000L))
    }

    @Test
    fun resolve_chargingProgressBar_mapsTitleRemainingAndFraction() {
        val info = resolveTileProgress(
            NotificationProgressFields(
                title = "Charging",
                text = "45 min remaining",
                progress = 67,
                progressMax = 100,
                ongoing = true,
            ),
            nowMs = 1_000L,
        )
        assertNotNull(info)
        assertEquals("Charging", info!!.statusTitle)
        assertEquals("45 min remaining", info.remainingLabel)
        assertEquals(0.67f, info.fraction!!, 0.001f)
        assertEquals("45 min remaining", info.caption(1_000L))
        assertFalse(info.indeterminate)
    }

    @Test
    fun resolve_ongoingRemainingWithoutBar_stillMaps() {
        val info = resolveTileProgress(
            NotificationProgressFields(
                title = "Bolt.Earth",
                text = "1 hr 12 min remaining",
                ongoing = true,
            ),
            nowMs = 1L,
        )
        assertNotNull(info)
        assertEquals("1 hr 12 min remaining", info!!.remainingLabel)
        assertFalse(info.hasBar)
    }

    @Test
    fun resolve_countdownChronometer_ticksFromWhen() {
        val now = 10_000L
        val end = now + 45 * 60 * 1000L
        val info = resolveTileProgress(
            NotificationProgressFields(
                title = "Charging",
                showChronometer = true,
                chronometerCountDown = true,
                whenMs = end,
                ongoing = true,
            ),
            nowMs = now,
        )
        assertNotNull(info)
        assertEquals(end, info!!.countdownEndsAtMs)
        assertEquals("45 min remaining", info.remainingText(now))
        assertEquals("44 min remaining", info.remainingText(now + 60_000L))
        assertEquals("Almost done", info.remainingText(end + 1_000L))
    }

    @Test
    fun resolve_skipsMediaSession() {
        assertNull(
            resolveTileProgress(
                NotificationProgressFields(
                    title = "Now playing",
                    text = "3:21 remaining",
                    progress = 50,
                    progressMax = 100,
                    hasMediaSession = true,
                ),
                nowMs = 1L,
            ),
        )
    }

    @Test
    fun resolve_clearedProgress_isIgnored() {
        assertNull(
            resolveTileProgress(
                NotificationProgressFields(
                    title = "Done",
                    progress = 0,
                    progressMax = 0,
                    indeterminate = false,
                ),
                nowMs = 1L,
            ),
        )
    }

    @Test
    fun resolve_indeterminateDownload() {
        val info = resolveTileProgress(
            NotificationProgressFields(
                title = "Downloading",
                text = "Installing…",
                indeterminate = true,
            ),
            nowMs = 1L,
        )
        assertNotNull(info)
        assertTrue(info!!.indeterminate)
        assertNull(info.fraction)
        assertEquals("Installing…", info.statusBody)
    }

    @Test
    fun merge_progressKeepsPeekFlipAndDropsBadge() {
        val merged = TileNotificationStore.mergeIntoDisplay(
            packageName = "com.revos.bolt.android",
            providerCounter = null,
            providerBackFaceTitle = null,
            hasRichFrontFace = false,
            info = TileNotificationInfo(
                packageName = "com.revos.bolt.android",
                count = 0,
                peekTitle = "Charging",
                peekBody = "45 min remaining",
                updatedAtMs = 1L,
                progress = TileProgressInfo(
                    progress = 67,
                    progressMax = 100,
                    statusTitle = "Charging",
                    remainingLabel = "45 min remaining",
                ),
            ),
        )
        assertEquals(
            TileProgressInfo(
                progress = 67,
                progressMax = 100,
                statusTitle = "Charging",
                remainingLabel = "45 min remaining",
            ),
            merged.progress,
        )
        assertNull(merged.counter)
        assertEquals("Charging", merged.backFaceTitle)
        assertEquals("45 min remaining", merged.backFaceBody)
        assertTrue(merged.hasFlipFace)
    }

    @Test
    fun merge_progressDoesNotOverrideProviderBackFace() {
        val merged = TileNotificationStore.mergeIntoDisplay(
            packageName = "com.metro.calendar",
            providerCounter = null,
            providerBackFaceTitle = "10:00 — Standup",
            hasRichFrontFace = false,
            info = TileNotificationInfo(
                packageName = "com.metro.calendar",
                count = 0,
                peekTitle = "Syncing",
                peekBody = "12 min remaining",
                updatedAtMs = 1L,
                progress = TileProgressInfo(
                    indeterminate = true,
                    statusTitle = "Syncing",
                ),
            ),
        )
        assertEquals("10:00 — Standup", merged.backFaceTitle)
        assertTrue(merged.hasFlipFace)
        assertNotNull(merged.progress)
    }
}
