package com.metro.dialer.telecom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.metro.dialer.InCallActivity
import com.metro.dialer.R
import com.metro.dialer.data.ActiveCall
import com.metro.dialer.data.DialerCallLogic
import com.metro.system.MetroPreferences

/**
 * Posts the ongoing active-call notification used when the in-call UI is backgrounded.
 *
 * Content is intentionally minimal (name + status/timer) so the Metro green return-to-call
 * banner can present it in WP8.1 form without Material CallStyle chrome.
 */
object ActiveCallNotifier {
    const val NOTIFICATION_ID = 0xC411
    const val NOTIFICATION_TAG = "active_call"
    private const val CHANNEL_ID = "metro_active_call"

    private val handler = Handler(Looper.getMainLooper())
    private var ticking = false

    private val tickRunnable = object : Runnable {
        override fun run() {
            val call = MetroCallSession.activeCall.value
            if (call == null) {
                ticking = false
                return
            }
            val context = appContext ?: return
            post(context, call)
            handler.postDelayed(this, 1_000L)
        }
    }

    @Volatile
    private var appContext: Context? = null

    fun start(context: Context, call: ActiveCall) {
        appContext = context.applicationContext
        ensureChannel(context.applicationContext)
        post(context.applicationContext, call)
        if (!ticking) {
            ticking = true
            handler.postDelayed(tickRunnable, 1_000L)
        }
    }

    fun update(context: Context, call: ActiveCall) {
        appContext = context.applicationContext
        ensureChannel(context.applicationContext)
        post(context.applicationContext, call)
    }

    fun stop(context: Context) {
        ticking = false
        handler.removeCallbacks(tickRunnable)
        NotificationManagerCompat.from(context.applicationContext)
            .cancel(NOTIFICATION_TAG, NOTIFICATION_ID)
        appContext = null
    }

    private fun post(context: Context, call: ActiveCall) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, InCallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val endIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, EndCallActionReceiver::class.java).setAction(EndCallActionReceiver.ACTION_END),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val statusText = if (call.connected) {
            val elapsed = ((System.currentTimeMillis() - call.startedAtMillis) / 1000L)
                .coerceAtLeast(0L)
                .toInt()
            DialerCallLogic.formatDuration(elapsed)
        } else {
            context.getString(R.string.calling)
        }

        val accent = runCatching {
            android.graphics.Color.parseColor(MetroPreferences(context).accentColorHex)
        }.getOrDefault(0xFF1BA1E2.toInt())

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_phone)
            .setContentTitle(call.displayName)
            .setContentText(statusText)
            .setSubText(context.getString(R.string.active_call_notification_label))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setColor(accent)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                R.drawable.ic_notification_phone,
                context.getString(R.string.end_call),
                endIntent,
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            manager.notify(NOTIFICATION_TAG, NOTIFICATION_ID, notification)
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        // Drop the temporary quiet channel from an earlier heads-up fix.
        manager.deleteNotificationChannel("metro_active_call_quiet")
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.active_call_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.active_call_channel_description)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
            },
        )
    }
}
