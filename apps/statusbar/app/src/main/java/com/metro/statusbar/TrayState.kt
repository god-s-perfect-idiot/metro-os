package com.metro.statusbar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
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
import androidx.compose.ui.graphics.Color
import com.metro.system.MetroAppRegistry
import com.metro.system.MetroBroadcasts
import com.metro.system.MetroPreferences
import com.metro.system.MetroStatusBar
import com.metro.ui.MetroColors
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

    /** Foreground app package used when [StatusTrayPreferences.matchAppBackground] is on. */
    var foregroundPackage by mutableStateOf<String?>(null)
        private set

    /** Theme background for the foreground non-Metro app; null when matching is off. */
    var appBackgroundColor by mutableStateOf<Color?>(null)
        private set

    /**
     * Temporary toast accent fill while a Metro toast banner is visible.
     */
    var notificationsShellFill by mutableStateOf<Color?>(null)
        private set

    /**
     * When [notificationsShellFill] is set, true = tray transparent over the toast underlay
     * (continuous flip band); false = opaque accent on the tray (exit handoff).
     */
    var notificationsShellUnderlay by mutableStateOf(false)
        private set

    /** Temporary charcoal fill while the Metro volume HUD is visible (outranks toast). */
    var volumeShellFill by mutableStateOf<Color?>(null)
        private set

    /**
     * When [volumeShellFill] is set, true = tray transparent over the HUD underlay;
     * false = tray paints opaque charcoal (exit handoff so the system bar stays covered).
     */
    var volumeShellUnderlay by mutableStateOf(true)
        private set

    /**
     * Morph duration for the next tray fill / glyph transition driven by shell overlays.
     * Overlays set this to match their enter/exit motion.
     */
    var shellFillAnimationMs by mutableStateOf(MetroStatusBar.SHELL_FILL_DURATION_MS_DEFAULT)
        private set

    var theme by mutableStateOf(resolveTheme())
        private set

    var clockText by mutableStateOf(TrayClockFormatter.format())
        private set

    var battery by mutableStateOf(BatteryStatus.Unknown)
        private set

    var dataConnectionLabel by mutableStateOf<String?>(null)
        private set

    var signalBars by mutableStateOf(SignalBarsStatus.Unknown)
        private set

    /** True when [AudioManager.STREAM_RING] volume is 0 — mute glyph after Wi-Fi. */
    var ringerMuted by mutableStateOf(false)
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
            ringerMuted = ringerMuted,
            battery = battery,
            theme = theme,
            notificationShadeOpen = notificationShadeOpen,
            systemStatusBarsHidden = systemStatusBarsHidden,
            shellFillAnimationMs = shellFillAnimationMs,
        )

    /** Left icons + battery when present — drives stagger timing for auto-collapse. */
    fun animatingIconCount(): Int {
        val left = TrayIndicatorOrder.visibleLeft(
            dataConnectionLabel = dataConnectionLabel,
            wifiConnected = signalBars.wifiBands != null,
            ringerMuted = ringerMuted,
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

    private val ringerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                VOLUME_CHANGED_ACTION,
                AudioManager.RINGER_MODE_CHANGED_ACTION,
                -> refreshRingerMute()
            }
        }
    }

    fun refreshTheme() {
        preferences.pullThemeFromProvider()
        if (!trayPrefs.matchAppBackground) {
            appBackgroundColor = null
        }
        theme = resolveTheme()
    }

    /**
     * Tracks the foreground package. Metro suite apps keep the Metro page fill; other apps may
     * use their published status-bar / primary theme color when match-app-background is on.
     */
    fun applyForegroundPackage(packageName: String?) {
        val normalized = packageName?.takeUnless { ForegroundAppDetector.isIgnored(it) }
        if (foregroundPackage == normalized) {
            if (trayPrefs.matchAppBackground && !isMetroSuiteForeground() && appBackgroundColor == null) {
                resolveThemeBackgroundForForeground()
            }
            return
        }
        foregroundPackage = normalized
        if (isMetroSuiteForeground() || !trayPrefs.matchAppBackground) {
            appBackgroundColor = null
            theme = resolveTheme()
            return
        }
        resolveThemeBackgroundForForeground()
    }

    /** Re-reads the match-app-background toggle and resolves theme fill for the current app. */
    fun applyMatchAppBackgroundPreference() {
        if (!trayPrefs.matchAppBackground || isMetroSuiteForeground()) {
            appBackgroundColor = null
            theme = resolveTheme()
            return
        }
        resolveThemeBackgroundForForeground()
    }

    private fun resolveThemeBackgroundForForeground() {
        val pkg = foregroundPackage
        val resolved = if (pkg.isNullOrBlank()) {
            null
        } else {
            AppThemeBackgroundResolver.resolve(appContext, pkg)
        }
        val next = resolved ?: MetroColors.background(preferences.isDark)
        if (!AppThemeBackgroundResolver.isMateriallyDifferent(appBackgroundColor, next)) {
            theme = resolveTheme()
            return
        }
        appBackgroundColor = next
        theme = resolveTheme()
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

    fun refreshRingerMute() {
        ringerMuted = RingerMuteSource.isMuted(appContext)
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
        refreshRingerMute()
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
        theme = resolveTheme()
    }

    /**
     * Applies or clears a temporary shell-overlay fill. [owner] must be
     * [MetroStatusBar.OWNER_NOTIFICATIONS] or [MetroStatusBar.OWNER_VOLUME]; unknown owners are
     * ignored. Volume outranks notifications when both are set.
     *
     * Toast / volume can paint as an underlay (tray transparent over a continuous band) or as an
     * opaque tray fill (exit handoff so the system bar stays covered).
     *
     * [durationMs] is stored for [StatusTray] color morphs so they can match the overlay motion.
     * [underlay] applies while that owner's color is set; clears reset underlay flags.
     */
    fun applyShellFill(
        owner: String?,
        color: Color?,
        durationMs: Int = MetroStatusBar.SHELL_FILL_DURATION_MS_DEFAULT,
        underlay: Boolean = owner == MetroStatusBar.OWNER_VOLUME && color != null,
    ) {
        shellFillAnimationMs = durationMs.coerceAtLeast(0)
        when (owner) {
            MetroStatusBar.OWNER_NOTIFICATIONS -> {
                val nextUnderlay = if (color == null) false else underlay
                if (notificationsShellFill == color && notificationsShellUnderlay == nextUnderlay) {
                    return
                }
                notificationsShellFill = color
                notificationsShellUnderlay = nextUnderlay
            }
            MetroStatusBar.OWNER_VOLUME -> {
                val nextUnderlay = if (color == null) true else underlay
                if (volumeShellFill == color && volumeShellUnderlay == nextUnderlay) return
                volumeShellFill = color
                volumeShellUnderlay = nextUnderlay
            }
            else -> return
        }
        theme = resolveTheme()
    }

    private fun effectiveShellFill(): Color? = volumeShellFill ?: notificationsShellFill

    /**
     * Active shell overlay paints under the tray (transparent) so one continuous band can animate
     * behind the glyphs. Volume outranks toast.
     */
    private fun shellFillUnderlay(): Boolean = when {
        volumeShellFill != null -> volumeShellUnderlay
        notificationsShellFill != null -> notificationsShellUnderlay
        else -> false
    }

    private fun resolveTheme(): TrayThemeSnapshot =
        TrayThemeResolver.resolve(
            preferences = preferences,
            visibilityMode = visibilityMode,
            matchAppBackground = trayPrefs.matchAppBackground,
            appBackgroundColor = appBackgroundColor,
            metroSuiteForeground = isMetroSuiteForeground(),
            shellFillColor = effectiveShellFill(),
            shellFillUnderlay = shellFillUnderlay(),
        )

    private fun isMetroSuiteForeground(): Boolean {
        val pkg = foregroundPackage ?: return false
        return MetroAppRegistry.isMetroSuite(pkg)
    }

    /** Hide the Metro tray while the Android notification shade is expanded. */
    fun applyNotificationShadeOpen(open: Boolean) {
        if (notificationShadeOpen == open) return
        notificationShadeOpen = open
        if (open) {
            collapse()
        } else {
            ensureExpandedIfNeverHides()
        }
    }

    /** Hide the Metro tray while Android status bars are immersive-hidden. */
    fun applySystemStatusBarsHidden(hidden: Boolean) {
        if (systemStatusBarsHidden == hidden) return
        systemStatusBarsHidden = hidden
        if (hidden) {
            collapse()
        } else {
            ensureExpandedIfNeverHides()
        }
    }

    /** Keep indicators expanded when setup chooses Never for hide-icons. */
    fun ensureExpandedIfNeverHides(nowMs: Long = System.currentTimeMillis()) {
        if (trayPrefs.neverHidesIcons) {
            expand(nowMs)
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
        val ringerFilter = IntentFilter().apply {
            addAction(VOLUME_CHANGED_ACTION)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(ringerReceiver, ringerFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(ringerReceiver, ringerFilter)
        }
        refreshDataConnectionLabel()
        refreshSignalBars()
        refreshRingerMute()
        registerTelephonyUpdates(context)
    }

    fun unregisterReceivers(context: Context) {
        runCatching { context.unregisterReceiver(themeReceiver) }
        runCatching { context.unregisterReceiver(batteryReceiver) }
        runCatching { context.unregisterReceiver(wifiReceiver) }
        runCatching { context.unregisterReceiver(ringerReceiver) }
        unregisterTelephonyUpdates()
    }

    companion object {
        /** Same action string the volume HUD listens on for stream level changes. */
        private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
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
