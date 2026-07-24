package com.metro.launcher

import com.metro.launcher.data.MailTilePackages
import com.metro.launcher.data.TileNotificationInfo
import com.metro.launcher.data.TileNotificationStore
import com.metro.launcher.data.resolveMailTilePeek
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
    fun merge_gmailPeek_keepsSenderSubjectContent() {
        val merged = TileNotificationStore.mergeIntoDisplay(
            packageName = "com.google.android.gm",
            providerCounter = null,
            providerBackFaceTitle = null,
            hasRichFrontFace = false,
            info = info(
                packageName = "com.google.android.gm",
                count = 2,
                title = "Ada Lovelace",
                body = "Shall we meet at 3?",
                subtitle = "Project notes",
            ),
        )
        assertEquals("Ada Lovelace", merged.backFaceTitle)
        assertEquals("Project notes", merged.backFaceSubtitle)
        assertEquals("Shall we meet at 3?", merged.backFaceBody)
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

    @Test
    fun resolveMailTilePeek_bigTextStyle_mapsSenderSubjectContent() {
        val peek = resolveMailTilePeek(
            title = "Ada Lovelace",
            text = "Project notes",
            bigText = "Shall we meet at 3?",
            conversationTitle = null,
            messageSender = null,
            messageText = null,
        )
        assertEquals("Ada Lovelace", peek.sender)
        assertEquals("Project notes", peek.subject)
        assertEquals("Shall we meet at 3?", peek.content)
    }

    @Test
    fun resolveMailTilePeek_multilineText_splitsSubjectAndContent() {
        val peek = resolveMailTilePeek(
            title = "Grace Hopper",
            text = "Compiler tips\nDon't forget the docs.",
            bigText = null,
            conversationTitle = null,
            messageSender = null,
            messageText = null,
        )
        assertEquals("Grace Hopper", peek.sender)
        assertEquals("Compiler tips", peek.subject)
        assertEquals("Don't forget the docs.", peek.content)
    }

    @Test
    fun resolveMailTilePeek_messagingStyle_usesConversationTitle() {
        val peek = resolveMailTilePeek(
            title = "Inbox",
            text = null,
            bigText = null,
            conversationTitle = "Weekend plans",
            messageSender = "Alan Turing",
            messageText = "See you Saturday.",
        )
        assertEquals("Alan Turing", peek.sender)
        assertEquals("Weekend plans", peek.subject)
        assertEquals("See you Saturday.", peek.content)
    }

    @Test
    fun mailTilePackages_includesGmail() {
        assertTrue(MailTilePackages.contains("com.google.android.gm"))
        assertFalse(MailTilePackages.contains("com.whatsapp"))
    }

    private fun info(
        packageName: String,
        count: Int,
        title: String?,
        body: String?,
        subtitle: String? = null,
    ) = TileNotificationInfo(
        packageName = packageName,
        count = count,
        peekTitle = title,
        peekSubtitle = subtitle,
        peekBody = body,
        updatedAtMs = 0L,
    )
}
