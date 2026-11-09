package com.metro.clock.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.metro.clock.MainActivity

object AlarmScheduler {
    const val EXTRA_ALARM_ID = "alarm_id"
    const val ACTION_FIRE = "com.metro.clock.ACTION_FIRE_ALARM"

    fun rescheduleAll(context: Context, alarms: List<AlarmItem> = AlarmStore(context).load()) {
        AlarmNotifier.ensureChannel(context)
        alarms.forEach { schedule(context, it) }
    }

    fun schedule(context: Context, alarm: AlarmItem) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = AlarmLogic.nextTriggerMillis(alarm)
        val pi = pendingFire(context, alarm.id)
        if (triggerAt == null) {
            am.cancel(pi)
            return
        }
        val show = PendingIntent.getActivity(
            context,
            showRequestCode(alarm.id),
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_ALARM_ID, alarm.id)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            pendingFlags(),
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, show), pi)
    }

    fun cancel(context: Context, alarmId: String) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(pendingFire(context, alarmId))
        AlarmNotifier.cancelAlarm(context, alarmId)
    }

    private fun pendingFire(context: Context, alarmId: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(ACTION_FIRE)
            .putExtra(EXTRA_ALARM_ID, alarmId)
        return PendingIntent.getBroadcast(
            context,
            fireRequestCode(alarmId),
            intent,
            pendingFlags(),
        )
    }

    private fun fireRequestCode(alarmId: String): Int =
        (alarmId.hashCode() and 0x7FFF_FFFF)

    private fun showRequestCode(alarmId: String): Int =
        (alarmId.hashCode() and 0x7FFF_FFFF) xor 0x5A5A

    private fun pendingFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
}
