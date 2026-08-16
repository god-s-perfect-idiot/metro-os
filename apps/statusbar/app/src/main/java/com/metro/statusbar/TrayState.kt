package com.metro.statusbar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
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
    private val trayPrefs = StatusTrayPreferences(appContext)

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

    var signalBars by mutableStateOf(SignalBarsStatus.Unknown)
        private set

    var lastExpandedAtMs by mutableLongStateOf(0L)
        private set

    /** True while Android's notification shade covers the tray region. */
    var notificationShadeOpen by mutableStateOf(false)
        private set

    /**
     * True while Android system status bars are hidden (immersive / fullscreen). The Metro
     * overlay must not paint over fullscreen content.
     */
    var systemStatusBarsHidden by mutableStateOf(false)
        private set

    private var telephonyManager: TelephonyManager? = null
    private var telephonyCallback: TelephonyCallback? = null
    @Suppress("DEPRECATION")
    private var phoneStateListener: PhoneStateListener? = null

    val snapshot: TraySnapshot
        get() = TraySnapshot(
            clockText = clockText,
            expanded = expanded,
            showProgress = showProgress,
            // Always the full expanded set so exit animations can run when [expanded] flips false.
            indicators = TrayIndicatorOrder.expanded,
            dataConnectionLabel = dataConnectionLabel,
            signalBars = signalBars,
            battery = battery,
            theme = theme,
            notificationShadeOpen = notificationShadeOpen,
            systemStatusBarsHidden = systemStatusBarsHidden,
        )

    /** Left icons + battery when present — drives stagger timing for auto-collapse. */
    fun animatingIconCount(): Int {
        val left = TrayIndicatorOrder.visibleLeft(
            dataConnectionLabel = dataConnectionLabel,
            wifiConnected = signalBars.wifiBands != null,
        ).size
        val batteryIcon = if (battery.present) 1 else 0
        return left + batteryIcon
    }

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

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiManager.RSSI_CHANGED_ACTION,
                WifiManager.NETWORK_STATE_CHANGED_ACTION,
                WifiManager.WIFI_STATE_CHANGED_ACTION,
                -> refreshWifiSignal()
            }
        }
    }

    fun refreshTheme() {
        preferences.pullThemeFromProvider()
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

    fun refreshSignalBars() {
        signalBars = RadioSignalSource.current(appContext)
        // Late-grant: attach listeners once READ_PHONE_STATE becomes available.
        if (telephonyManager == null && CellularSignalSource.canRead(appContext)) {
            registerTelephonyUpdates(appContext)
        }
    }

    fun refreshWifiSignal() {
        signalBars = signalBars.copy(wifiBands = WifiSignalSource.currentBands(appContext))
    }

    fun refreshCellularSignal(signalStrength: SignalStrength? = null) {
        val bars = if (signalStrength != null) {
            CellularSignalLevels.fromSignalStrength(signalStrength)
        } else {
            CellularSignalSource.currentBars(appContext)
        }
        signalBars = signalBars.copy(cellularBars = bars)
    }

    fun expand(nowMs: Long = System.currentTimeMillis()) {
        // Start / home always restores a visible tray — leave fullscreen hide behind.
        if (visibilityMode == TrayVisibilityMode.Hidden) {
            applyVisibilityMode(TrayVisibilityMode.Opaque)
        }
        refreshSignalBars()
        refreshDataConnectionLabel()
        expanded = true
        lastExpandedAtMs = nowMs
    }

    fun collapse() {
        expanded = false
    }

    /** Tap / home always (re)starts the reveal; does not toggle closed mid-hold. */
    fun toggleExpanded(nowMs: Long = System.currentTimeMillis()) {
        expand(nowMs)
    }

    fun tickAutoCollapse(nowMs: Long = System.currentTimeMillis()) {
        if (
            TrayCollapseScheduler.shouldAutoCollapse(
                expanded = expanded,
                lastExpandedAtMs = lastExpandedAtMs,
                nowMs = nowMs,
                animatingIconCount = animatingIconCount(),
                holdMs = trayPrefs.iconHideTimeoutMs,
            )
        ) {
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

    /** Hide the Metro tray while the Android notification shade is expanded. */
    fun applyNotificationShadeOpen(open: Boolean) {
        if (notificationShadeOpen == open) return
        notificationShadeOpen = open
        if (open) {
            collapse()
        }
    }

    /** Hide the Metro tray while Android status bars are immersive-hidden. */
    fun applySystemStatusBarsHidden(hidden: Boolean) {
        if (systemStatusBarsHidden == hidden) return
        systemStatusBarsHidden = hidden
        if (hidden) {
            collapse()
        }
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
        val wifiFilter = IntentFilter().apply {
            addAction(WifiManager.RSSI_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(wifiReceiver, wifiFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(wifiReceiver, wifiFilter)
        }
        refreshDataConnectionLabel()
        refreshSignalBars()
        registerTelephonyUpdates(context)
    }

    fun unregisterReceivers(context: Context) {
        runCatching { context.unregisterReceiver(themeReceiver) }
        runCatching { context.unregisterReceiver(batteryReceiver) }
        runCatching { context.unregisterReceiver(wifiReceiver) }
        unregisterTelephonyUpdates()
    }

    private fun registerTelephonyUpdates(context: Context) {
        if (!CellularDataSource.canRead(context) && !CellularSignalSource.canRead(context)) return
        val manager = context.getSystemService(TelephonyManager::class.java) ?: return
        telephonyManager = manager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object :
                TelephonyCallback(),
                TelephonyCallback.DisplayInfoListener,
                TelephonyCallback.SignalStrengthsListener {
                override fun onDisplayInfoChanged(displayInfo: TelephonyDisplayInfo) {
                    dataConnectionLabel = DataConnectionLabels.fromDisplayInfo(
                        networkType = displayInfo.networkType,
                        overrideNetworkType = displayInfo.overrideNetworkType,
                    )
                }

                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    refreshCellularSignal(signalStrength)
                }
            }
            telephonyCallback = callback
            runCatching {
                manager.registerTelephonyCallback(context.mainExecutor, callback)
            }.onFailure {
                telephonyManager = null
                telephonyCallback = null
            }
            return
        }

        @Suppress("DEPRECATION")
        val listener = object : PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                refreshCellularSignal(signalStrength)
            }
        }
        phoneStateListener = listener
        @Suppress("DEPRECATION")
        runCatching {
            manager.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
        }.onFailure {
            telephonyManager = null
            phoneStateListener = null
        }
    }

    private fun unregisterTelephonyUpdates() {
        val manager = telephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = telephonyCallback
            if (manager != null && callback != null) {
                runCatching { manager.unregisterTelephonyCallback(callback) }
            }
            telephonyCallback = null
        } else {
            val listener = phoneStateListener
            if (manager != null && listener != null) {
                @Suppress("DEPRECATION")
                runCatching { manager.listen(listener, PhoneStateListener.LISTEN_NONE) }
            }
            phoneStateListener = null
        }
        telephonyManager = null
    }
}
