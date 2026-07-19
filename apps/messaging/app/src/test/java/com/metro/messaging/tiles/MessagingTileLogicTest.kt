package com.metro.messaging.tiles

import com.metro.messaging.data.SmsTilePeek
import com.metro.messaging.data.StubMessagingDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MessagingTileLogicTest {
    @Test
    fun peekFromDemoThreads_exportsUnreadAndLabel() {
        val peek = MessagingTileLogic.peekFromDemoThreads()
        assertEquals(1, peek.unreadCount)
        assertEquals("Alex Morgan", peek.latestUnreadLabel)
    }

    @Test
    fun buildTileData_exportsCounterAndPeekWhenUnread() {
        val tile = MessagingTileLogic.buildTileData(
            peek = MessagingTileLogic.peekFromDemoThreads(),
            packageName = "com.metro.messaging",
            accentHex = "#F0A30A",
        )
        assertEquals("Messaging", tile.title)
        assertEquals("#F0A30A", tile.backgroundColorHex)
        assertEquals(1, tile.counter)
        assertEquals("Alex Morgan", tile.backFaceTitle)
    }

    @Test
    fun buildTileData_omitsCounterWhenAllRead() {
        val tile = MessagingTileLogic.buildTileData(
            peek = SmsTilePeek(unreadCount = 0, latestUnreadLabel = null),
            packageName = "com.metro.messaging",
            accentHex = "#F0A30A",
        )
        assertNull(tile.counter)
        assertNull(tile.backFaceTitle)
    }

    @Test
    fun peekFromDemoThreads_matchesThreadUnreadSum() {
        val threads = StubMessagingDataSource.demoThreads()
        val unread = threads.sumOf { it.unreadCount.coerceAtLeast(0) }
        assertEquals(unread, MessagingTileLogic.peekFromDemoThreads().unreadCount)
        // Sanity: demo still has at least one unread ConversationThread
        assertEquals(1, threads.count { it.unreadCount > 0 })
    }
}
