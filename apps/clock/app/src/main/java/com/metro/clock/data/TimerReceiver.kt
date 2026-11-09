package com.metro.clock.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TimerScheduler.ACTION_FIRE) return
        if (!ClockAlertBus.offer(ClockAlert.Timer)) {
            AlarmNotifier.notifyTimerFinished(context)
        }
    }
}
