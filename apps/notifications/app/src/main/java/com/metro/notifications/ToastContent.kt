package com.metro.notifications

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import java.util.Locale
import java.util.regex.Pattern

/**
 * Extracts WP toast title/body from Android notification extras, preferring real message
 * copy over group-summary placeholders like "2 new messages".
 */
object ToastContent {
    private val COUNT_SUMMARY: Pattern = Pattern.compile(
        "^\\d+\\s+(new\\s+)?(messages?|chats?|notifications?|emails?|alerts?)\\.?$",
        Pattern.CASE_INSENSITIVE,
    )
    private val COUNT_SUMMARY_PREFIX: Pattern = Pattern.compile(
        "^(new\\s+)?\\d+\\s+(messages?|chats?|notifications?)\\.?$",
        Pattern.CASE_INSENSITIVE,
    )
    private val SELF_SENDERS = setOf("you", "me")

    data class Copy(
        val title: String,
        val body: String?,
        /** Chat / conversation name when this is a group thread; shown above title. */
        val groupTitle: String? = null,
    )

    /** Lightweight sibling used when resolving group summaries to child copy. */
    data class ActivePost(
        val key: String,
        val packageName: String,
        val groupKey: String?,
        val postTime: Long,
        val isGroupSummary: Boolean,
        val notification: Notification,
    )

    fun resolve(sbn: StatusBarNotification, active: Array<StatusBarNotification>?): Copy =
        resolve(
            key = sbn.key,
            packageName = sbn.packageName,
            groupKey = sbn.groupKeyOrNull(),
            isGroupSummary = sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            notification = sbn.notification,
            active = active?.map { it.toActivePost() },
        )

    fun resolve(
        key: String,
        packageName: String,
        groupKey: String?,
        isGroupSummary: Boolean,
        notification: Notification,
        active: List<ActivePost>?,
    ): Copy {
        val direct = copyFrom(notification)
        if (!needsGroupLookup(isGroupSummary, direct)) {
            return direct.withGroupTitleFrom(notification)
        }

        val child = bestGroupChild(key, packageName, groupKey, active)
        if (child != null) {
            val fromChild = copyFrom(child.notification)
            if (!isCountSummary(fromChild.title, fromChild.body)) {
                return fromChild
                    .withGroupTitleFrom(child.notification)
                    .withGroupTitleFrom(notification)
            }
        }

        val fromLines = copyFromTextLines(notification.extras)
        if (fromLines != null && !isCountSummary(fromLines.title, fromLines.body)) {
            return fromLines.withGroupTitleFrom(notification)
        }
        return direct.withGroupTitleFrom(notification)
    }

    /** Shade reply / RemoteInput echo — do not raise another Metro toast. */
    fun isSelfReply(sbn: StatusBarNotification): Boolean = isSelfReply(sbn.notification)

    fun isSelfReply(notification: Notification): Boolean {
        val extras = notification.extras
        val history = remoteInputHistory(extras)
        val last = lastMessagingMessage(extras)
        val lastText = last?.second
        if (history.isNotEmpty() && lastText != null) {
            if (history.any { it.equals(lastText, ignoreCase = true) }) return true
        }
        // Apps often append the reply as MessagingStyle from "You" / "Me".
        val sender = last?.first?.lowercase(Locale.US)
        if (sender != null && sender in SELF_SENDERS) return true
        // Title/body shaped like "You: …" after a RemoteInput update.
        if (history.isNotEmpty()) {
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
            if (title.lowercase(Locale.US) in SELF_SENDERS) return true
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
            if (history.any { it.equals(text, ignoreCase = true) }) return true
        }
        return false
    }

    /**
     * Incoming call / alarm / full-screen peeks that must use stock SystemUI (or FSI),
     * not a Metro toast — heads-up is suppressed globally while Metro owns peeks.
     */
    fun isCriticalInterrupt(sbn: StatusBarNotification): Boolean =
        isCriticalInterrupt(sbn.notification)

    fun isCriticalInterrupt(notification: Notification): Boolean {
        if (notification.fullScreenIntent != null) return true
        val category = notification.category
        if (category == Notification.CATEGORY_CALL || category == Notification.CATEGORY_ALARM) {
            return true
        }
        if (notification.flags and Notification.FLAG_INSISTENT != 0) return true
        val extras = notification.extras
        // Notification.CallStyle (API 31+): android.callType is set on the extras bundle.
        if (extras.containsKey("android.callType") || extras.containsKey(Notification.EXTRA_CALL_PERSON)) {
            return true
        }
        return false
    }

    fun isCountSummary(title: String, body: String?): Boolean {
        val t = title.trim()
        val b = body?.trim().orEmpty()
        if (t.isNotEmpty() && matchesCount(t) && (b.isEmpty() || matchesCount(b))) return true
        if (b.isNotEmpty() && matchesCount(b)) return true
        return false
    }

    private fun matchesCount(value: String): Boolean =
        COUNT_SUMMARY.matcher(value).matches() || COUNT_SUMMARY_PREFIX.matcher(value).matches()

