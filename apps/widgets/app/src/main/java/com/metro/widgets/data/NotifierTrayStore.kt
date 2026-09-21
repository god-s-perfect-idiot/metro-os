package com.metro.widgets.data

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.metro.system.MetroAppRegistry
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Cross-package tray snapshot for the Notifier live tile — cycles every eligible notification
 * one flip at a time (Start multi-peek timing).
 */
object NotifierTrayStore {
    /** Shell / overlay packages whose FGS notifications must never drive the notifier. */
    val IgnoredPackages: Set<String> = setOf(
        "com.metro.launcher",
        "com.metro.statusbar",
        "com.metro.navbar",
        "com.metro.notifications",
        "com.metro.volume",
        "com.metro.lockscreen",
        "com.metro.widgets",
    )

    @Volatile
    private var snapshot: NotifierTraySnapshot = NotifierTraySnapshot()

    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    fun snapshot(): NotifierTraySnapshot = snapshot

    fun addListener(listener: () -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: () -> Unit) {
        listeners -= listener
    }

    fun clear() {
        snapshot = NotifierTraySnapshot()
        notifyListeners()
    }

    fun replaceAll(context: Context, active: Array<StatusBarNotification>?) {
        snapshot = aggregate(context, active)
        notifyListeners()
    }

    internal fun aggregate(
        context: Context,
        active: Array<StatusBarNotification>?,
    ): NotifierTraySnapshot {
        if (active.isNullOrEmpty()) return NotifierTraySnapshot()
        val peeks = active
            .filter { isEligible(it) }
            .mapNotNull { sbn -> extractPeek(context, sbn) }
        val queue = NotifierPeekLogic.buildTrayQueue(peeks)
        val count = active.count { isEligible(it) && !hasProgressOnly(it) }
        return NotifierTraySnapshot(
            peeks = queue,
            count = count.coerceAtLeast(queue.size),
        )
    }

    internal fun isEligible(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName in IgnoredPackages) return false
        val flags = sbn.notification.flags
        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
        return true
    }

    private fun hasProgressOnly(sbn: StatusBarNotification): Boolean {
        val extras = sbn.notification.extras
        val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val indeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)
        if (max <= 0 && !indeterminate) return false
        val title = extras.charSequence(Notification.EXTRA_TITLE)
        val text = extras.charSequence(Notification.EXTRA_TEXT)
        return title.isNullOrBlank() && text.isNullOrBlank()
    }

    private fun extractPeek(
        context: Context,
        sbn: StatusBarNotification,
    ): NotifierPeekLines? {
        val notification = sbn.notification
        val extras = notification.extras
        val title = extras.charSequence(Notification.EXTRA_TITLE)
        val text = extras.charSequence(Notification.EXTRA_TEXT)
        val bigText = extras.charSequence(Notification.EXTRA_BIG_TEXT)
        val (messageSender, messageText) = extras.lastMessagingMessage()
        val resolvedTitle = title ?: messageSender
        val resolvedBody = text ?: bigText ?: messageText
            ?: extras.charSequence(Notification.EXTRA_SUB_TEXT)
        val peek = NotifierPeekLines(
            title = resolvedTitle,
            subtitle = null,
            body = resolvedBody,
            appLabel = resolveAppLabel(context, sbn),
            packageName = sbn.packageName,
            postTimeMs = sbn.postTime,
        ).normalizedForFlip()
        return peek.takeIf { it.hasContent }
    }

    /**
     * Footer label for the peek face — same priority as the shade when possible:
     * substitute app name → suite registry → PackageManager → last package segment.
     */
    internal fun resolveAppLabel(context: Context, sbn: StatusBarNotification): String {
        val extras = sbn.notification.extras
        // Notification.EXTRA_SUBSTITUTE_APP_NAME ("android.substName") — not always on the SDK stub.
        extras.getString("android.substName")
            ?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { return it }

        MetroAppRegistry.label(sbn.packageName)?.let { return it }

        val pm = context.packageManager
        runCatching {
            val info = pm.getApplicationInfo(sbn.packageName, 0)
            val label = info.loadLabel(pm)?.toString()?.trim()
            if (!label.isNullOrEmpty()) return label
        }
        runCatching {
            val info = pm.getApplicationInfo(sbn.packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)
            val label = pm.getApplicationLabel(info)?.toString()?.trim()
            if (!label.isNullOrEmpty()) return label
        }

        return sbn.packageName.substringAfterLast('.')
    }

    private fun notifyListeners() {
        listeners.forEach { it() }
    }

    private fun Bundle.lastMessagingMessage(): Pair<String?, String?> {
        @Suppress("DEPRECATION")
        val messages = getParcelableArray(Notification.EXTRA_MESSAGES) ?: return null to null
        val last = messages.lastOrNull() as? Bundle ?: return null to null
        val sender = last.getCharSequence("sender")
            ?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val text = last.getCharSequence("text")
            ?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        return sender to text
    }

    private fun Bundle.charSequence(key: String): String? =
        getCharSequence(key)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
}
