package com.metro.statusbar

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.graphics.Color as AndroidColor
import android.content.Intent
import android.graphics.Rect
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
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
import kotlin.math.ceil

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
     */
    private fun rehostOverlay() {
        val accessibilityHost = StatusBarAccessibilityService.getInstance()
        val hostContext: Context = accessibilityHost ?: this
        val windowType =
            if (accessibilityHost != null) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                applicationOverlayWindowType()
            }

        if (overlayView != null && currentWindowType == windowType) return
        removeOverlay()

        val barHeightDp = statusBarInsetDp()
        val horizontalPadding = statusBarHorizontalPaddingDp()
        val composeView = ComposeView(hostContext).apply {
            setBackgroundColor(AndroidColor.TRANSPARENT)
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
                        barHeightDp = barHeightDp,
                        startPaddingDp = horizontalPadding.start,
                        endPaddingDp = horizontalPadding.end,
                    )
                }
            }
        }

        val manager = hostContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        manager.addView(composeView, createLayoutParams(windowType))
        overlayView = composeView
        overlayManager = manager
        currentWindowType = windowType
    }

    private fun removeOverlay() {
        overlayView?.let { view -> runCatching { overlayManager?.removeView(view) } }
        overlayView = null
        overlayManager = null
        currentWindowType = null
    }

    private fun createLayoutParams(windowType: Int): WindowManager.LayoutParams =
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
    private fun statusBarInsetDp(): Int {
        val density = resources.displayMetrics.density
        val topPx: Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val wm = getSystemService(WINDOW_SERVICE) as WindowManager
                val insets = wm.currentWindowMetrics.windowInsets
                val statusBars = insets.getInsets(WindowInsets.Type.statusBars())
                val cutoutTop = insets.displayCutout?.safeInsetTop ?: 0
                maxOf(statusBars.top, cutoutTop)
            } else {
                val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
                if (resId > 0) resources.getDimensionPixelSize(resId) else (24 * density).toInt()
            }
        val dp = (topPx / density).toInt()
        return maxOf(dp, TraySpec.TRAY_HEIGHT_DP)
    }

    /**
     * Leaves space for cutouts and Android privacy indicators so system dots never overlap the
     * Metro tray clock row.
     */
    private fun statusBarHorizontalPaddingDp(): HorizontalPaddingDp {
        val density = resources.displayMetrics.density
        var startPx = TraySpec.START_PADDING_DP * density
        var endPx = TraySpec.END_PADDING_DP * density

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            val metrics = wm.currentWindowMetrics
            val insets = metrics.windowInsets
            val cutout = insets.displayCutout

            startPx = maxOf(startPx, cutout?.safeInsetLeft?.toFloat() ?: 0f)
            endPx = maxOf(endPx, cutout?.safeInsetRight?.toFloat() ?: 0f)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val bounds = insets.privacyIndicatorBounds
                if (bounds != null && !bounds.isEmpty) {
                    val gapPx = TraySpec.PRIVACY_INDICATOR_GAP_DP * density
                    val widthPx = metrics.bounds.width().toFloat()
                    if (bounds.centerX() >= widthPx / 2f) {
                        // Keep only a tiny trailing cushion next to the privacy dots instead of
                        // reserving the entire dots width on the tray's right edge.
                        endPx = maxOf(endPx, widthPx - bounds.right + gapPx)
                    } else {
                        startPx = maxOf(startPx, bounds.right + gapPx)
                    }
                }
            }
        }

        return HorizontalPaddingDp(
            start = ceil(startPx / density).toInt(),
            end = ceil(endPx / density).toInt(),
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
            instance?.let { svc -> svc.handler.post { svc.rehostOverlay() } }
        }

        /** Called when the accessibility service disconnects — fall back to the app overlay. */
        fun onAccessibilityServiceDisconnected() {
            instance?.let { svc -> svc.handler.post { svc.rehostOverlay() } }
        }

        fun start(context: Context) {
            val intent = Intent(context, StatusBarOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Forwards a [MetroStatusBar] contract request (received as a broadcast from another app)
         * to the running overlay service so the tray updates in place.
         */
        fun deliver(context: Context, action: String, source: Intent) {
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

private data class HorizontalPaddingDp(
    val start: Int,
    val end: Int,
)
