package com.metro.launcher.data

/**
 * WP8.1 Mail-style live-tile peek fields parsed from a notification.
 *
 * The Start face shows [sender] (From) + [content] (email body). [subject] is kept for
 * parsing/fallback when the notification has no distinct body preview.
 */
internal data class MailTilePeek(
    val sender: String?,
    val subject: String?,
    val content: String?,
) {
    val hasContent: Boolean
        get() = !sender.isNullOrBlank() || !subject.isNullOrBlank() || !content.isNullOrBlank()
}

/** Packages whose notifications should render as a mail From + content peek face. */
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

    // Classic BigTextStyle: title=sender, text=subject, bigText=body (sometimes "subject\\nbody").
    if (cleanBig != null && cleanText != null && cleanBig != cleanText) {
        val content = if (cleanBig.startsWith(cleanText)) {
            cleanBig.removePrefix(cleanText)
                .trimStart('\n', '\r', ' ')
                .takeIf { it.isNotEmpty() }
                ?: cleanBig
        } else {
            cleanBig
        }
        return MailTilePeek(sender = cleanTitle, subject = cleanText, content = content)
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

/**
 * Map a parsed [MailTilePeek] onto the two-line Start face: From + email content.
 * Subject is never the second title; it is only used when no body preview exists.
 */
internal fun mailPeekFaceLines(mail: MailTilePeek): Pair<String?, String?> {
    val content = mail.content?.takeIf { it.isNotBlank() }
    val subject = mail.subject?.takeIf { it.isNotBlank() }
    return mail.sender to (content ?: subject)
}
