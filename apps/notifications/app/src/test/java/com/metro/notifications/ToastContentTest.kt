package com.metro.notifications

import android.app.Notification
import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ToastContentTest {
    @Test
    fun isCountSummary_detectsCommonPlaceholders() {
        assertTrue(ToastContent.isCountSummary("WhatsApp", "2 new messages"))
        assertTrue(ToastContent.isCountSummary("3 new messages", null))
        assertTrue(ToastContent.isCountSummary("", "5 messages"))
        assertFalse(ToastContent.isCountSummary("Alice", "hey there"))
        assertFalse(ToastContent.isCountSummary("Mom", "2 pizzas please"))
    }

    @Test
    fun resolve_prefersMessagingStyleOverCountText() {
        val messages = arrayOf(
            message("Alice", "first"),
            message("Alice", "second hello"),
        )
        val notification = notification(
            title = "Alice",
            text = "2 new messages",
            messages = messages,
        )
        val copy = ToastContent.resolve(
            key = "wa|1",
            packageName = "com.whatsapp",
            groupKey = null,
            isGroupSummary = false,
            notification = notification,
            active = null,
        )
        assertEquals("Alice", copy.title)
        assertEquals("second hello", copy.body)
    }

    @Test
    fun resolve_usesNewestGroupChildWhenSummaryIsCountOnly() {
        val childNotification = notification(title = "Alice", text = "running late")
        val summary = notification(
            title = "Alice",
            text = "2 new messages",
            flags = Notification.FLAG_GROUP_SUMMARY,
        )
        val copy = ToastContent.resolve(
            key = "wa|summary",
            packageName = "com.whatsapp",
            groupKey = "g:alice",
            isGroupSummary = true,
            notification = summary,
            active = listOf(
                ToastContent.ActivePost(
                    key = "wa|child",
                    packageName = "com.whatsapp",
                    groupKey = "g:alice",
                    postTime = 100L,
                    isGroupSummary = false,
                    notification = childNotification,
                ),
            ),
        )
        assertEquals("Alice", copy.title)
        assertEquals("running late", copy.body)
    }

    @Test
    fun isSelfReply_whenRemoteInputMatchesLastMessage() {
        val notification = notification(
            title = "Alice",
            text = "on my way",
            messages = arrayOf(message("You", "on my way")),
            remoteInputHistory = arrayOf("on my way"),
        )
        assertTrue(ToastContent.isSelfReply(notification))
    }

    @Test
    fun isSelfReply_falseForIncomingMessage() {
        val notification = notification(
            title = "Alice",
            text = "hello",
            messages = arrayOf(message("Alice", "hello")),
        )
        assertFalse(ToastContent.isSelfReply(notification))
    }

    @Test
    fun isCriticalInterrupt_forCallCategoryAndAlarm() {
        val call = notification(title = "Alice", category = Notification.CATEGORY_CALL)
        assertTrue(ToastContent.isCriticalInterrupt(call))

        val alarm = notification(title = "Alarm", category = Notification.CATEGORY_ALARM)
        assertTrue(ToastContent.isCriticalInterrupt(alarm))

        val normal = notification(title = "Alice", text = "hi")
        assertFalse(ToastContent.isCriticalInterrupt(normal))
    }

    private fun message(sender: String, text: String): Bundle =
        Bundle().apply {
            putCharSequence("sender", sender)
            putCharSequence("text", text)
        }

    private fun notification(
        title: String? = null,
        text: String? = null,
        category: String? = null,
        flags: Int = 0,
        messages: Array<Bundle>? = null,
        remoteInputHistory: Array<String>? = null,
    ): Notification =
        Notification().also { n ->
            val extras = Bundle()
            title?.let { extras.putCharSequence(Notification.EXTRA_TITLE, it) }
            text?.let { extras.putCharSequence(Notification.EXTRA_TEXT, it) }
            messages?.let { extras.putParcelableArray(Notification.EXTRA_MESSAGES, it) }
            remoteInputHistory?.let {
                extras.putCharSequenceArray(Notification.EXTRA_REMOTE_INPUT_HISTORY, it)
            }
            n.extras = extras
            if (category != null) n.category = category
            n.flags = flags
        }
}
