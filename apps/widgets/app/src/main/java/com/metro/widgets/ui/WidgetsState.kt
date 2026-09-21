package com.metro.widgets.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metro.widgets.data.BatterySnapshot
import com.metro.widgets.data.ClockFaceParts
import com.metro.widgets.data.TimeFaceLogic
import com.metro.widgets.data.WidgetTelemetry
import java.time.LocalDateTime

class WidgetsState(private val appContext: Context) {
    var clock by mutableStateOf(TimeFaceLogic.parts())
        private set
    var battery by mutableStateOf(WidgetTelemetry.readBattery(appContext))
        private set
    var storage by mutableStateOf(WidgetTelemetry.readStorage())
        private set

    private var started = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val percent = if (level >= 0 && scale > 0) {
                ((level * 100f) / scale + 0.5f).toInt().coerceIn(0, 100)
            } else {
                battery.percent
            }
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            battery = BatterySnapshot(percent = percent, charging = charging)
        }
    }

    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshClock()
        }
    }

    fun start() {
        if (started) return
        started = true
        refreshClock()
        battery = WidgetTelemetry.readBattery(appContext)
        storage = WidgetTelemetry.readStorage()
        appContext.registerReceiver(
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        )
        val timeFilter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        appContext.registerReceiver(timeReceiver, timeFilter)
    }

    fun stop() {
        if (!started) return
        started = false
        runCatching { appContext.unregisterReceiver(batteryReceiver) }
        runCatching { appContext.unregisterReceiver(timeReceiver) }
    }

    fun refreshClock(now: LocalDateTime = LocalDateTime.now()) {
        clock = TimeFaceLogic.parts(now)
    }
}