    private fun needsGroupLookup(isGroupSummary: Boolean, copy: Copy): Boolean {
        if (isCountSummary(copy.title, copy.body)) return true
        return isGroupSummary
    }

    private fun copyFrom(notification: Notification): Copy {
        val extras = notification.extras
        lastMessagingMessage(extras)?.let { (sender, text) ->
            // Prefer real MessagingStyle copy over "N new messages" in EXTRA_TEXT.
            if (text != null && !matchesCount(text)) {
                val title = sender
                    ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()
                    ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
                val resolvedTitle = title.ifEmpty { text }
                val resolvedBody = if (title.isEmpty()) null else text
                return Copy(
                    title = resolvedTitle,
                    body = resolvedBody,
                    groupTitle = distinctGroupTitle(extras, resolvedTitle, resolvedBody),
                )
            }
            if (sender != null && text != null) {
                return Copy(
                    title = sender,
                    body = text,
                    groupTitle = distinctGroupTitle(extras, sender, text),
                )
            }
        }
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
            .ifEmpty {
                extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()?.trim().orEmpty()
            }
            .ifEmpty {
                extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim().orEmpty()
            }
        val body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()
        return Copy(
            title = title,
            body = body,
            groupTitle = distinctGroupTitle(extras, title, body),
        )
    }

    /**
     * Conversation / chat name for group threads. Null for 1:1 chats where the conversation
     * title is just the contact (same as [title]).
     */
    fun distinctGroupTitle(extras: Bundle, title: String, body: String?): String? {
        val conversation = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        val t = title.trim()
        val b = body?.trim().orEmpty()
        if (conversation.equals(t, ignoreCase = true)) return null
        if (b.isNotEmpty() && conversation.equals(b, ignoreCase = true)) return null
        val isGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false) ||
            (t.isNotEmpty() && !conversation.equals(t, ignoreCase = true))
        return conversation.takeIf { isGroup }
    }

    /** Prefer an existing group title; otherwise fill from [notification] extras. */
    private fun Copy.withGroupTitleFrom(notification: Notification): Copy {
        if (!groupTitle.isNullOrBlank()) return this
        val fromExtras = distinctGroupTitle(notification.extras, title, body) ?: return this
        return copy(groupTitle = fromExtras)
    }

    private fun copyFromTextLines(extras: Bundle): Copy? {
        @Suppress("DEPRECATION")
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES) ?: return null
        val last = lines.lastOrNull()?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (matchesCount(last)) return null
        val colon = last.indexOf(':')
        return if (colon in 1 until last.lastIndex) {
            Copy(
                title = last.substring(0, colon).trim(),
                body = last.substring(colon + 1).trim().takeIf { it.isNotEmpty() },
            )
        } else {
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
            Copy(title = title.ifEmpty { last }, body = if (title.isEmpty()) null else last)
        }
    }

    private fun bestGroupChild(
        summaryKey: String,
        packageName: String,
        groupKey: String?,
        active: List<ActivePost>?,
    ): ActivePost? {
        if (active.isNullOrEmpty() || groupKey == null) return null
        return active
            .asSequence()
            .filter { it.key != summaryKey }
            .filter { it.packageName == packageName }
            .filter { it.groupKey == groupKey }
            .filter { !it.isGroupSummary }
            .map { it to copyFrom(it.notification) }
            .filter { (_, copy) -> !isCountSummary(copy.title, copy.body) }
            .maxByOrNull { (post, _) -> post.postTime }
            ?.first
    }

    private fun remoteInputHistory(extras: Bundle): List<String> {
        @Suppress("DEPRECATION")
        val raw = extras.getCharSequenceArray(Notification.EXTRA_REMOTE_INPUT_HISTORY) ?: return emptyList()
        return raw.mapNotNull { it?.toString()?.trim()?.takeIf { s -> s.isNotEmpty() } }
    }

    /**
     * MessagingStyle (SMS, WhatsApp, etc.): last message sender + text.
     * Bundle keys match [Notification.MessagingStyle.Message] ("sender" / "text").
     */
    fun lastMessagingMessage(extras: Bundle): Pair<String?, String?>? {
        @Suppress("DEPRECATION")
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES) ?: return null
        // Walk newest-first; skip count placeholders and empty rows.
        for (i in messages.indices.reversed()) {
            val msg = messages[i] as? Bundle ?: continue
            val sender = msg.getCharSequence("sender")?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            val text = msg.getCharSequence("text")?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            if (text != null && matchesCount(text)) continue
            if (sender == null && text == null) continue
            return sender to text
        }
        return null
    }
}

/** Stable group id for debounce; null when the post is not part of an Android group. */
internal fun StatusBarNotification.groupKeyOrNull(): String? {
    val key = groupKey ?: return null
    val hasGroup = !notification.group.isNullOrEmpty() ||
        notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
    return key.takeIf { hasGroup }
}

private fun StatusBarNotification.toActivePost(): ToastContent.ActivePost =
    ToastContent.ActivePost(
        key = key,
        packageName = packageName,
        groupKey = groupKeyOrNull(),
        postTime = postTime,
        isGroupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
        notification = notification,
    )
