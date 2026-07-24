package com.metro.launcher.data

/**
 * WP8.1 Mail-style live-tile peek: sender (user name), subject (title), body (content).
 */
internal data class MailTilePeek(
    val sender: String?,
    val subject: String?,
    val content: String?,
) {
    val hasContent: Boolean
        get() = !sender.isNullOrBlank() || !subject.isNullOrBlank() || !content.isNullOrBlank()
}

/** Packages whose notifications should render as a three-line mail peek face. */
internal object MailTilePackages {
    private val packages = setOf(
        "com.google.android.gm",
        "com.google.android.gm.lite",
        "com.google.android.apps.gmail",
        "com.metro.mail",
    )

    fun contains(packageName: String): Boolean = packageName in packages
}

/**
 * Resolve sender / subject / content from common Gmail (and mail-app) notification shapes.
 *
 * Priority:
 * 1. MessagingStyle last message (sender + text) + conversation title as subject
 * 2. Title + text + distinct bigText (sender / subject / body)
 * 3. Title + multi-line text (sender / subject\\ncontent)
 * 4. Title + text (+ optional bigText as content)
 */
internal fun resolveMailTilePeek(
    title: String?,
    text: String?,
    bigText: String?,
    conversationTitle: String?,
    messageSender: String?,
    messageText: String?,
): MailTilePeek {
    val cleanTitle = title?.trim()?.takeIf { it.isNotEmpty() }
    val cleanText = text?.trim()?.takeIf { it.isNotEmpty() }
    val cleanBig = bigText?.trim()?.takeIf { it.isNotEmpty() }
    val cleanConv = conversationTitle?.trim()?.takeIf { it.isNotEmpty() }
    val cleanMsgSender = messageSender?.trim()?.takeIf { it.isNotEmpty() }
    val cleanMsgText = messageText?.trim()?.takeIf { it.isNotEmpty() }

    // MessagingStyle: conversation title = subject, last message = sender + body.
    if (cleanMsgSender != null || cleanMsgText != null) {
        val sender = cleanMsgSender ?: cleanTitle
        val subject = cleanConv?.takeIf { it != sender } ?: cleanTitle?.takeIf { it != sender }
        val content = cleanMsgText ?: cleanBig ?: cleanText?.takeIf { it != subject }
        return MailTilePeek(sender = sender, subject = subject, content = content)
    }

    // Classic BigTextStyle: title=sender, text=subject, bigText=body.
    if (cleanBig != null && cleanText != null && cleanBig != cleanText) {
        return MailTilePeek(sender = cleanTitle, subject = cleanText, content = cleanBig)
    }

    // Subject + snippet packed into EXTRA_TEXT with a newline.
    if (cleanText != null && '\n' in cleanText) {
        val parts = cleanText.split('\n', limit = 2)
        val subject = parts[0].trim().takeIf { it.isNotEmpty() }
        val content = parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() } ?: cleanBig
        return MailTilePeek(sender = cleanTitle, subject = subject, content = content)
    }

    // Conversation title as subject when present and distinct from the sender line.
    if (cleanConv != null && cleanConv != cleanTitle) {
        val content = cleanText ?: cleanBig
        return MailTilePeek(sender = cleanTitle, subject = cleanConv, content = content)
    }

    // Fallback: title=sender, text=subject, bigText=content (may be null).
    return MailTilePeek(sender = cleanTitle, subject = cleanText, content = cleanBig)
}
