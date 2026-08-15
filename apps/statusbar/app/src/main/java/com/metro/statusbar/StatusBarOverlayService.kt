package com.metro.statusbar

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.RoundedCorner
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.metro.statusbar.ui.StatusTray
import com.metro.system.MetroStatusBar
import com.metro.ui.MetroTheme
import java.time.ZonedDateTime

class StatusBarOverlayService :
    Service(),
    LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore = ViewModelStore()

    private var overlayView: ComposeView? = null
    private var overlayManager: WindowManager? = null
    private var currentWindowType: Int? = null
    // Lazily created so it is only built after the service's base context is attached (onCreate),
    // never in the constructor where `this` is not yet a usable Context.
    private val trayState by lazy { TrayState(this) }
    private val handler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            trayState.refreshClock()
            trayState.refreshDataConnectionLabel()
            scheduleNextClockTick()
        }
    }
    private val autoCollapseRunnable = object : Runnable {
        override fun run() {
            trayState.tickAutoCollapse()
            handler.postDelayed(this, 500L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Must run while the lifecycle is still INITIALIZED, before moving to CREATED.
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        startForeground(NOTIFICATION_ID, buildNotification())
        rehostOverlay()
        trayState.registerReceivers(this)
        trayState.refreshTheme()
        trayState.refreshClock()
        trayState.refreshBattery()
        trayState.refreshDataConnectionLabel()
        scheduleNextClockTick()
        handler.post(autoCollapseRunnable)
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            MetroStatusBar.ACTION_REFRESH -> trayState.refreshTheme()
            MetroStatusBar.ACTION_EXPAND -> trayState.expand()
            MetroStatusBar.ACTION_SET_PROGRESS ->
                trayState.setProgressVisible(intent.getBooleanExtra(MetroStatusBar.EXTRA_PROGRESS, false))
            MetroStatusBar.ACTION_SET_VISIBILITY ->
                trayState.applyVisibilityMode(
                    TrayVisibilityMode.fromContract(intent.getStringExtra(MetroStatusBar.EXTRA_VISIBILITY_MODE)),
                )
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        handler.removeCallbacks(clockRunnable)
        handler.removeCallbacks(autoCollapseRunnable)
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        removeOverlay()
        trayState.unregisterReceivers(this)
        viewModelStore.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * (Re)creates the overlay window, preferring the accessibility-hosted
     * [WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY] so the tray is drawn *above* the
     * system status bar. Falls back to a plain app overlay (drawn below the system bar) when the
     * accessibility service is not connected.
     *
     * @param force when true, rebuild even if the window type is unchanged (e.g. notch padding).
     */
    private fun rehostOverlay(force: Boolean = false) {
        val accessibilityHost = StatusBarAccessibilityService.getInstance()
        val hostContext: Context = accessibilityHost ?: this
        val windowType =
            if (accessibilityHost != null) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                applicationOverlayWindowType()
            }

        if (!force && overlayView != null && currentWindowType == windowType) return
        removeOverlay()

        val manager = hostContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val barHeightDp = statusBarInsetDp(manager)
        val horizontalPadding = statusBarHorizontalPaddingDp(manager, barHeightDp)
        val composeView = ComposeView(hostContext).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            suppressSystemBarInsets()
            setViewTreeLifecycleOwner(this@StatusBarOverlayService)
            setViewTreeSavedStateRegistryOwner(this@StatusBarOverlayService)
            setViewTreeViewModelStoreOwner(this@StatusBarOverlayService)
            setContent {
                MetroTheme(
                    darkTheme = trayState.theme.darkTheme,
                    accent = trayState.theme.accentColor,
                ) {
                    StatusTray(
                        snapshot = trayState.snapshot,
                        onTrayTap = { trayState.toggleExpanded() },
                        onSwipeOpenNotifications = { openNotificationShadeFromTray() },
                        barHeightDp = barHeightDp,
                        leftPaddingDp = horizontalPadding.left,
                        rightPaddingDp = horizontalPadding.right,
                    )
                }
            }
        }

        manager.addView(composeView, createLayoutParams(windowType))
        overlayView = composeView
        overlayManager = manager
        currentWindowType = windowType
        applyOverlayVisibilityForShade()
    }

    /**
     * Swipe-down on the Metro tray opens the Android notification shade and hides the overlay so
     * it does not paint above SystemUI.
     */
    private fun openNotificationShadeFromTray() {
        trayState.applyNotificationShadeOpen(true)
        applyOverlayVisibilityForShade()
        val opened = StatusBarAccessibilityService.openNotificationShade()
        if (!opened) {
            trayState.applyNotificationShadeOpen(false)
            applyOverlayVisibilityForShade()
        }
    }

    private fun applyOverlayVisibilityForShade() {
        val hide = trayState.notificationShadeOpen
        overlayView?.visibility = if (hide) View.GONE else View.VISIBLE
    }

    private fun removeOverlay() {
        overlayView?.let { view -> runCatching { overlayManager?.removeView(view) } }
        overlayView = null
        overlayManager = null
        currentWindowType = null
    }

    private fun createLayoutParams(
        windowType: Int,
    ): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                fitInsetsTypes = 0
                fitInsetsSides = 0
            }
        }

    /**
     * Height the tray must occupy to fully cover the system status-bar region — including a notch
     * or hole-punch cutout — so no part of the Android bar peeks through below it. Never smaller
     * than the WP 32dp strip.
     */
    private fun statusBarInsetDp(wm: WindowManager): Int {
        val density = resources.displayMetrics.density
        val topPx: Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val insets = wm.currentWindowMetrics.windowInsets
                val statusBars = insets.getInsets(WindowInsets.Type.statusBars())
                val cutoutTop = insets.getInsets(WindowInsets.Type.displayCutout()).top
                maxOf(statusBars.top, cutoutTop)
            } else {
                val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
                if (resId > 0) resources.getDimensionPixelSize(resId) else (24 * density).toInt()
            }
        val dp = (topPx / density).toInt()
        return maxOf(dp, TraySpec.TRAY_HEIGHT_DP)
    }

    /**
     * Physical left/right padding so tray glyphs clear the configured notch side, cutouts,
     * waterfall edges, top rounded corners, and Android privacy indicators. Uses absolute edges
     * (not RTL start/end) because WP tray chrome stays clock-on-right regardless of locale.
     */
    private fun statusBarHorizontalPaddingDp(
        wm: WindowManager,
        barHeightDp: Int,
    ): HorizontalPaddingDp {
        val density = resources.displayMetrics.density
        var systemLeftPx = 0f
        var systemRightPx = 0f

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = wm.currentWindowMetrics
            val bounds = metrics.bounds
            val insets = metrics.windowInsets
            val cutoutInsets = insets.getInsets(WindowInsets.Type.displayCutout())
            systemLeftPx = maxOf(systemLeftPx, cutoutInsets.left.toFloat())
            systemRightPx = maxOf(systemRightPx, cutoutInsets.right.toFloat())

            val waterfall = insets.displayCutout?.waterfallInsets
            if (waterfall != null) {
                systemLeftPx = maxOf(systemLeftPx, waterfall.left.toFloat())
                systemRightPx = maxOf(systemRightPx, waterfall.right.toFloat())
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val barHeightPx = barHeightDp * density
                // Glyphs sit in the vertical middle of the tray; sample that band so corner
                // chords clear clock/battery without padding the full corner radius.
                val contentTopY = barHeightPx * 0.18f
                val contentBottomY = barHeightPx * 0.82f
                val topLeft = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
                val topRight = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)
                if (topLeft != null) {
                    systemLeftPx = maxOf(
                        systemLeftPx,
                        StatusBarSafeInsets.topRoundedCornerInsetPx(
                            radius = topLeft.radius,
                            centerX = topLeft.center.x,
                            centerY = topLeft.center.y,
                            contentTopY = contentTopY,
                            contentBottomY = contentBottomY,
                            windowLeft = bounds.left,
                            windowRight = bounds.right,
                            isLeftCorner = true,
                        ),
                    )
                }
                if (topRight != null) {
                    systemRightPx = maxOf(
                        systemRightPx,
                        StatusBarSafeInsets.topRoundedCornerInsetPx(
                            radius = topRight.radius,
                            centerX = topRight.center.x,
                            centerY = topRight.center.y,
                            contentTopY = contentTopY,
                            contentBottomY = contentBottomY,
                            windowLeft = bounds.left,
                            windowRight = bounds.right,
                            isLeftCorner = false,
                        ),
                    )
                }

                val privacyBounds = insets.privacyIndicatorBounds
                if (privacyBounds != null && !privacyBounds.isEmpty) {
                    val gapPx = TraySpec.PRIVACY_INDICATOR_GAP_DP * density
                    val widthPx = bounds.width().toFloat()
                    if (privacyBounds.centerX() >= widthPx / 2f) {
                        // Tiny trailing cushion next to the privacy dots — not the full dots width.
                        systemRightPx = maxOf(systemRightPx, widthPx - privacyBounds.right + gapPx)
                    } else {
                        systemLeftPx = maxOf(systemLeftPx, privacyBounds.right + gapPx)
                    }
                }
            }
        }

        return TraySpec.horizontalPaddingDp(
            notchPosition = StatusTrayPreferences(this).notchPosition,
            systemLeftDp = StatusBarSafeInsets.pxToDpCeil(systemLeftPx, density),
            systemRightDp = StatusBarSafeInsets.pxToDpCeil(systemRightPx, density),
        )
    }

    /** Draw to the very top edge instead of being pushed below the status bar inset. */
    private fun View.suppressSystemBarInsets() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setOnApplyWindowInsetsListener { _, _ -> WindowInsets.CONSUMED }
        } else {
            @Suppress("DEPRECATION")
            systemUiVisibility = systemUiVisibility or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
    }

    private fun scheduleNextClockTick() {
        val now = ZonedDateTime.now()
        val nextMinute = now.plusMinutes(1).withSecond(0).withNano(0)
        val delayMs = java.time.Duration.between(now, nextMinute).toMillis().coerceAtLeast(1000L)
        handler.removeCallbacks(clockRunnable)
        handler.postDelayed(clockRunnable, delayMs)
    }

    private fun applicationOverlayWindowType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun buildNotification(): Notification {
        val channelId = "metro_statusbar_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    getString(R.string.overlay_notification_title),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_body))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1002

        /** Running service instance, used by the accessibility service to trigger a rehost. */
        @Volatile
        private var instance: StatusBarOverlayService? = null

        /** Called when the accessibility service connects — re-host on the higher overlay layer. */
        fun onAccessibilityServiceConnected() {
            instance?.let { svc -> svc.handler.post { svc.rehostOverlay(force = true) } }
        }

        /** Called when the accessibility service disconnects — fall back to the app overlay. */
        fun onAccessibilityServiceDisconnected() {
            instance?.let { svc -> svc.handler.post { svc.rehostOverlay(force = true) } }
        }

        /**
         * Shows or hides the Metro tray when the Android notification shade opens or closes.
         * The accessibility overlay sits above SystemUI, so the tray must not draw while the
         * shade is expanded.
         */
        fun onNotificationShadeOpenChanged(open: Boolean) {
            instance?.let { svc ->
                svc.handler.post {
                    svc.trayState.applyNotificationShadeOpen(open)
                    svc.applyOverlayVisibilityForShade()
                }
            }
        }

        /**
         * Rebuilds the overlay in place (same window type) so inset / notch preference changes
         * take effect without restarting the service.
         */
        fun requestRehost() {
            instance?.let { svc -> svc.handler.post { svc.rehostOverlay(force = true) } }
        }

        fun isRunning(): Boolean = instance != null

        fun start(context: Context) {
            val intent = Intent(context, StatusBarOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, StatusBarOverlayService::class.java))
        }

        /**
         * Starts or stops the overlay to match [StatusTrayPreferences.enabled], when overlay +
         * accessibility permissions allow it.
         */
        fun applyMasterToggle(context: Context, enabled: Boolean) {
            val prefs = StatusTrayPreferences(context)
            prefs.enabled = enabled
            if (!enabled) {
                stop(context)
                return
            }
            if (!Settings.canDrawOverlays(context) || !StatusBarAccessibilityService.isEnabled()) {
                return
            }
            start(context)
        }

        /**
         * Forwards a [MetroStatusBar] contract request (received as a broadcast from another app)
         * to the running overlay service so the tray updates in place. No-op when the master
         * toggle is off so broadcasts cannot revive a hidden tray.
         */
        fun deliver(context: Context, action: String, source: Intent) {
            if (!StatusTrayPreferences(context).enabled) return
            val intent = Intent(context, StatusBarOverlayService::class.java).apply {
                this.action = action
                putExtras(source)
            }
            context.startService(intent)
        }

        fun requestRefresh(context: Context) {
            deliver(context, MetroStatusBar.ACTION_REFRESH, Intent())
        }
    }
}
