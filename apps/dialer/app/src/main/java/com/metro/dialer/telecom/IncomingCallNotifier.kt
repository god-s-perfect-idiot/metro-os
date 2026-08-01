package com.metro.dialer.telecom

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.metro.dialer.InCallActivity
import com.metro.dialer.R
import com.metro.dialer.data.ActiveCall
import com.metro.system.MetroPreferences

/**
 * Incoming-call notification used only when the Metro incoming UI cannot be started directly
 * (locked / screen-off). It carries a full-screen intent so [InCallActivity] can still appear.
 *
 * Never posted while the device is unlocked and interactive — that path would produce an Android
 * heads-up over the WP incoming-call page.
 */
object IncomingCallNotifier {
    const val NOTIFICATION_ID = 0xC412
    const val NOTIFICATION_TAG = "incoming_call"
    private const val CHANNEL_ID = "metro_incoming_call"

    fun show(context: Context, call: ActiveCall) {
        val appContext = context.applicationContext
        ensureChannel(appContext)
        val manager = NotificationManagerCompat.from(appContext)
        if (!manager.areNotificationsEnabled()) return

        val fullScreenIntent = PendingIntent.getActivity(
            appContext,
            0,
            Intent(appContext, InCallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val accent = runCatching {
            android.graphics.Color.parseColor(MetroPreferences(appContext).accentColorHex)
        }.getOrDefault(0xFF1BA1E2.toInt())

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_phone)
            .setContentTitle(call.displayName)
            .setContentText(appContext.getString(R.string.incoming_call_label))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setAutoCancel(false)
            .setColor(accent)
            .setContentIntent(fullScreenIntent)
            .setFullScreenIntent(fullScreenIntent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setTimeoutAfter(60_000L)
            .build()

        runCatching {
            manager.notify(NOTIFICATION_TAG, NOTIFICATION_ID, notification)
        }
    }

    fun stop(context: Context) {
        NotificationManagerCompat.from(context.applicationContext)
            .cancel(NOTIFICATION_TAG, NOTIFICATION_ID)
    }

    /** True when a service activity-start is likely blocked and FSI is required. */
    fun needsFullScreenIntent(context: Context): Boolean {
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        if (keyguard?.isKeyguardLocked == true) return true
        val power = context.getSystemService(PowerManager::class.java)
        return power?.isInteractive == false
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.incoming_call_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.incoming_call_channel_description)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
            },
        )
    }
}
