package com.metro.conversations.tiles

import com.metro.conversations.data.ConversationMessage
import com.metro.conversations.data.ReplyableConversation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationsTileLogicTest {
    @Test
    fun buildTileData_cyclesActiveConversationsAsPeeks() {
        val data = ConversationsTileLogic.buildTileData(
            conversations = listOf(
                conversation("wa:1", "com.whatsapp", "WhatsApp", "Alice", "hey"),
                conversation("tg:1", "org.telegram.messenger", "Telegram", "Bob", "later"),
            ),
            packageName = "com.metro.conversations",
            accentHex = "#00ABA9",
        )
        assertEquals("Conversations", data.title)
        assertEquals(2, data.counter)
        assertEquals(2, data.peeks?.size)
        assertEquals("Alice", data.peeks!![0].title)
        assertEquals("WhatsApp", data.peeks!![0].subtitle)
        assertEquals("hey", data.peeks!![0].body)
        assertEquals("Bob", data.peeks!![1].title)
        assertNull(data.widgetFace)
    }

    @Test
    fun buildTileData_emptyWhenNoActiveChats() {
        val data = ConversationsTileLogic.buildTileData(
            conversations = emptyList(),
            packageName = "com.metro.conversations",
            accentHex = "#00ABA9",
        )
        assertNull(data.counter)
        assertNull(data.peeks)
        assertTrue(data.title.isNotBlank())
    }

    private fun conversation(
        key: String,
        packageName: String,
        appLabel: String,
        title: String,
        preview: String,
    ) = ReplyableConversation(
        key = key,
        packageName = packageName,
        appLabel = appLabel,
        title = title,
        preview = preview,
        postTimeMs = 1L,
        messages = listOf(
            ConversationMessage("1", title, preview, 1L, fromSelf = false),
        ),
    )
}
