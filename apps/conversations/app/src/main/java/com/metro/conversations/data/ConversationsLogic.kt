package com.metro.conversations.data

/**
 * One message bubble shown inside a replyable conversation thread.
 */
data class ConversationMessage(
    val id: String,
    val sender: String?,
    val text: String,
    val timestampMs: Long,
    val fromSelf: Boolean,
    /** Tray-visible image (MessagingStyle data URI or BigPicture), if any. */
    val image: android.graphics.Bitmap? = null,
)

/**
 * A shade notification that Conversations can show — replyable chats, plus Gmail
 * even when the notification has no free-form RemoteInput.
 */
data class ReplyableConversation(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val preview: String,
    val postTimeMs: Long,
    val messages: List<ConversationMessage>,
    val canReply: Boolean = true,
    /** Sender / conversation photo from the shade notification when available. */
    val senderPhoto: android.graphics.Bitmap? = null,
)

data class AppConversationGroup(
    val packageName: String,
    val appLabel: String,
    val conversations: List<ReplyableConversation>,
)

/**
 * Snapshot parsed from a [android.service.notification.StatusBarNotification] before
 * app-label resolution — kept free of Android types for unit tests.
 */
data class ReplyableNotificationSnapshot(
    val key: String,
    val packageName: String,
    val title: String,
    val preview: String,
    val postTimeMs: Long,
    val messages: List<ConversationMessage>,
    val isGroupSummary: Boolean,
    val hasReplyAction: Boolean,
)

/** Home grid tile — [AllApps] + [Favorites], then apps, then optional [Clear]. */
sealed class HomeTile {
    data object AllApps : HomeTile()

    data object Favorites : HomeTile()

    data object Clear : HomeTile()

    data class App(
        val packageName: String,
        val appLabel: String,
        val conversationCount: Int,
    ) : HomeTile()
}

/**
 * Raw notification strings used to resolve list title / preview / thread body.
 * Platform-free so unit tests can cover Gmail / InboxStyle / MessagingStyle shapes.
 */
data class NotificationCopyInput(
    val packageName: String,
    val title: String? = null,
    val text: String? = null,
    val bigText: String? = null,
    val conversationTitle: String? = null,
    val subText: String? = null,
    val infoText: String? = null,
    val textLines: List<String> = emptyList(),
    val messages: List<ConversationMessage> = emptyList(),
)

data class ResolvedNotificationCopy(
    val title: String,
    val preview: String,
    val messages: List<ConversationMessage>,
)

object ConversationsLogic {
    private val SELF_SENDERS = setOf("you", "me")

    /** SMS / suite Messaging — Conversations is for third-party chat + Gmail. */
    val EXCLUDED_MESSAGING_PACKAGES: Set<String> = setOf(
        "com.metro.messaging",
        "com.android.mms",
        "com.android.messaging",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
    )

    /** Gmail / suite Mail — prefer From + body over subject-only previews. */
    val MAIL_PACKAGES: Set<String> = setOf(
        "com.google.android.gm",
        "com.google.android.gm.lite",
        "com.google.android.apps.gmail",
        "com.metro.mail",
    )

    fun isExcludedMessagingPackage(packageName: String): Boolean =
        packageName in EXCLUDED_MESSAGING_PACKAGES

    fun isGmailPackage(packageName: String): Boolean =
        packageName in MAIL_PACKAGES

    fun isMailPackage(packageName: String): Boolean =
        packageName in MAIL_PACKAGES

    fun isReplyableCandidate(snapshot: ReplyableNotificationSnapshot): Boolean {
        if (isExcludedMessagingPackage(snapshot.packageName)) return false
        if (snapshot.isGroupSummary) return false
        if (snapshot.title.isBlank() && snapshot.preview.isBlank() && snapshot.messages.isEmpty()) {
            return false
        }
        if (snapshot.hasReplyAction) return true
        // Mail often peeks without RemoteInput — still list so users can open / read.
        return isMailPackage(snapshot.packageName)
    }

