package com.metro.clock.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.metro.clock.MainActivity

object AlarmNotifier {
    private const val CHANNEL_ID = "metro_clock_alarms"
    private const val CHANNEL_NAME = "Alarms"
    private const val NOTIFICATION_BASE = 0x41_4C_52 // "ALR"
    private const val TIMER_NOTIFICATION_ID = 0x54_4D_52 // "TMR"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val existing = nm.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Alarm and timer alerts"
                setBypassDnd(true)
                enableVibration(true)
            },
        )
    }

    fun notifyAlarm(context: Context, alarm: AlarmItem) {
        ensureChannel(context)
        val open = openMainPending(
            context = context,
            requestCode = requestCode(alarm.id),
            extras = mapOf(AlarmScheduler.EXTRA_ALARM_ID to alarm.id),
        )
        val timeLabel = AlarmLogic.formatTime(alarm.hour, alarm.minute)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(alarm.name.ifBlank { "Alarm" })
            .setContentText(timeLabel)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            ?.notify(NOTIFICATION_BASE + requestCode(alarm.id), notification)

        runCatching {
            context.startActivity(
                Intent(context, MainActivity::class.java)
                    .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
        }
    }

    fun notifyTimerFinished(context: Context) {
        ensureChannel(context)
        val open = openMainPending(
            context = context,
            requestCode = TIMER_NOTIFICATION_ID,
            extras = mapOf(TimerScheduler.EXTRA_RING_KIND to TimerScheduler.RING_KIND_TIMER),
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Timer")
            .setContentText("Time's up")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        context.getSystemService(NotificationManager::class.java)
            ?.notify(TIMER_NOTIFICATION_ID, notification)
    }

    fun cancelAlarm(context: Context, alarmId: String) {
        context.getSystemService(NotificationManager::class.java)
            ?.cancel(NOTIFICATION_BASE + requestCode(alarmId))
    }

    private fun openMainPending(
        context: Context,
        requestCode: Int,
        extras: Map<String, String>,
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        extras.forEach { (key, value) -> intent.putExtra(key, value) }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun requestCode(alarmId: String): Int = alarmId.hashCode() and 0x7FFF_FFFF
}
