package com.metro.notifications

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
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
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
import com.metro.notifications.ui.ToastBanner
import com.metro.system.MetroPreferences
import com.metro.system.MetroStatusBar
import com.metro.ui.MetroTheme
import kotlin.math.roundToInt

/**
 * Foreground service hosting WP8.1 toast banners.
 *
 * The WindowManager view exists only while a toast is visible — same attach/detach pattern as
 * the volume HUD. The window is pinned below the status-bar / cutout inset so the accent bar
 * does not draw under a notch. No Action Center and no status-tray chrome.
 */
class NotificationsOverlayService :
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
    /** Status-bar / cutout inset; the toast window is pinned just below this. */
    private var topInsetPx: Int = 0

    private val handler = Handler(Looper.getMainLooper())
    private val toastedKeys = mutableSetOf<String>()
    private val rehostLock = Any()

    var toast by mutableStateOf<ToastSnapshot?>(null)
        private set
    var toastExiting by mutableStateOf(false)
        private set
    var darkTheme by mutableStateOf(true)
        private set
    var accent by mutableStateOf(Color(0xFF1BA1E2))
        private set
    private var acknowledgeOnExit = false

    private val toastTimeout = Runnable {
        dismissToast(acknowledged = false)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        startForeground(NOTIFICATION_ID, buildNotification())
        refreshTheme()
        HeadsUpController.disableStockHeadsUp(this)
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_REFRESH -> handler.post { refreshTheme() }
            ACTION_SHOW_TEST -> handler.post { showTestToast() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        handler.removeCallbacks(toastTimeout)
        handler.removeCallbacksAndMessages(null)
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        removeOverlay()
        HeadsUpController.restoreStockHeadsUp(this)
        viewModelStore.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun refreshTheme() {
        val prefs = MetroPreferences(this)
        prefs.pullThemeFromProvider()
        darkTheme = prefs.isDark
        accent = prefs.accentColor
    }

    private fun showToast(snapshot: ToastSnapshot) {
        toastExiting = false
        acknowledgeOnExit = false
        toast = snapshot
        handler.removeCallbacks(toastTimeout)
        handler.postDelayed(toastTimeout, NotificationsPreferences(this).toastDurationMs)
        ensureOverlayShowing()
    }

    private fun showTestToast() {
        showToast(
            ToastSnapshot(
                key = "test:${System.currentTimeMillis()}",
                packageName = packageName,
                title = getString(R.string.test_toast_title),
                body = getString(R.string.test_toast_body),
            ),
        )
    }

    private fun dismissToast(acknowledged: Boolean) {
        handler.removeCallbacks(toastTimeout)
        if (toast == null || toastExiting) return
        acknowledgeOnExit = acknowledged
        toastExiting = true
    }

    private fun finishExit() {
        if (!toastExiting) return
        val current = toast
        val acknowledged = acknowledgeOnExit
        toastExiting = false
        acknowledgeOnExit = false
        toast = null
        if (acknowledged && current != null) {
            ActionNotificationListenerService.openNotification(current.key)
        }
        removeOverlay()
    }

    private fun ensureOverlayShowing() {
        synchronized(rehostLock) {
            if (overlayView != null) return
            attachOverlay()
        }
    }

    private fun rehostOverlay() {
        synchronized(rehostLock) {
            val wasVisible = toast != null
            removeOverlayLocked()
            if (wasVisible) {
                attachOverlay()
            }
        }
    }

    private fun attachOverlay() {
        val accessibilityHost = NotificationsAccessibilityService.getInstance()
        val host: Context = accessibilityHost ?: this
        val windowType =
            if (accessibilityHost != null) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                applicationOverlayWindowType()
            }
        topInsetPx = statusBarInsetPx()
        val composeView = ComposeView(host).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            clipChildren = false
            clipToPadding = false
            suppressSystemBarInsets()
            setViewTreeLifecycleOwner(this@NotificationsOverlayService)
            setViewTreeSavedStateRegistryOwner(this@NotificationsOverlayService)
            setViewTreeViewModelStoreOwner(this@NotificationsOverlayService)
            setContent {
                MetroTheme(darkTheme = darkTheme, accent = accent) {
                    val current = toast
                    if (current != null) {
                        ToastBanner(
                            toast = current,
                            accent = accent,
                            exiting = toastExiting,
                            onTap = { dismissToast(acknowledged = true) },
                            onSwipeDismiss = { dismissToast(acknowledged = false) },
                            onExitFinished = { handler.post { finishExit() } },
                        )
                    }
                }
            }
        }
        val manager = host.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        runCatching {
            manager.addView(
                composeView,
                createLayoutParams(
                    windowType,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    topInsetPx,
                ),
            )
            overlayView = composeView
            overlayManager = manager
            currentWindowType = windowType
        }.onFailure { Log.e(TAG, "Failed to attach toast overlay", it) }
    }

    private fun removeOverlay() {
        synchronized(rehostLock) {
            removeOverlayLocked()
        }
    }

    private fun removeOverlayLocked() {
        overlayView?.let { view -> runCatching { overlayManager?.removeView(view) } }
        overlayView = null
        overlayManager = null
        currentWindowType = null
        topInsetPx = 0
    }

    private fun createLayoutParams(
        windowType: Int,
        heightPx: Int,
        yPx: Int,
    ): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            heightPx,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = yPx
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
     * Top of the Metro tray / system status-bar region, including notch or hole-punch.
     * Matches the volume HUD inset so the accent bar starts below the tray, not under the cutout.
     */
    private fun statusBarInsetPx(): Int {
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
                if (resId > 0) {
                    resources.getDimensionPixelSize(resId)
                } else {
                    (24 * density).toInt()
                }
            }
        val minTrayPx = (MetroStatusBar.HEIGHT_DP * density).roundToInt()
        return maxOf(topPx, minTrayPx)
    }

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

    private fun applicationOverlayWindowType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun buildNotification(): Notification {
        val channelId = "metro_notifications_overlay"
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
        private const val TAG = "NotificationsOverlay"
        private const val NOTIFICATION_ID = 1008
        private const val ACTION_REFRESH = "com.metro.notifications.action.REFRESH"
        private const val ACTION_SHOW_TEST = "com.metro.notifications.action.SHOW_TEST"

        @Volatile
        private var instance: NotificationsOverlayService? = null

        fun isRunning(): Boolean = instance != null

        fun onAccessibilityServiceConnected() {
            instance?.handler?.post { instance?.rehostOverlay() }
        }

        fun onAccessibilityServiceDisconnected() {
            instance?.handler?.post { instance?.rehostOverlay() }
        }

        fun start(context: Context) {
            val intent = Intent(context, NotificationsOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, NotificationsOverlayService::class.java))
        }

        fun applyMasterToggle(context: Context, enabled: Boolean) {
            val prefs = NotificationsPreferences(context)
            prefs.enabled = enabled
            if (!enabled) {
                ActionNotificationListenerService.clearHeadsUpSuppression()
                HeadsUpController.restoreStockHeadsUp(context)
                stop(context)
                return
            }
            // Suppress stock peeks as soon as Metro owns toasts — do not wait for FGS attach.
            HeadsUpController.disableStockHeadsUp(context)
            ActionNotificationListenerService.requestHeadsUpSuppression()
            if (!Settings.canDrawOverlays(context) || !NotificationsAccessibilityService.isEnabled()) {
                return
            }
            start(context)
        }

        fun requestRefresh(context: Context) {
            if (!NotificationsPreferences(context).enabled) return
            val intent = Intent(context, NotificationsOverlayService::class.java).apply {
                action = ACTION_REFRESH
            }
            context.startService(intent)
        }

        /** Raises a sample toast from the setup screen. Requires the overlay FGS to be allowed. */
        fun showTestToast(context: Context) {
            if (!NotificationsPreferences(context).enabled) return
            if (!Settings.canDrawOverlays(context) || !NotificationsAccessibilityService.isEnabled()) {
                return
            }
            val intent = Intent(context, NotificationsOverlayService::class.java).apply {
                action = ACTION_SHOW_TEST
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && instance == null) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun considerToast(
            packageName: String,
            key: String,
            flags: Int,
            importance: Int,
            matchesInterruptionFilter: Boolean,
            screenInteractive: Boolean,
            isGroupSummary: Boolean,
            isActiveCall: Boolean,
            onlyAlertOnce: Boolean,
            title: String,
            body: String?,
        ) {
            val svc = instance ?: return
            svc.handler.post {
                val already = key in svc.toastedKeys
                val show = ToastDecision.shouldShow(
                    packageName = packageName,
                    flags = flags,
                    importance = importance,
                    matchesInterruptionFilter = matchesInterruptionFilter,
                    screenInteractive = screenInteractive,
                    isGroupSummary = isGroupSummary,
                    isActiveCall = isActiveCall,
                    onlyAlertOnceAlreadyShown = already && onlyAlertOnce,
                )
                if (!show) return@post
                svc.toastedKeys += key
                val resolvedTitle = title.ifEmpty { body.orEmpty() }.ifEmpty { packageName }
                svc.showToast(
                    ToastSnapshot(
                        key = key,
                        packageName = packageName,
                        title = resolvedTitle,
                        body = if (title.isEmpty()) null else body,
                    ),
                )
            }
        }

        fun onNotificationRemoved(key: String) {
            instance?.handler?.post {
                instance?.toastedKeys?.remove(key)
                if (instance?.toast?.key == key) {
                    instance?.dismissToast(acknowledged = false)
                }
            }
        }
    }
}