    fun groupByApp(
        conversations: List<ReplyableConversation>,
    ): List<AppConversationGroup> {
        return conversations
            .groupBy { it.packageName }
            .entries
            .map { (packageName, items) ->
                val label = items.firstOrNull()?.appLabel.orEmpty().ifBlank { packageName }
                AppConversationGroup(
                    packageName = packageName,
                    appLabel = label,
                    conversations = items.sortedByDescending { it.postTimeMs },
                )
            }
            .sortedWith(
                compareByDescending<AppConversationGroup> { group ->
                    group.conversations.maxOfOrNull { it.postTimeMs } ?: 0L
                }.thenBy { it.appLabel.lowercase() },
            )
    }

    /** Tile 1 = all chats, tile 2 = favorites, then each app group; clear only when apps exist. */
    fun homeTiles(groups: List<AppConversationGroup>): List<HomeTile> {
        val apps = groups.map { group ->
            HomeTile.App(
                packageName = group.packageName,
                appLabel = group.appLabel,
                conversationCount = group.conversations.size,
            )
        }
        return listOf(HomeTile.AllApps, HomeTile.Favorites) + apps +
            if (apps.isNotEmpty()) listOf(HomeTile.Clear) else emptyList()
    }

    fun conversationsFor(
        groups: List<AppConversationGroup>,
        packageName: String?,
    ): List<ReplyableConversation> {
        return if (packageName == null) {
            groups.asSequence()
                .flatMap { it.conversations.asSequence() }
                .sortedByDescending { it.postTimeMs }
                .toList()
        } else {
            groups.firstOrNull { it.packageName == packageName }?.conversations.orEmpty()
        }
    }

    /** Active shade chats whose peer title matches a saved favorite for that app. */
    fun favoriteConversations(
        groups: List<AppConversationGroup>,
        favorites: Set<FavoriteChat>,
    ): List<ReplyableConversation> {
        if (favorites.isEmpty()) return emptyList()
        return conversationsFor(groups, packageName = null)
            .filter { conversation ->
                favorites.any { it.matchesConversation(conversation) }
            }
    }

    /**
     * Resolve list title + preview (+ thread messages) from notification extras.
     *
     * Prefers MessagingStyle / BigText body / InboxStyle lines over subject-only
     * [NotificationCopyInput.text], matching what the shade actually shows.
     */
    fun resolveNotificationCopy(input: NotificationCopyInput): ResolvedNotificationCopy {
        if (isMailPackage(input.packageName)) {
            return resolveMailCopy(input)
        }
        return resolveChatCopy(input)
    }

    private fun resolveChatCopy(input: NotificationCopyInput): ResolvedNotificationCopy {
        val messages = input.messages
        val last = messages.lastOrNull()
        if (last != null) {
            val title = resolvePeerTitle(
                conversationTitle = input.conversationTitle,
                title = input.title,
                messages = messages,
                fallback = "conversation",
            )
            val bodyPreview = last.text.ifBlank {
                if (last.image != null) "photo" else ""
            }
            val preview = when {
                !last.fromSelf && !last.sender.isNullOrBlank() &&
                    !last.sender.equals(title, ignoreCase = true) &&
                    bodyPreview.isNotEmpty() -> {
                    "${last.sender}: $bodyPreview"
                }
                else -> bodyPreview
            }
            return ResolvedNotificationCopy(title = title, preview = preview, messages = messages)
        }

        val title = resolvePeerTitle(
            conversationTitle = input.conversationTitle,
            title = input.title,
            messages = emptyList(),
            fallback = "conversation",
        )
        val preview = firstDistinctContent(
            candidates = listOf(
                clean(input.bigText),
                lastTextLine(input.textLines),
                clean(input.text),
                clean(input.subText),
                clean(input.infoText),
            ),
            title = title,
        ) ?: ""
        val threadMessages = if (preview.isNotEmpty()) {
            listOf(
                ConversationMessage(
                    id = "preview",
                    sender = null,
                    text = preview,
                    timestampMs = 0L,
                    fromSelf = false,
                ),
            )
        } else {
            emptyList()
        }
        return ResolvedNotificationCopy(
            title = title,
            preview = preview,
            messages = threadMessages,
        )
    }

