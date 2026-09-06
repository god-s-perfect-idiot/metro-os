package com.metro.launcher.data

import android.app.Notification
import android.content.Context
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
        "com.metro.notifications",
        "com.metro.volume",
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
    fun replaceAll(context: Context, active: Array<StatusBarNotification>?) {
        val next = aggregate(context, active)
        val changed = linkedSetOf<String>()
        changed += byPackage.keys
        changed += next.keys
        byPackage.clear()
        byPackage.putAll(next)
        changed.forEach { notifyListeners(it) }
    }

    /**
     * Merge provider tile fields with notification peek/badge/progress.
     * Rich faces (agenda / photo grid) keep the front face; notifications still supply the badge
     * when the provider has no counter, and supply a flip face when the provider has none.
     * Progress-bar notifications overlay a bar on the front face; the same notification still
     * supplies the flip/peek copy so tiles like Bolt.Earth keep turning.
     */
    fun mergeIntoDisplay(
        packageName: String,
        providerCounter: Int?,
        providerBackFaceTitle: String?,
        hasRichFrontFace: Boolean,
        info: TileNotificationInfo? = snapshot(packageName),
    ): MergedNotificationFace {
        val progress = info?.progress
        val counter = when {
            providerCounter != null && providerCounter > 0 -> providerCounter
            // Ongoing progress (charging / download) is not an unread count.
            progress != null && (info?.count ?: 0) <= 0 -> null
            info != null && info.count > 0 -> info.count
            else -> null
        }
        val backFaces = when {
            !providerBackFaceTitle.isNullOrBlank() ->
                listOf(TilePeekLines(providerBackFaceTitle, null, null))
            hasRichFrontFace -> emptyList()
            else -> info?.peekQueue.orEmpty()
        }
        val lead = backFaces.firstOrNull()
        return MergedNotificationFace(
            counter = counter,
            backFaceTitle = lead?.title,
            backFaceSubtitle = lead?.subtitle,
            backFaceBody = lead?.body,
            backFaces = backFaces,
            hasFlipFace = backFaces.isNotEmpty(),
            progress = progress,
        )
    }

    internal fun aggregate(
        context: Context,
        active: Array<StatusBarNotification>?,
    ): Map<String, TileNotificationInfo> {
        if (active.isNullOrEmpty()) return emptyMap()
        val grouped = active
            .filter { isEligible(it) }
            .groupBy { it.packageName }
        return grouped.mapValues { (packageName, items) ->
            val nowMs = System.currentTimeMillis()
            val annotated = items.map { item ->
                val custom = snapshotCustomNotification(context, item.notification)
                Triple(item, custom, extractProgress(item, custom, nowMs))
            }
            val progress = annotated
                .mapNotNull { (item, _, progress) -> progress?.let { item.postTime to it } }
                .maxByOrNull { it.first }
                ?.second
            // Progress notifications still peek — charging remaining belongs on the flip face.
            // Collect every eligible notification so the live tile can cycle peeks 1-by-1.
            val peeks = buildNotificationPeekQueue(
                annotated.map { (item, custom, _) ->
                    item.postTime to extractPeek(packageName, item.notification, custom)
                },
            )
            val lead = peeks.firstOrNull()
            val badge = annotated.sumOf { (item, _, progressInfo) ->
                if (progressInfo != null) 0
                else {
                    val n = item.notification.number
                    if (n > 0) n else 1
                }
            }
            TileNotificationInfo(
                packageName = packageName,
                count = badge,
                peekTitle = lead?.title,
                peekSubtitle = lead?.subtitle,
                peekBody = lead?.body,
                peeks = peeks,
                updatedAtMs = items.maxOf { it.postTime },
                progress = progress,
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

    private fun extractProgress(
        sbn: StatusBarNotification,
        custom: CustomNotificationSnapshot?,
        nowMs: Long,
    ): TileProgressInfo? {
        val notification = sbn.notification
        val extras = notification.extras
        val flags = notification.flags
        @Suppress("DEPRECATION")
        val foregroundService = flags and Notification.FLAG_FOREGROUND_SERVICE != 0
        val ongoing = flags and Notification.FLAG_ONGOING_EVENT != 0 || foregroundService
        val extrasMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val extrasIndeterminate = extras.getBoolean(
            Notification.EXTRA_PROGRESS_INDETERMINATE,
            false,
        )
        val progressMax = when {
            extrasMax > 0 -> extrasMax
            custom?.hasProgressBar == true -> custom.progressMax.coerceAtLeast(1)
            else -> 0
        }
        val progress = when {
            extrasMax > 0 || extrasIndeterminate -> extras.getInt(Notification.EXTRA_PROGRESS, 0)
            custom?.hasProgressBar == true -> custom.progress
            else -> 0
        }
        val indeterminate = when {
            extrasMax > 0 || extrasIndeterminate -> extrasIndeterminate
            else -> custom?.indeterminate == true
        }
        val extrasTitle = extras.charSequence(Notification.EXTRA_TITLE)
        val extrasText = extras.charSequence(Notification.EXTRA_TEXT)
        val customPeek = custom?.texts?.let { peekFromCustomTexts(it) }
        return resolveTileProgress(
            NotificationProgressFields(
                title = extrasTitle ?: customPeek?.title,
                text = extrasText ?: customPeek?.body,
                bigText = extras.charSequence(Notification.EXTRA_BIG_TEXT),
                subText = extras.charSequence(Notification.EXTRA_SUB_TEXT),
                infoText = extras.charSequence(Notification.EXTRA_INFO_TEXT),
                progress = progress,
                progressMax = progressMax,
                indeterminate = indeterminate,
                hasMediaSession = extras.containsKey(Notification.EXTRA_MEDIA_SESSION),
                showChronometer = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false),
                chronometerCountDown = extras.getBoolean(
                    Notification.EXTRA_CHRONOMETER_COUNT_DOWN,
                    false,
                ),
                whenMs = notification.`when`,
                ongoing = ongoing,
                extraTexts = custom?.texts.orEmpty(),
            ),
            nowMs = nowMs,
        )
    }

    private fun extractPeek(
        packageName: String,
        notification: Notification,
        custom: CustomNotificationSnapshot?,
    ): TilePeekLines {
        val extras = notification.extras
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
            val (from, content) = mailPeekFaceLines(mail)
            return TilePeekLines(
                title = from,
                subtitle = null,
                body = content,
            )
        }
        if (!title.isNullOrBlank() || !text.isNullOrBlank() || !bigText.isNullOrBlank()) {
            val body = text ?: bigText ?: extras.charSequence(Notification.EXTRA_SUB_TEXT)
            return TilePeekLines(title = title, subtitle = null, body = body)
        }
        return peekFromCustomTexts(custom?.texts.orEmpty())
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
}

data class MergedNotificationFace(
    val counter: Int?,
    val backFaceTitle: String?,
    val backFaceBody: String?,
    val hasFlipFace: Boolean,
    val backFaceSubtitle: String? = null,
    /** Newest-first queue of flip faces; Start cycles through these one at a time. */
    val backFaces: List<TilePeekLines> = emptyList(),
    val progress: TileProgressInfo? = null,
)
