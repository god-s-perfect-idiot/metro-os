package com.metro.clock.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.metro.clock.MainActivity

object TimerScheduler {
    const val ACTION_FIRE = "com.metro.clock.ACTION_FIRE_TIMER"
    const val EXTRA_RING_KIND = "ring_kind"
    const val RING_KIND_TIMER = "timer"
    private const val REQUEST_CODE = 0x71_4E_52 // "TMR"

    fun schedule(context: Context, endAtMillis: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingFire(context)
        val show = PendingIntent.getActivity(
            context,
            REQUEST_CODE + 1,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_RING_KIND, RING_KIND_TIMER)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            pendingFlags(),
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(endAtMillis, show), pi)
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(pendingFire(context))
    }

    private fun pendingFire(context: Context): PendingIntent {
        val intent = Intent(context, TimerReceiver::class.java).setAction(ACTION_FIRE)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            pendingFlags(),
        )
    }

    private fun pendingFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
}
