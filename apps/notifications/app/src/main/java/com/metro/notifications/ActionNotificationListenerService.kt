package com.metro.notifications

import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Watches posted notifications and asks the overlay to raise a WP toast for peek-class posts.
 *
 * Critical interrupts (calls / alarms / full-screen intents) temporarily restore stock heads-up
 * and fire [Notification.fullScreenIntent] when present so WhatsApp calls and similar peeks
 * are not swallowed by Metro's global heads-up suppress.
 */
class ActionNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        syncHeadsUpSuppression()
        // Android re-delivers every active notification on connect. Mark them seen so the
        // overlay does not replay past toasts — only posts after this point can peek.
        seedActiveAsSeen()
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
        super.onListenerDisconnected()
    }

    private fun syncHeadsUpSuppression() {
        if (NotificationsPreferences(this).enabled) {
            HeadsUpController.disableStockHeadsUp(this)
            if (!HeadsUpController.isCriticalStockPeekActive()) {
                runCatching {
                    requestListenerHints(HINT_HOST_DISABLE_NOTIFICATION_EFFECTS)
                }
            }
        } else {
            runCatching { requestListenerHints(0) }
        }
    }

    private fun seedActiveAsSeen() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        NotificationsOverlayService.seedSeenNotifications(
            keys = active.map { it.key },
            groupKeys = active.mapNotNull { it.groupKeyOrNull() },
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        considerToast(sbn, rankingMap)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null) return
        HeadsUpController.endCriticalStockPeek(this, sbn.key)
        NotificationsOverlayService.onNotificationRemoved(sbn.key, sbn.groupKeyOrNull())
    }

    private fun considerToast(sbn: StatusBarNotification, rankingMap: RankingMap) {
        if (ToastContent.isCriticalInterrupt(sbn)) {
            handleCriticalInterrupt(sbn)
            return
        }

        if (ToastContent.isSelfReply(sbn)) {
            NotificationsOverlayService.markSeenQuietly(
                key = sbn.key,
                groupKey = sbn.groupKeyOrNull(),
                contentSignature = contentSignature(sbn, null),
            )
            return
        }

        val ranking = Ranking()
        val ranked = rankingMap.getRanking(sbn.key, ranking)
        val importance = if (ranked) ranking.importance else NotificationManager.IMPORTANCE_DEFAULT
        val matches = if (ranked) ranking.matchesInterruptionFilter() else true
        val interactive = getSystemService(PowerManager::class.java)?.isInteractive != false
        val onlyAlertOnce = sbn.notification.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0
        val active = runCatching { activeNotifications }.getOrNull()
        val copy = ToastContent.resolve(sbn, active)
        // Group summaries that only carry "N new messages" with no resolvable child copy —
        // remember them quietly; do not toast the placeholder.
        if (ToastContent.isCountSummary(copy.title, copy.body)) {
            NotificationsOverlayService.markSeenQuietly(
                key = sbn.key,
                groupKey = sbn.groupKeyOrNull(),
                contentSignature = contentSignature(sbn, copy),
            )
            return
        }
        NotificationsOverlayService.considerToast(
            packageName = sbn.packageName,
            key = sbn.key,
            groupKey = sbn.groupKeyOrNull(),
            flags = sbn.notification.flags,
            importance = importance,
            matchesInterruptionFilter = matches,
            screenInteractive = interactive,
            isActiveCall = isActiveCall(sbn),
            onlyAlertOnce = onlyAlertOnce,
            title = copy.title,
            body = copy.body,
            contentSignature = contentSignature(sbn, copy),
        )
    }

    /**
     * Temporarily restore stock heads-up and fire full-screen intent when present.
     * Metro toast is skipped — the app's own call / alarm UI owns the interrupt.
     */
    private fun handleCriticalInterrupt(sbn: StatusBarNotification) {
        HeadsUpController.beginCriticalStockPeek(this, sbn.key)
        val fsi = sbn.notification.fullScreenIntent
        if (fsi != null) {
            runCatching { sendContentIntent(this, fsi) }
                .onFailure { Log.w(TAG, "Failed to launch fullScreenIntent for ${sbn.key}", it) }
        }
        // Mark seen so a later non-critical update of the same key does not double-toast.
        NotificationsOverlayService.markSeenQuietly(
            key = sbn.key,
            groupKey = sbn.groupKeyOrNull(),
            contentSignature = "critical:${sbn.key}",
        )
    }

    companion object {
        private const val TAG = "ActionNotificationListener"

        @Volatile
        private var instance: ActionNotificationListenerService? = null

        /** Ask SystemUI to suppress notification effects while Metro toasts are enabled. */
        fun requestHeadsUpSuppression() {
            val service = instance ?: return
            if (HeadsUpController.isCriticalStockPeekActive()) return
            HeadsUpController.disableStockHeadsUp(service)
            runCatching {
                service.requestListenerHints(HINT_HOST_DISABLE_NOTIFICATION_EFFECTS)
            }
        }

        fun clearHeadsUpSuppression() {
            val service = instance ?: return
            runCatching { service.requestListenerHints(0) }
        }

        /**
         * Launch the notifying app via the notification's content intent (WP8.1 toast tap).
         * Must run in the tap callback — delaying past the exit flip loses the user-gesture
         * window and Android blocks the activity start.
         */
        fun openNotification(itemKey: String, cancelAfterOpen: Boolean = true): Boolean {
            val service = instance ?: return false
            val active = runCatching { service.activeNotifications }.getOrNull() ?: return false
            val match = active.firstOrNull { sbn ->
                sbn.key == itemKey ||
                    "${sbn.packageName}:${sbn.id}:${sbn.tag.orEmpty()}" == itemKey
            } ?: return false
            val opened = runCatching {
                val content = match.notification.contentIntent
                if (content != null) {
                    sendContentIntent(service, content)
                    true
                } else {
                    launchPackage(service, match.packageName)
                }
            }.onFailure {
                Log.w(TAG, "Failed to open notification $itemKey", it)
            }.getOrDefault(false)
            if (opened && cancelAfterOpen) {
                runCatching { service.cancelNotification(match.key) }
            }
            return opened
        }

        private fun sendContentIntent(service: ActionNotificationListenerService, intent: PendingIntent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = ActivityOptions.makeBasic().apply {
                    pendingIntentBackgroundActivityStartMode =
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                }
                intent.send(service, 0, null, null, null, null, options.toBundle())
            } else {
                intent.send()
            }
        }

        private fun launchPackage(service: ActionNotificationListenerService, packageName: String): Boolean {
            val launch = service.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            } ?: return false
            service.startActivity(launch)
            return true
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

        /** Active notification keys for the overlay to treat as already-seen (no replay toast). */
        fun activeSeenSnapshot(): Pair<List<String>, List<String>> {
            val service = instance ?: return emptyList<String>() to emptyList()
            val active = runCatching { service.activeNotifications }.getOrNull()
                ?: return emptyList<String>() to emptyList()
            return active.map { it.key } to active.mapNotNull { it.groupKeyOrNull() }
        }
    }
}

private fun isActiveCall(sbn: StatusBarNotification): Boolean =
    sbn.packageName == "com.metro.dialer" && sbn.tag == "active_call"

/** Detect MessagingStyle / text updates so the same key can re-toast on new content. */
private fun contentSignature(sbn: StatusBarNotification, copy: ToastContent.Copy?): String {
    val title = copy?.title ?: ""
    val body = copy?.body.orEmpty()
    val extras = sbn.notification.extras
    @Suppress("DEPRECATION")
    val messageCount = extras.getParcelableArray(Notification.EXTRA_MESSAGES)?.size ?: 0
    @Suppress("DEPRECATION")
    val historyCount = extras.getCharSequenceArray(Notification.EXTRA_REMOTE_INPUT_HISTORY)?.size ?: 0
    return "$title\u0000$body\u0000$messageCount\u0000$historyCount"
}