    private fun resolveMailCopy(input: NotificationCopyInput): ResolvedNotificationCopy {
        val last = input.messages.lastOrNull()
        if (last != null) {
            val sender = resolvePeerTitle(
                conversationTitle = null,
                title = input.title,
                messages = input.messages,
                fallback = "mail",
            )
            val subject = clean(input.conversationTitle)
                ?.takeIf { !it.equals(sender, ignoreCase = true) && !isSelfSender(it) }
                ?: clean(input.title)?.takeIf { !it.equals(sender, ignoreCase = true) && !isSelfSender(it) }
            val body = clean(last.text)
                ?: bodyFromBigText(input.text, input.bigText)
                ?: clean(input.text)?.takeIf { it != subject }
            val preview = body ?: subject.orEmpty()
            return ResolvedNotificationCopy(
                title = sender,
                preview = preview,
                messages = synthesizeMailMessages(
                    existing = input.messages,
                    sender = sender,
                    subject = subject,
                    body = body,
                ),
            )
        }

        val sender = resolvePeerTitle(
            conversationTitle = null,
            title = input.title,
            messages = emptyList(),
            fallback = "mail",
        )
        val text = clean(input.text)
        val big = clean(input.bigText)
        val conv = clean(input.conversationTitle)

        // Classic BigTextStyle: title=sender, text=subject, bigText=body.
        if (big != null && text != null && big != text) {
            val body = bodyFromBigText(text, big) ?: big
            return ResolvedNotificationCopy(
                title = sender,
                preview = body,
                messages = synthesizeMailMessages(
                    existing = emptyList(),
                    sender = sender,
                    subject = text,
                    body = body,
                ),
            )
        }

        // Subject + snippet packed into EXTRA_TEXT with a newline.
        if (text != null && '\n' in text) {
            val parts = text.split('\n', limit = 2)
            val subject = parts[0].trim().takeIf { it.isNotEmpty() }
            val body = parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() } ?: big
            return ResolvedNotificationCopy(
                title = sender,
                preview = body ?: subject.orEmpty(),
                messages = synthesizeMailMessages(
                    existing = emptyList(),
                    sender = sender,
                    subject = subject,
                    body = body,
                ),
            )
        }

