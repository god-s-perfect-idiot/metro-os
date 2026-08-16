package com.metro.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.os.Bundle
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Watches posted notifications and asks the overlay to raise a WP toast for peek-class posts.
 */
class ActionNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        syncHeadsUpSuppression()
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
        super.onListenerDisconnected()
    }

    private fun syncHeadsUpSuppression() {
        if (NotificationsPreferences(this).enabled) {
            HeadsUpController.disableStockHeadsUp(this)
            runCatching {
                requestListenerHints(HINT_HOST_DISABLE_NOTIFICATION_EFFECTS)
            }
        } else {
            runCatching { requestListenerHints(0) }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        considerToast(sbn, rankingMap)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn?.key?.let { NotificationsOverlayService.onNotificationRemoved(it) }
    }

    private fun considerToast(sbn: StatusBarNotification, rankingMap: RankingMap) {
        val ranking = Ranking()
        val ranked = rankingMap.getRanking(sbn.key, ranking)
        val importance = if (ranked) ranking.importance else NotificationManager.IMPORTANCE_DEFAULT
        val matches = if (ranked) ranking.matchesInterruptionFilter() else true
        val interactive = getSystemService(PowerManager::class.java)?.isInteractive != false
        val groupSummary = sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
        val onlyAlertOnce = sbn.notification.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0
        NotificationsOverlayService.considerToast(
            packageName = sbn.packageName,
            key = sbn.key,
            flags = sbn.notification.flags,
            importance = importance,
            matchesInterruptionFilter = matches,
            screenInteractive = interactive,
            isGroupSummary = groupSummary,
            isActiveCall = isActiveCall(sbn),
            onlyAlertOnce = onlyAlertOnce,
            title = extraTitle(sbn),
            body = extraBody(sbn),
        )
    }

    companion object {
        private const val DIALER_PACKAGE = "com.metro.dialer"
        private const val ACTIVE_CALL_TAG = "active_call"

        @Volatile
        private var instance: ActionNotificationListenerService? = null

        /** Ask SystemUI to suppress notification effects while Metro toasts are enabled. */
        fun requestHeadsUpSuppression() {
            val service = instance ?: return
            HeadsUpController.disableStockHeadsUp(service)
            runCatching {
                service.requestListenerHints(HINT_HOST_DISABLE_NOTIFICATION_EFFECTS)
            }
        }

        fun clearHeadsUpSuppression() {
            val service = instance ?: return
            runCatching { service.requestListenerHints(0) }
        }

        fun openNotification(itemKey: String, cancelAfterOpen: Boolean = true) {
            val service = instance ?: return
            val active = runCatching { service.activeNotifications }.getOrNull() ?: return
            val match = active.firstOrNull { sbn ->
                "${sbn.packageName}:${sbn.id}:${sbn.tag.orEmpty()}" == itemKey ||
                    sbn.key == itemKey
            } ?: return
            runCatching {
                match.notification.contentIntent?.send()
                if (cancelAfterOpen) {
                    service.cancelNotification(match.key)
                }
            }
        }

        fun isEnabled(context: android.content.Context): Boolean {
            val expected = ComponentName(context, ActionNotificationListenerService::class.java)
            val flat = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ) ?: return false
            return flat.split(':').any {
                ComponentName.unflattenFromString(it)?.flattenToString() == expected.flattenToString()
            }
        }

        private fun isActiveCall(sbn: StatusBarNotification): Boolean =
            sbn.packageName == DIALER_PACKAGE && sbn.tag == ACTIVE_CALL_TAG

        private fun extraTitle(sbn: StatusBarNotification): String {
            val extras = sbn.notification.extras
            lastMessagingMessage(extras)?.first?.let { return it }
            return extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
                .ifEmpty { extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()?.trim().orEmpty() }
        }

        private fun extraBody(sbn: StatusBarNotification): String? {
            val extras = sbn.notification.extras
            lastMessagingMessage(extras)?.second?.let { return it }
            return extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
                ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()
        }

        /**
         * MessagingStyle (SMS, WhatsApp, etc.): last message sender + text.
         * Bundle keys match [Notification.MessagingStyle.Message] ("sender" / "text").
         */
        private fun lastMessagingMessage(extras: Bundle): Pair<String?, String?>? {
            @Suppress("DEPRECATION")
            val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES) ?: return null
            val last = messages.lastOrNull() as? Bundle ?: return null
            val sender = last.getCharSequence("sender")?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            val text = last.getCharSequence("text")?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            if (sender == null && text == null) return null
            return sender to text
        }
    }
}
