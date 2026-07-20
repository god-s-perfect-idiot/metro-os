package com.metro.statusbar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metro.system.MetroBroadcasts
import com.metro.system.MetroPreferences
import java.time.ZonedDateTime

class TrayState(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = MetroPreferences(appContext)

    var expanded by mutableStateOf(false)
        private set

    var showProgress by mutableStateOf(false)
        private set

    var visibilityMode by mutableStateOf(TrayVisibilityMode.Opaque)
        private set

    var theme by mutableStateOf(TrayThemeResolver.resolve(preferences, visibilityMode))
        private set

    var clockText by mutableStateOf(TrayClockFormatter.format())
        private set

    var battery by mutableStateOf(BatteryStatus.Unknown)
        private set

    var dataConnectionLabel by mutableStateOf<String?>(null)
        private set

    var lastExpandedAtMs by mutableLongStateOf(0L)
        private set

    private var telephonyManager: TelephonyManager? = null
    private var telephonyCallback: TelephonyCallback? = null

    val snapshot: TraySnapshot
        get() = TraySnapshot(
            clockText = clockText,
            expanded = expanded,
            showProgress = showProgress,
            indicators = if (expanded) TrayIndicatorOrder.expanded else TrayIndicatorOrder.collapsed,
            dataConnectionLabel = dataConnectionLabel,
            battery = battery,
            theme = theme,
        )

    private val themeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MetroBroadcasts.ACTION_THEME_CHANGED) {
                refreshTheme()
            }
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                battery = BatterySource.parse(intent)
            }
        }
    }

    fun refreshTheme() {
        theme = TrayThemeResolver.resolve(preferences, visibilityMode)
    }

    fun refreshClock(now: ZonedDateTime = ZonedDateTime.now()) {
        clockText = TrayClockFormatter.format(now)
    }

    fun refreshBattery() {
        battery = BatterySource.current(appContext)
    }

    fun refreshDataConnectionLabel() {
        dataConnectionLabel = CellularDataSource.current(appContext)
    }

    fun expand(nowMs: Long = System.currentTimeMillis()) {
        expanded = true
        lastExpandedAtMs = nowMs
    }

    fun collapse() {
        expanded = false
    }

    fun toggleExpanded(nowMs: Long = System.currentTimeMillis()) {
        if (expanded) {
            collapse()
        } else {
            expand(nowMs)
        }
    }

    fun tickAutoCollapse(nowMs: Long = System.currentTimeMillis()) {
        if (TrayCollapseScheduler.shouldAutoCollapse(expanded, lastExpandedAtMs, nowMs)) {
            collapse()
        }
    }

    fun setProgressVisible(visible: Boolean) {
        showProgress = visible
    }

    fun applyVisibilityMode(mode: TrayVisibilityMode) {
        visibilityMode = mode
        refreshTheme()
    }

    fun registerReceivers(context: Context) {
        val filter = IntentFilter(MetroBroadcasts.ACTION_THEME_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(themeReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(themeReceiver, filter)
        }
        // Sticky broadcast; the registration call also returns the current battery state.
        val sticky = context.registerReceiver(
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        )
        battery = BatterySource.parse(sticky)
        refreshDataConnectionLabel()
        registerTelephonyUpdates(context)
    }

    fun unregisterReceivers(context: Context) {
        runCatching { context.unregisterReceiver(themeReceiver) }
        runCatching { context.unregisterReceiver(batteryReceiver) }
        unregisterTelephonyUpdates()
    }

    private fun registerTelephonyUpdates(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (!CellularDataSource.canRead(context)) return
        val manager = context.getSystemService(TelephonyManager::class.java) ?: return
        telephonyManager = manager
        val callback = object : TelephonyCallback(), TelephonyCallback.DisplayInfoListener {
            override fun onDisplayInfoChanged(displayInfo: TelephonyDisplayInfo) {
                dataConnectionLabel = DataConnectionLabels.fromDisplayInfo(
                    networkType = displayInfo.networkType,
                    overrideNetworkType = displayInfo.overrideNetworkType,
                )
            }
        }
        telephonyCallback = callback
        runCatching {
            manager.registerTelephonyCallback(context.mainExecutor, callback)
        }.onFailure {
            telephonyManager = null
            telephonyCallback = null
        }
    }

    private fun unregisterTelephonyUpdates() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val manager = telephonyManager ?: return
        val callback = telephonyCallback ?: return
        runCatching { manager.unregisterTelephonyCallback(callback) }
        telephonyManager = null
        telephonyCallback = null
    }
}
