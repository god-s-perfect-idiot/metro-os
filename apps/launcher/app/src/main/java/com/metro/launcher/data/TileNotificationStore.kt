package com.metro.launcher.data

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-memory notification snapshots keyed by package, fed by [com.metro.launcher.TileNotificationListenerService].
 */
object TileNotificationStore {
    /** Shell / overlay packages whose FGS notifications must never drive Start tiles. */
    val IgnoredPackages: Set<String> = setOf(
        "com.metro.launcher",
        "com.metro.statusbar",
        "com.metro.navbar",
    )

    private val byPackage = ConcurrentHashMap<String, TileNotificationInfo>()
    private val listeners = CopyOnWriteArrayList<(String) -> Unit>()

    fun snapshot(packageName: String): TileNotificationInfo? = byPackage[packageName]

    fun all(): Map<String, TileNotificationInfo> = byPackage.toMap()

    fun addListener(listener: (packageName: String) -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: (packageName: String) -> Unit) {
        listeners -= listener
    }

    fun clear() {
        val packages = byPackage.keys.toList()
        byPackage.clear()
        packages.forEach { notifyListeners(it) }
    }

    /** Rebuild snapshots from the full active notification set. */
    fun replaceAll(active: Array<StatusBarNotification>?) {
        val next = aggregate(active)
        val changed = linkedSetOf<String>()
        changed += byPackage.keys
        changed += next.keys
        byPackage.clear()
        byPackage.putAll(next)
        changed.forEach { notifyListeners(it) }
    }

    /**
     * Merge provider tile fields with notification peek/badge.
     * Rich faces (agenda / photo grid) keep the front face; notifications still supply the badge
     * when the provider has no counter, and supply a flip face when the provider has none.
     */
    fun mergeIntoDisplay(
        packageName: String,
        providerCounter: Int?,
        providerBackFaceTitle: String?,
        hasRichFrontFace: Boolean,
        info: TileNotificationInfo? = snapshot(packageName),
    ): MergedNotificationFace {
        val counter = when {
            providerCounter != null && providerCounter > 0 -> providerCounter
            info != null && info.count > 0 -> info.count
            else -> null
        }
        val backFaceTitle = when {
            !providerBackFaceTitle.isNullOrBlank() -> providerBackFaceTitle
            hasRichFrontFace -> null
            info?.hasPeek == true -> info.peekTitle?.takeIf { it.isNotBlank() }
                ?: info.peekSubtitle
                ?: info.peekBody
            else -> null
        }
        val backFaceSubtitle = when {
            !providerBackFaceTitle.isNullOrBlank() -> null
            hasRichFrontFace -> null
            info?.hasPeek == true && !info.peekTitle.isNullOrBlank() ->
                info.peekSubtitle?.takeIf { it.isNotBlank() }
            else -> null
        }
        val backFaceBody = when {
            !providerBackFaceTitle.isNullOrBlank() -> null
            hasRichFrontFace -> null
            info?.hasPeek != true -> null
            !info.peekTitle.isNullOrBlank() -> info.peekBody
            !info.peekSubtitle.isNullOrBlank() -> info.peekBody
            else -> null
        }
        return MergedNotificationFace(
            counter = counter,
            backFaceTitle = backFaceTitle,
            backFaceSubtitle = backFaceSubtitle,
            backFaceBody = backFaceBody,
            hasFlipFace = !backFaceTitle.isNullOrBlank() ||
                !backFaceSubtitle.isNullOrBlank() ||
                !backFaceBody.isNullOrBlank(),
        )
    }

    internal fun aggregate(active: Array<StatusBarNotification>?): Map<String, TileNotificationInfo> {
        if (active.isNullOrEmpty()) return emptyMap()
        val grouped = active
            .filter { isEligible(it) }
            .groupBy { it.packageName }
        return grouped.mapValues { (packageName, items) ->
            val newest = items.maxByOrNull { it.postTime }!!
            val peek = extractPeek(packageName, newest.notification.extras)
            val badge = items.sumOf { item ->
                val n = item.notification.number
                if (n > 0) n else 1
            }
            TileNotificationInfo(
                packageName = packageName,
                count = badge.coerceAtLeast(1),
                peekTitle = peek.title,
                peekSubtitle = peek.subtitle,
                peekBody = peek.body,
                updatedAtMs = newest.postTime,
            )
        }
    }

    internal fun isEligible(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName in IgnoredPackages) return false
        val flags = sbn.notification.flags
        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
        return true
    }

    private fun notifyListeners(packageName: String) {
        listeners.forEach { it(packageName) }
    }

    private fun extractPeek(packageName: String, extras: Bundle): PeekLines {
        val title = extras.charSequence(Notification.EXTRA_TITLE)
        val text = extras.charSequence(Notification.EXTRA_TEXT)
        val bigText = extras.charSequence(Notification.EXTRA_BIG_TEXT)
        if (MailTilePackages.contains(packageName)) {
            val conversationTitle = extras.charSequence(Notification.EXTRA_CONVERSATION_TITLE)
            val (messageSender, messageText) = extras.lastMessagingMessage()
            val mail = resolveMailTilePeek(
                title = title,
                text = text,
                bigText = bigText,
                conversationTitle = conversationTitle,
                messageSender = messageSender,
                messageText = messageText,
            )
            return PeekLines(
                title = mail.sender,
                subtitle = mail.subject,
                body = mail.content,
            )
        }
        val body = text ?: bigText ?: extras.charSequence(Notification.EXTRA_SUB_TEXT)
        return PeekLines(title = title, subtitle = null, body = body)
    }

    private fun Bundle.lastMessagingMessage(): Pair<String?, String?> {
        @Suppress("DEPRECATION")
        val messages = getParcelableArray(Notification.EXTRA_MESSAGES) ?: return null to null
        val last = messages.lastOrNull() as? Bundle ?: return null to null
        // MessagingStyle.Message bundle keys ("sender" / "text") — KEY_* constants are not
        // always public across SDK compile targets.
        val sender = last.getCharSequence("sender")
            ?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val text = last.getCharSequence("text")
            ?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        return sender to text
    }

    private fun Bundle.charSequence(key: String): String? =
        getCharSequence(key)?.toString()?.trim()?.takeIf { it.isNotEmpty() }

    private data class PeekLines(
        val title: String?,
        val subtitle: String?,
        val body: String?,
    )
}

data class MergedNotificationFace(
    val counter: Int?,
    val backFaceTitle: String?,
    val backFaceBody: String?,
    val hasFlipFace: Boolean,
    val backFaceSubtitle: String? = null,
)
