package com.metro.launcher

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.metro.launcher.data.MusicNowPlayingInfo
import com.metro.launcher.data.MusicNowPlayingStore
import com.metro.launcher.data.TileNotificationInfo
import com.metro.launcher.data.TileNotificationStore
import com.metro.system.MetroTileUpdates

/**
 * Reads active notifications for every package and feeds [TileNotificationStore] so Start live
 * tiles can show WP8.1-style badges, flip/peek faces, and progress overlays (charging /
 * downloads whose notifications carry a progress bar or remaining-time caption).
 *
 * Also binds [MusicNowPlayingStore] to [android.media.session.MediaSessionManager] (same
 * notification-listener privilege) so pinned music apps can show Xbox Music–style now-playing.
 *
 * Peek-face taps call [openNotification] to fire the shade item's contentIntent (same pattern
 * as toast / Conversations).
 */
class TileNotificationListenerService : NotificationListenerService() {
    private val musicListener: (String) -> Unit = { packageName ->
        MetroTileUpdates.requestUpdate(this, packageName)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        MusicNowPlayingStore.addListener(musicListener)
        MusicNowPlayingStore.bind(
            context = this,
            listenerComponent = ComponentName(this, TileNotificationListenerService::class.java),
        )
        publishAll()
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
        MusicNowPlayingStore.removeListener(musicListener)
        MusicNowPlayingStore.unbind()
        TileNotificationStore.clear()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        publishAll()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        publishAll()
    }

    private fun publishAll() {
        val previous = TileNotificationStore.all()
        val active = runCatching { activeNotifications }.getOrNull()
        TileNotificationStore.replaceAll(this, active)
        val next = TileNotificationStore.all()
        changedPackages(previous, next).forEach { packageName ->
            MetroTileUpdates.requestUpdate(this, packageName)
        }
    }

    companion object {
        private const val TAG = "TileNotificationListener"

        @Volatile
        private var instance: TileNotificationListenerService? = null

        /**
         * Launch via the notification's contentIntent (must run in the tap callback —
         * delaying past the Start exit wave loses the user-gesture / BAL window).
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

        private fun sendContentIntent(
            service: TileNotificationListenerService,
            intent: PendingIntent,
        ) {
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

        private fun launchPackage(
            service: TileNotificationListenerService,
            packageName: String,
        ): Boolean {
            val launch = service.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            } ?: return false
            service.startActivity(launch)
            return true
        }

        internal fun changedPackages(
            previous: Map<String, TileNotificationInfo>,
            next: Map<String, TileNotificationInfo>,
        ): Set<String> {
            val keys = previous.keys + next.keys
            return keys.filter { previous[it] != next[it] }.toSet()
        }

        internal fun changedMusicPackages(
            previous: Map<String, MusicNowPlayingInfo>,
            next: Map<String, MusicNowPlayingInfo>,
        ): Set<String> {
            val keys = previous.keys + next.keys
            return keys.filter { previous[it] != next[it] }.toSet()
        }
    }
}
