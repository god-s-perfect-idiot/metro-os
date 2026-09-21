package com.metro.widgets.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metro.system.MetroIntents
import com.metro.system.MetroLockscreen
import com.metro.system.MetroTileUpdates
import com.metro.widgets.R
import com.metro.widgets.data.BatterySnapshot
import com.metro.widgets.data.NotifierAccess
import com.metro.widgets.data.NotifierTraySnapshot
import com.metro.widgets.data.NotifierTrayStore
import com.metro.widgets.data.TimeFaceLogic
import com.metro.widgets.data.WidgetKind
import com.metro.widgets.data.WidgetPinLogic
import com.metro.widgets.data.WidgetTelemetry
import com.metro.widgets.tiles.WidgetTorchStore
import java.time.LocalDateTime

class WidgetsState(private val appContext: Context) {
    var clock by mutableStateOf(TimeFaceLogic.parts())
        private set
    var battery by mutableStateOf(WidgetTelemetry.readBattery(appContext))
        private set
    var notifierAccessGranted by mutableStateOf(NotifierAccess.isEnabled(appContext))
        private set
    var notifierTray by mutableStateOf(NotifierTrayStore.snapshot())
        private set
    var torchOn by mutableStateOf(false)
        private set
    var torchAvailable by mutableStateOf(false)
        private set

    private var started = false

    private val torchListener: (Boolean) -> Unit = { on -> torchOn = on }

    private val notifierListener: () -> Unit = {
        notifierTray = NotifierTrayStore.snapshot()
        notifierAccessGranted = NotifierAccess.isEnabled(appContext)
        MetroTileUpdates.requestUpdate(
            appContext,
            appContext.packageName,
            WidgetKind.Notifier.id,
        )
    }

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
            MetroTileUpdates.requestUpdate(
                appContext,
                appContext.packageName,
                WidgetKind.Battery.id,
            )
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
        refreshNotifierAccess()
        battery = WidgetTelemetry.readBattery(appContext)
        notifierTray = NotifierTrayStore.snapshot()
        val torch = WidgetTorchStore.ensure(appContext)
        torchAvailable = torch.isAvailable
        torchOn = WidgetTorchStore.isOn(appContext)
        WidgetTorchStore.addListener(torchListener)
        NotifierTrayStore.addListener(notifierListener)
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
        NotifierTrayStore.removeListener(notifierListener)
        WidgetTorchStore.removeListener(torchListener)
        // Keep WidgetTorchStore listening for Start-pinned torch tiles.
        runCatching { appContext.unregisterReceiver(batteryReceiver) }
        runCatching { appContext.unregisterReceiver(timeReceiver) }
    }

    fun hasTorchCameraPermission(): Boolean = WidgetTorchStore.hasCameraPermission(appContext)

    fun toggleTorch(): Boolean = WidgetTorchStore.toggle(appContext)

    fun refreshClock(now: LocalDateTime = LocalDateTime.now()) {
        clock = TimeFaceLogic.parts(now)
    }

    fun refreshNotifierAccess() {
        notifierAccessGranted = NotifierAccess.isEnabled(appContext)
        notifierTray = NotifierTrayStore.snapshot()
    }

    fun openNotifierAccessSettings() {
        NotifierAccess.openSettings(appContext)
    }

    /** Lock the device via Metro lockscreen a11y; opens Accessibility settings if unavailable. */
    fun lockDevice() {
        if (MetroLockscreen.isAccessibilityEnabled(appContext)) {
            MetroLockscreen.requestLock(appContext)
        } else {
            MetroLockscreen.openAccessibilitySettings(appContext)
        }
    }

    /** Pin a catalog widget as a secondary Start tile (launcher brings Start forward). */
    fun pinToStart(kind: WidgetKind) {
        MetroIntents.requestPinTile(
            context = appContext,
            packageName = MetroIntents.PACKAGE_WIDGETS,
            tileId = WidgetPinLogic.tileId(kind),
            size = WidgetPinLogic.pinSizeStorageValue(kind),
        )
        Toast.makeText(appContext, R.string.pinned_to_start, Toast.LENGTH_SHORT).show()
    }
}
