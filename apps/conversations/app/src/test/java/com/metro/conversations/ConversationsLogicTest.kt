package com.metro.conversations

import com.metro.conversations.data.ConversationsLogic
import com.metro.conversations.data.ConversationMessage
import com.metro.conversations.data.FavoriteChat
import com.metro.conversations.data.HomeTile
import com.metro.conversations.data.NotificationCopyInput
import com.metro.conversations.data.ReplyableConversation
import com.metro.conversations.data.ReplyableNotificationSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationsLogicTest {
    @Test
    fun isReplyableCandidate_requiresReplyAction() {
        val snapshot = sampleSnapshot(hasReplyAction = false)
        assertFalse(ConversationsLogic.isReplyableCandidate(snapshot))
    }

    @Test
    fun isReplyableCandidate_skipsGroupSummary() {
        val snapshot = sampleSnapshot(isGroupSummary = true)
        assertFalse(ConversationsLogic.isReplyableCandidate(snapshot))
    }

    @Test
    fun isReplyableCandidate_skipsSuiteMessaging() {
        val snapshot = sampleSnapshot(packageName = "com.metro.messaging")
        assertFalse(ConversationsLogic.isReplyableCandidate(snapshot))
    }

    @Test
    fun isReplyableCandidate_skipsAndroidMessages() {
        val snapshot = sampleSnapshot(packageName = "com.google.android.apps.messaging")
        assertFalse(ConversationsLogic.isReplyableCandidate(snapshot))
    }

    @Test
    fun isReplyableCandidate_includesGmailWithoutReply() {
        val snapshot = sampleSnapshot(
            packageName = "com.google.android.gm",
            hasReplyAction = false,
        )
        assertTrue(ConversationsLogic.isReplyableCandidate(snapshot))
    }

    @Test
    fun isReplyableCandidate_rejectsNonGmailWithoutReply() {
        val snapshot = sampleSnapshot(
            packageName = "com.whatsapp",
            hasReplyAction = false,
        )
        assertFalse(ConversationsLogic.isReplyableCandidate(snapshot))
    }

    @Test
    fun isReplyableCandidate_acceptsReplyableChild() {
        val snapshot = sampleSnapshot()
        assertTrue(ConversationsLogic.isReplyableCandidate(snapshot))
    }

    @Test
    fun groupByApp_sortsAppsByNewestThenLabel() {
        val conversations = listOf(
            conversation("wa:1", "com.whatsapp", "WhatsApp", postTimeMs = 100L),
            conversation("tg:1", "org.telegram.messenger", "Telegram", postTimeMs = 300L),
            conversation("wa:2", "com.whatsapp", "WhatsApp", postTimeMs = 200L),
        )
        val groups = ConversationsLogic.groupByApp(conversations)
        assertEquals(listOf("org.telegram.messenger", "com.whatsapp"), groups.map { it.packageName })
        assertEquals(listOf("wa:2", "wa:1"), groups[1].conversations.map { it.key })
    }

    @Test
    fun homeTiles_startsWithAllAppsThenFavoritesThenAppsThenClear() {
        val groups = ConversationsLogic.groupByApp(
            listOf(conversation("wa:1", "com.whatsapp", "WhatsApp", 1L)),
        )
        val tiles = ConversationsLogic.homeTiles(groups)
        assertEquals(HomeTile.AllApps, tiles[0])
        assertEquals(HomeTile.Favorites, tiles[1])
        assertEquals(
            HomeTile.App("com.whatsapp", "WhatsApp", 1),
            tiles[2],
        )
        assertEquals(HomeTile.Clear, tiles.last())
    }

    @Test
    fun homeTiles_omitsClearWhenNoApps() {
        assertEquals(
            listOf(HomeTile.AllApps, HomeTile.Favorites),
            ConversationsLogic.homeTiles(emptyList()),
        )
    }

    @Test
    fun favoriteConversations_filtersByPackageAndSender() {
        val groups = ConversationsLogic.groupByApp(
            listOf(
                conversation("wa:1", "com.whatsapp", "WhatsApp", 100L, title = "Alice"),
                conversation("wa:2", "com.whatsapp", "WhatsApp", 200L, title = "Bob"),
                conversation("tg:1", "org.telegram.messenger", "Telegram", 300L, title = "Alice"),
            ),
        )
        val favorites = setOf(
            FavoriteChat("com.whatsapp", "Alice"),
            FavoriteChat("org.telegram.messenger", "alice"),
        )
        val filtered = ConversationsLogic.favoriteConversations(groups, favorites)
        assertEquals(listOf("tg:1", "wa:1"), filtered.map { it.key })
    }

    @Test
    fun conversationsFor_allAppsFlattensNewestFirst() {
        val groups = ConversationsLogic.groupByApp(
            listOf(
                conversation("a", "pkg.a", "A", 100L),
                conversation("b", "pkg.b", "B", 200L),
            ),
        )
        assertEquals(
            listOf("b", "a"),
            ConversationsLogic.conversationsFor(groups, packageName = null).map { it.key },
        )
    }

    @Test
    fun resolveNotificationCopy_chatPrefersMessagingStyleBody() {
        val copy = ConversationsLogic.resolveNotificationCopy(
            NotificationCopyInput(
                packageName = "com.whatsapp",
                title = "Alice",
                text = "2 new messages",
                messages = listOf(
                    ConversationMessage("1", "Alice", "first", 1L, fromSelf = false),
                    ConversationMessage("2", "Alice", "second hello", 2L, fromSelf = false),
                ),
            ),
        )
        assertEquals("Alice", copy.title)
        assertEquals("second hello", copy.preview)
    }

    @Test
    fun resolveNotificationCopy_chatKeepsPeerTitleAfterSelfReply() {
        val copy = ConversationsLogic.resolveNotificationCopy(
            NotificationCopyInput(
                packageName = "com.whatsapp",
                title = "You",
                text = "sounds good",
                messages = listOf(
                    ConversationMessage("1", "Alice", "free later?", 1L, fromSelf = false),
                    ConversationMessage("2", "You", "sounds good", 2L, fromSelf = true),
                ),
            ),
        )
        assertEquals("Alice", copy.title)
        assertEquals("sounds good", copy.preview)
    }

    @Test
    fun resolveNotificationCopy_chatPrefersConversationTitleOverYou() {
        val copy = ConversationsLogic.resolveNotificationCopy(
            NotificationCopyInput(
                packageName = "org.telegram.messenger",
                title = "You",
                conversationTitle = "Family",
                messages = listOf(
                    ConversationMessage("1", "You", "on my way", 1L, fromSelf = true),
                ),
            ),
        )
        assertEquals("Family", copy.title)
    }

    @Test
    fun isMultiSenderThread_requiresDistinctPeers() {
        assertFalse(
            ConversationsLogic.isMultiSenderThread(
                listOf(
                    ConversationMessage("1", "Alice", "hi", 1L, fromSelf = false),
                    ConversationMessage("2", "You", "hey", 2L, fromSelf = true),
                    ConversationMessage("3", "Alice", "ok", 3L, fromSelf = false),
                ),
            ),
        )
        assertTrue(
            ConversationsLogic.isMultiSenderThread(
                listOf(
                    ConversationMessage("1", "Alice", "hi", 1L, fromSelf = false),
                    ConversationMessage("2", "Bob", "hey", 2L, fromSelf = false),
                    ConversationMessage("3", "You", "ok", 3L, fromSelf = true),
                ),
            ),
        )
    }

    @Test
    fun resolveNotificationCopy_chatPrefersBigTextOverSubjectText() {
        val copy = ConversationsLogic.resolveNotificationCopy(
            NotificationCopyInput(
                packageName = "com.example.chat",
                title = "Mom",
                text = "Dinner",
                bigText = "Can you pick up milk on the way home?",
            ),
        )
        assertEquals("Mom", copy.title)
        assertEquals("Can you pick up milk on the way home?", copy.preview)
    }

    @Test
    fun resolveNotificationCopy_gmailUsesBodyNotJustSubject() {
        val copy = ConversationsLogic.resolveNotificationCopy(
            NotificationCopyInput(
                packageName = "com.google.android.gm",
                title = "Alex Rivera",
                text = "Project update",
                bigText = "Project update\nHere are the notes from today.",
            ),
        )
        assertEquals("Alex Rivera", copy.title)
        assertEquals("Here are the notes from today.", copy.preview)
        assertTrue(copy.messages.isNotEmpty())
        assertTrue(copy.messages.first().text.contains("Here are the notes"))
    }

    @Test
    fun resolveNotificationCopy_gmailMultilineTextSplitsBody() {
        val copy = ConversationsLogic.resolveNotificationCopy(
            NotificationCopyInput(
                packageName = "com.google.android.gm",
                title = "Boss",
                text = "Tomorrow\nPlease send the deck before noon.",
            ),
        )
        assertEquals("Boss", copy.title)
        assertEquals("Please send the deck before noon.", copy.preview)
    }

    @Test
    fun resolveAppLabel_prefersSubstituteThenPmThenHumanized() {
        assertEquals(
            "WhatsApp",
            ConversationsLogic.resolveAppLabel(
                packageName = "com.whatsapp",
                packageManagerLabel = "com.whatsapp",
                substituteAppName = "WhatsApp",
            ),
        )
        assertEquals(
            "Telegram",
            ConversationsLogic.resolveAppLabel(
                packageName = "org.telegram.messenger",
                packageManagerLabel = "Telegram",
                substituteAppName = null,
            ),
        )
        assertEquals(
            "messenger",
            ConversationsLogic.resolveAppLabel(
                packageName = "org.telegram.messenger",
                packageManagerLabel = "org.telegram.messenger",
                substituteAppName = null,
            ),
        )
    }

    @Test
    fun findByKey_returnsMatchingConversation() {
        val groups = ConversationsLogic.groupByApp(
            listOf(conversation("a", "pkg.a", "A", 1L)),
        )
        assertEquals("a", ConversationsLogic.findByKey(groups, "a")?.key)
    }

    private fun sampleSnapshot(
        hasReplyAction: Boolean = true,
        isGroupSummary: Boolean = false,
        packageName: String = "com.example.chat",
    ) = ReplyableNotificationSnapshot(
        key = "key",
        packageName = packageName,
        title = "Alex",
        preview = "hey",
        postTimeMs = 1L,
        messages = emptyList(),
        isGroupSummary = isGroupSummary,
        hasReplyAction = hasReplyAction,
    )

    private fun conversation(
        key: String,
        packageName: String,
        appLabel: String,
        postTimeMs: Long,
        title: String = "title",
    ) = ReplyableConversation(
        key = key,
        packageName = packageName,
        appLabel = appLabel,
        title = title,
        preview = "preview",
        postTimeMs = postTimeMs,
        messages = emptyList(),
    )
}
