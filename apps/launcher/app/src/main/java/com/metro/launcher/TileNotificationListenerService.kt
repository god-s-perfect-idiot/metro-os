package com.metro.launcher

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
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
 */
class TileNotificationListenerService : NotificationListenerService() {
    private val musicListener: (String) -> Unit = { packageName ->
        MetroTileUpdates.requestUpdate(this, packageName)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        MusicNowPlayingStore.addListener(musicListener)
        MusicNowPlayingStore.bind(
            context = this,
            listenerComponent = ComponentName(this, TileNotificationListenerService::class.java),
        )
        publishAll()
    }

    override fun onListenerDisconnected() {
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
