package com.metro.launcher

import com.metro.launcher.data.TileNotificationInfo
import com.metro.launcher.data.TileNotificationStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TileNotificationMergeTest {
    @Test
    fun merge_usesNotificationCounterWhenProviderHasNone() {
        val merged = TileNotificationStore.mergeIntoDisplay(
            packageName = "com.metro.messaging",
            providerCounter = null,
            providerBackFaceTitle = null,
            hasRichFrontFace = false,
            info = info("com.metro.messaging", 3, "Alice", "Hey"),
        )
        assertEquals(3, merged.counter)
        assertEquals("Alice", merged.backFaceTitle)
        assertEquals("Hey", merged.backFaceBody)
        assertTrue(merged.hasFlipFace)
    }

    @Test
    fun merge_prefersProviderCounter() {
        val merged = TileNotificationStore.mergeIntoDisplay(
            packageName = "com.metro.messaging",
            providerCounter = 2,
            providerBackFaceTitle = null,
            hasRichFrontFace = false,
            info = info("com.metro.messaging", 9, "Alice", "Hey"),
        )
        assertEquals(2, merged.counter)
    }

    @Test
    fun merge_skipsNotificationFlipWhenRichFrontFace() {
        val merged = TileNotificationStore.mergeIntoDisplay(
            packageName = "com.metro.people",
            providerCounter = null,
            providerBackFaceTitle = null,
            hasRichFrontFace = true,
            info = info("com.metro.people", 1, "Bob", "Ping"),
        )
        assertEquals(1, merged.counter)
        assertNull(merged.backFaceTitle)
        assertFalse(merged.hasFlipFace)
    }

    @Test
    fun merge_prefersProviderBackFace() {
        val merged = TileNotificationStore.mergeIntoDisplay(
            packageName = "com.metro.calendar",
            providerCounter = null,
            providerBackFaceTitle = "10:00 — Standup",
            hasRichFrontFace = false,
            info = info("com.metro.calendar", 1, "Notif", "Body"),
        )
        assertEquals("10:00 — Standup", merged.backFaceTitle)
        assertNull(merged.backFaceBody)
        assertTrue(merged.hasFlipFace)
    }

    @Test
    fun changedPackages_detectsDiffs() {
        val previous = mapOf(
            "a" to info("a", 1, "t", "b"),
            "b" to info("b", 2, "t2", null),
        )
        val next = mapOf(
            "a" to info("a", 1, "t", "b"),
            "c" to info("c", 1, "n", null),
        )
        val changed = TileNotificationListenerService.changedPackages(previous, next)
        assertEquals(setOf("b", "c"), changed)
    }

    private fun info(
        packageName: String,
        count: Int,
        title: String?,
        body: String?,
    ) = TileNotificationInfo(
        packageName = packageName,
        count = count,
        peekTitle = title,
        peekBody = body,
        updatedAtMs = 0L,
    )
}
