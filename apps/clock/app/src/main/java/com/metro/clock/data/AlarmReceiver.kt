package com.metro.clock.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_FIRE) return
        val id = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
        val store = AlarmStore(context)
        val alarm = store.load().firstOrNull { it.id == id } ?: return

        if (!ClockAlertBus.offer(ClockAlert.Alarm(alarm))) {
            AlarmNotifier.notifyAlarm(context, alarm)
        } else {
            AlarmNotifier.cancelAlarm(context, alarm.id)
        }

        if (alarm.repeatDays.isEmpty()) {
            store.setEnabled(id, false)
        } else {
            AlarmScheduler.schedule(context, alarm)
        }
    }
}