        val inboxLine = lastTextLine(input.textLines)
        val subject = conv?.takeIf { !it.equals(sender, ignoreCase = true) } ?: text
        val body = inboxLine
            ?: big?.takeIf { it != text }
            ?: text?.takeIf { conv != null && conv != text }
        val preview = body ?: subject.orEmpty()
        return ResolvedNotificationCopy(
            title = sender,
            preview = preview,
            messages = synthesizeMailMessages(
                existing = emptyList(),
                sender = sender,
                subject = subject?.takeIf { it != body },
                body = body ?: preview.takeIf { it.isNotEmpty() },
            ),
        )
    }

    private fun synthesizeMailMessages(
        existing: List<ConversationMessage>,
        sender: String,
        subject: String?,
        body: String?,
    ): List<ConversationMessage> {
        if (existing.isNotEmpty()) return existing
        val text = when {
            !body.isNullOrBlank() && !subject.isNullOrBlank() && body != subject -> {
                "$subject\n$body"
            }
            !body.isNullOrBlank() -> body
            !subject.isNullOrBlank() -> subject
            else -> return emptyList()
        }
        return listOf(
            ConversationMessage(
                id = "mail",
                sender = sender,
                text = text,
                timestampMs = 0L,
                fromSelf = false,
            ),
        )
    }

    private fun bodyFromBigText(text: String?, bigText: String?): String? {
        val cleanText = clean(text) ?: return clean(bigText)
        val cleanBig = clean(bigText) ?: return null
        if (cleanBig == cleanText) return null
        if (cleanBig.startsWith(cleanText)) {
            return cleanBig.removePrefix(cleanText)
                .trimStart('\n', '\r', ' ')
                .takeIf { it.isNotEmpty() }
                ?: cleanBig
        }
        return cleanBig
    }

    private fun firstDistinctContent(candidates: List<String?>, title: String): String? {
        val normalizedTitle = title.trim()
        return candidates.firstOrNull { value ->
            !value.isNullOrBlank() &&
                !value.equals(normalizedTitle, ignoreCase = true) &&
                !isCountSummary(value)
        }
    }

    private fun lastTextLine(lines: List<String>): String? =
        lines.asReversed().firstOrNull { line ->
            val trimmed = line.trim()
            trimmed.isNotEmpty() && !isCountSummary(trimmed)
        }?.trim()

    fun isCountSummary(value: String): Boolean {
        val t = value.trim()
        if (t.isEmpty()) return false
        val lower = t.lowercase()
        return Regex("""^\d+\s+(new\s+)?(messages?|chats?|notifications?|emails?|alerts?)\.?$""")
            .matches(lower) ||
            Regex("""^(new\s+)?\d+\s+(messages?|chats?|notifications?)\.?$""")
                .matches(lower)
    }

    fun isSelfSender(sender: String?): Boolean {
        val value = sender?.trim()?.lowercase() ?: return false
        return value in SELF_SENDERS
    }

    /** True when the thread has messages from more than one non-self sender. */
    fun isMultiSenderThread(messages: List<ConversationMessage>): Boolean {
        val peers = messages.mapNotNull { message ->
            clean(message.sender)?.takeUnless { isSelfSender(it) }
        }.distinctBy { it.lowercase() }
        return peers.size > 1
    }

    /**
     * List / thread header name for the peer — never "You" / "Me".
     * After a reply, MessagingStyle often sets EXTRA_TITLE (or the last sender) to
     * "You"; keep the contact / conversation name instead.
     */
    internal fun resolvePeerTitle(
        conversationTitle: String?,
        title: String?,
        messages: List<ConversationMessage>,
        fallback: String,
    ): String {
        clean(conversationTitle)?.takeUnless { isSelfSender(it) }?.let { return it }
        clean(title)?.takeUnless { isSelfSender(it) }?.let { return it }
        messages.asReversed().firstNotNullOfOrNull { message ->
            clean(message.sender)?.takeUnless { isSelfSender(it) }
        }?.let { return it }
        return fallback
    }

    /** Conversation bubble time, e.g. `4:55pm`. */
    fun bubbleTime(timestampMs: Long): String {
        if (timestampMs <= 0L) return ""
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestampMs }
        var hour = cal.get(java.util.Calendar.HOUR)
        if (hour == 0) hour = 12
        val minute = cal.get(java.util.Calendar.MINUTE)
        val amPm = if (cal.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM) "am" else "pm"
        return "$hour:${minute.toString().padStart(2, '0')}$amPm"
    }

    /**
     * Prefer a real app label; never leave a raw package name when a substitute or
     * humanized fallback is available.
     */
    fun resolveAppLabel(
        packageName: String,
        packageManagerLabel: String?,
        substituteAppName: String?,
    ): String {
        clean(substituteAppName)?.let { return it }
        val pmLabel = clean(packageManagerLabel)
        if (pmLabel != null && !looksLikePackageName(pmLabel)) return pmLabel
        return humanizePackageName(packageName)
    }

    fun looksLikePackageName(value: String): Boolean {
        val trimmed = value.trim()
        if (!trimmed.contains('.')) return false
        // "com.whatsapp" / "org.telegram.messenger" — not a user-facing title.
        return trimmed.lowercase().matches(Regex("""^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$"""))
    }

    fun humanizePackageName(packageName: String): String {
        val last = packageName.substringAfterLast('.').ifBlank { packageName }
        return last.replace('_', ' ').trim().ifBlank { packageName }
    }

    fun findByKey(
        groups: List<AppConversationGroup>,
        key: String,
    ): ReplyableConversation? =
        groups.asSequence().flatMap { it.conversations.asSequence() }.firstOrNull { it.key == key }

    private fun clean(value: String?): String? =
        value?.trim()?.takeIf { it.isNotEmpty() }
}
