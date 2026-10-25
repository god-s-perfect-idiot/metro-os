package com.metro.people.data

import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.StatusBarNotification
import android.util.Log
import com.metro.people.PeopleListenerService
import com.metro.system.MetroPreferences

/**
 * Loads what's-new rows from People's own notification listener, gated by
 * Settings → connected apps → Conversation apps.
 */
class PeopleConversationRepository(
    private val context: Context,
) {
    private val prefs = MetroPreferences(context)
    private val labelCache = mutableMapOf<String, String>()

    fun hasNotificationAccess(): Boolean =
        PeopleListenerService.isNotificationAccessEnabled(context)

    fun loadWhatsNew(): List<WhatsNewItem> {
        if (!hasNotificationAccess()) return emptyList()
        val connected = prefs.conversationAppPackages
        return PeopleListenerService.activeNotificationsOrEmpty()
            .mapNotNull { sbn -> toWhatsNewItem(sbn, connected) }
            .sortedByDescending { it.postTimeMs }
    }

    fun open(key: String): Boolean {
        val sbn = PeopleListenerService.findActive(key) ?: return false
        val content = sbn.notification.contentIntent ?: return launchPackage(sbn.packageName)
        return runCatching {
            sendContentIntent(content)
            true
        }.onFailure { error ->
            Log.w(TAG, "contentIntent failed for $key", error)
        }.getOrDefault(launchPackage(sbn.packageName))
    }

    private fun toWhatsNewItem(
        sbn: StatusBarNotification,
        connected: Set<String>,
    ): WhatsNewItem? {
        if (sbn.packageName !in connected) return null
        val notification = sbn.notification ?: return null
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null
        val extras = notification.extras
        val title = extras.charSeq(Notification.EXTRA_TITLE)
            ?: extras.charSeq(Notification.EXTRA_CONVERSATION_TITLE)
            ?: extras.charSeq(Notification.EXTRA_TITLE_BIG)
            ?: return null
        val preview = extras.charSeq(Notification.EXTRA_TEXT)
            ?: extras.charSeq(Notification.EXTRA_BIG_TEXT)
            ?: extras.charSeq(Notification.EXTRA_SUB_TEXT)
            ?: ""
        if (title.isBlank() && preview.isBlank()) return null
        val hasReply = notification.actions?.any { action ->
            action.remoteInputs?.any { it.allowFreeFormInput } == true
        } == true
        val isMail = sbn.packageName in MAIL_PACKAGES
        if (!hasReply && !isMail) return null
        val unread = extras.getInt("android.conversationUnreadMessageCount", 0)
        val messageCount = extras.getParcelableArray(Notification.EXTRA_MESSAGES)?.size ?: 0
        val badge = when {
            unread > 0 -> unread
            messageCount > 0 -> messageCount
            else -> 0
        }
        val appLabel = appLabel(sbn.packageName)
        return WhatsNewItem(
            key = sbn.key,
            authorName = title,
            body = preview,
            sourceLabel = appLabel,
            postTimeMs = sbn.postTime,
            badgeCount = badge,
            packageName = sbn.packageName,
        )
    }

    private fun sendContentIntent(intent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val options = ActivityOptions.makeBasic().apply {
                pendingIntentBackgroundActivityStartMode =
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            }
            intent.send(context, 0, null, null, null, null, options.toBundle())
        } else {
            intent.send()
        }
    }

    private fun launchPackage(packageName: String): Boolean {
        val launch = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        } ?: return false
        return runCatching {
            context.startActivity(launch)
            true
        }.getOrDefault(false)
    }

    private fun appLabel(packageName: String): String {
        labelCache[packageName]?.let { return it }
        val pm = context.packageManager
        val label = try {
            val info = if (Build.VERSION.SDK_INT >= 33) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(info)?.toString()
        } catch (_: Exception) {
            null
        } ?: packageName.substringAfterLast('.')
        labelCache[packageName] = label
        return label
    }

    private fun android.os.Bundle.charSeq(key: String): String? =
        getCharSequence(key)?.toString()?.trim()?.takeIf { it.isNotEmpty() }

    companion object {
        private const val TAG = "PeopleConvRepo"
        private val MAIL_PACKAGES = setOf(
            "com.google.android.gm",
            "com.google.android.gm.lite",
            "com.google.android.apps.gmail",
            "com.metro.mail",
        )
    }
}
