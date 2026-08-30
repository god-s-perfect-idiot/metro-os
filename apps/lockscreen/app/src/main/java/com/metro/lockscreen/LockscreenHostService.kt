package com.metro.lockscreen

import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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
import com.metro.system.MetroStatusBar
import com.metro.ui.MetroSystemTheme
/**
 * Foreground host for the Metro lock surface.
 *
 * **Render path:** [TYPE_ACCESSIBILITY_OVERLAY] hosted by [LockscreenAccessibilityService].
 *
 * **Swipe-up path:** surface animates fully off-screen past threshold, then this service
 * hands off to SystemUI via [LockscreenBouncerActivity]. After a committed hand-off the
 * Metro fill stays suppressed until the next [Intent.ACTION_SCREEN_OFF] (sleep / lock).
 */
class LockscreenHostService :
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

    private val handler = Handler(Looper.getMainLooper())
    private val attachLock = Any()

    private var overlayView: ComposeView? = null
    private var overlayManager: WindowManager? = null
    private var receiverRegistered = false

    /**
     * After a committed swipe-up, do not re-attach the Metro fill until the next screen-off
     * (power button / sleep), regardless of biometric success or failure.
     */
    @Volatile
    private var handedOffUntilScreenOff = false

    private val presentRetries = longArrayOf(0L, 50L, 150L, 400L, 1000L)

    private val tickRunnable = object : Runnable {
        override fun run() {
            syncOverlayToKeyguard()
            handler.postDelayed(this, TICK_MS)
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    schedulePresentAttempts()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    // Next wake may show Metro again — clear hand-off from this lock session.
                    handedOffUntilScreenOff = false
                    handler.removeCallbacksAndMessages(PRESENT_TOKEN)
                    removeOverlay()
                    LockscreenBouncerActivity.finishIfShowing()
                }
                Intent.ACTION_USER_PRESENT -> {
                    removeOverlay()
                    LockscreenBouncerActivity.finishIfShowing()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        startForeground(NOTIFICATION_ID, buildNotification())
        registerScreenReceiver()
        handler.post(tickRunnable)
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        schedulePresentAttempts()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!LockscreenPreferences(this).enabled) {
            stopSelf()
            return START_NOT_STICKY
        }
        schedulePresentAttempts()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        unregisterScreenReceiver()
        removeOverlay()
        LockscreenBouncerActivity.finishIfShowing()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
        instance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun schedulePresentAttempts() {
        handler.removeCallbacksAndMessages(PRESENT_TOKEN)
        for (delay in presentRetries) {
            handler.postAtTime(
                { syncOverlayToKeyguard() },
                PRESENT_TOKEN,
                SystemClock.uptimeMillis() + delay,
            )
        }
    }

    private fun syncOverlayToKeyguard() {
        if (!LockscreenPreferences(this).enabled) {
            removeOverlay()
            return
        }
        if (LockscreenBouncerActivity.isShowing()) {
            removeOverlay()
            return
        }

        val locked = LockscreenKeyguard.isLocked(this)
        val displayAwake = LockscreenKeyguard.isDisplayAwake(this)
        if (LockscreenLogic.shouldPresentLock(
                enabled = true,
                keyguardLocked = locked,
                displayAwake = displayAwake,
                handedOff = handedOffUntilScreenOff,
            )
        ) {
            ensureOverlayShowing()
        } else {
            removeOverlay()
        }
    }

    private fun ensureOverlayShowing() {
        synchronized(attachLock) {
            if (overlayView != null) return
            attachOverlayLocked()
        }
    }

    private fun removeOverlay() {
        synchronized(attachLock) {
            removeOverlayLocked()
        }
    }

    private fun attachOverlayLocked() {
        val accessibilityHost = LockscreenAccessibilityService.getInstance()
        if (accessibilityHost == null) {
            Log.w(TAG, "Accessibility service not connected — cannot draw over keyguard")
            return
        }

        val host: Context = accessibilityHost
        val windowType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        val topInsetPx = statusBarInsetPx()
        val composeView = ComposeView(host).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            suppressSystemBarInsets()
            setViewTreeLifecycleOwner(this@LockscreenHostService)
            setViewTreeSavedStateRegistryOwner(this@LockscreenHostService)
            setViewTreeViewModelStoreOwner(this@LockscreenHostService)
            setContent {
                MetroSystemTheme {
                    LockscreenSurface(
                        onUnlockCommitted = {
                            handler.post { commitSwipeUnlock() }
                        },
                        topInsetPx = topInsetPx,
                    )
                }
            }
        }

        val manager = host.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try {
            manager.addView(composeView, createLayoutParams(windowType))
            overlayView = composeView
            overlayManager = manager
            // Lock draws its own transparent tray; hide the opaque system Metro tray.
            MetroStatusBar.requestFullscreen(this, fullscreen = true)
            Log.i(TAG, "Lock overlay attached")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to attach lock overlay", t)
            runCatching { manager.removeView(composeView) }
            overlayView = null
            overlayManager = null
        }
    }

    private fun removeOverlayLocked() {
        val view = overlayView
        val manager = overlayManager
        overlayView = null
        overlayManager = null
        if (view != null && manager != null) {
            runCatching { manager.removeView(view) }
                .onFailure { Log.w(TAG, "removeView failed", it) }
            MetroStatusBar.requestFullscreen(this, fullscreen = false)
        }
    }

    /**
     * Panel has finished sliding off-screen. Hand off to SystemUI and keep Metro suppressed
     * until the next screen-off — biometric fail/cancel must not bring the fill back.
     */
    private fun commitSwipeUnlock() {
        handedOffUntilScreenOff = true
        removeOverlay()
        launchBouncerActivity()
        handler.postDelayed({
            if (!LockscreenBouncerActivity.isShowing()) {
                Log.w(TAG, "Bouncer activity not up — gesture fallback into SystemUI")
                LockscreenAccessibilityService.getInstance()?.injectSwipeUpToBouncer()
                handler.postDelayed({ launchBouncerActivity() }, 350L)
            }
        }, 120L)
    }

    private fun launchBouncerActivity() {
        val intent = bouncerIntent()

        val a11y = LockscreenAccessibilityService.getInstance()
        if (a11y != null) {
            runCatching { a11y.startActivity(intent) }
                .onSuccess { Log.i(TAG, "bouncer started via accessibility") }
                .onFailure { Log.w(TAG, "bouncer a11y startActivity failed", it) }
        }

        runCatching {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pending = PendingIntent.getActivity(this, REQUEST_BOUNCER, intent, flags)
            pending.send(this, 0, null, null, null, null, activityStartOptions())
        }.onFailure { Log.w(TAG, "bouncer PendingIntent send failed", it) }

        runCatching { fireBouncerFullScreenIntent(intent) }
            .onFailure { Log.w(TAG, "bouncer FSI failed", it) }
    }

    private fun bouncerIntent(): Intent =
        Intent(this, LockscreenBouncerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }

    private fun fireBouncerFullScreenIntent(intent: Intent) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !areNotificationsEnabled()
        ) {
            Log.w(TAG, "notifications disabled — skipping FSI")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            !manager.canUseFullScreenIntent()
        ) {
            Log.w(TAG, "USE_FULL_SCREEN_INTENT not granted — skipping FSI")
            return
        }
        val channelId = "metro_lockscreen_bouncer"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    getString(R.string.unlock_notification_channel),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    setSound(null, null)
                    enableVibration(false)
                },
            )
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val fullScreen = PendingIntent.getActivity(this, REQUEST_BOUNCER_FSI, intent, flags)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.unlock_title))
            .setContentText(getString(R.string.unlock_description))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreen, true)
            .setAutoCancel(true)
            .setTimeoutAfter(2_500L)
            .build()
        manager.notify(NOTIFICATION_BOUNCER_ID, notification)
        handler.postDelayed({ manager.cancel(NOTIFICATION_BOUNCER_ID) }, 2_000L)
    }

    private fun areNotificationsEnabled(): Boolean {
        val manager = getSystemService(NotificationManager::class.java) ?: return false
        return manager.areNotificationsEnabled()
    }

    /** Options for PendingIntent.send — never for PendingIntent.getActivity. */
    private fun activityStartOptions(): android.os.Bundle? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        return ActivityOptions.makeBasic().apply {
            setPendingIntentBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
            )
        }.toBundle()
    }

    private fun createLayoutParams(
        windowType: Int,
        notTouchable: Boolean = false,
    ): WindowManager.LayoutParams {
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        if (notTouchable) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            flags,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                fitInsetsTypes = 0
                fitInsetsSides = 0
            }
        }
    }

    private fun View.suppressSystemBarInsets() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setOnApplyWindowInsetsListener { _, _ -> WindowInsets.CONSUMED }
        } else {
            @Suppress("DEPRECATION")
            systemUiVisibility = systemUiVisibility or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
    }

    /**
     * Height of the system status-bar / cutout band (same region as Metro status tray).
     * Floor at [MetroStatusBar.HEIGHT_DP] so icons always have a tray-sized band.
     */
    private fun statusBarInsetPx(): Int {
        val density = resources.displayMetrics.density
        val topPx: Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val wm = getSystemService(WINDOW_SERVICE) as WindowManager
                val insets = wm.currentWindowMetrics.windowInsets
                val statusBars = insets.getInsets(WindowInsets.Type.statusBars())
                val cutoutTop = insets.getInsets(WindowInsets.Type.displayCutout()).top
                maxOf(statusBars.top, cutoutTop)
            } else {
                val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
                if (resId > 0) {
                    resources.getDimensionPixelSize(resId)
                } else {
                    (24 * density).toInt()
                }
            }
        val minTrayPx = (MetroStatusBar.HEIGHT_DP * density).toInt()
        return maxOf(topPx, minTrayPx)
    }

    private fun registerScreenReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }

    private fun unregisterScreenReceiver() {
        if (!receiverRegistered) return
        runCatching { unregisterReceiver(screenReceiver) }
        receiverRegistered = false
    }

    private fun buildNotification(): Notification {
        val channelId = "metro_lockscreen_host"
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
        private const val TAG = "LockscreenHost"
        private const val NOTIFICATION_ID = 1006
        private const val NOTIFICATION_BOUNCER_ID = 1008
        private const val REQUEST_BOUNCER = 46
        private const val REQUEST_BOUNCER_FSI = 47
        private const val TICK_MS = 500L
        private val PRESENT_TOKEN = Any()

        @Volatile
        private var instance: LockscreenHostService? = null

        fun isRunning(): Boolean = instance != null

        fun notifyBouncerVisible() {
            instance?.let { svc ->
                svc.handedOffUntilScreenOff = true
                svc.handler.post {
                    svc.removeOverlay()
                    svc.getSystemService(NotificationManager::class.java)
                        ?.cancel(NOTIFICATION_BOUNCER_ID)
                }
            }
        }

        fun requestGestureBouncerFallback() {
            instance?.handler?.post {
                LockscreenAccessibilityService.getInstance()?.injectSwipeUpToBouncer()
            }
        }

        fun onAccessibilityServiceConnected() {
            instance?.let { svc ->
                svc.handler.post { svc.schedulePresentAttempts() }
            }
        }

        fun onAccessibilityServiceDisconnected() {
            instance?.let { svc -> svc.handler.post { svc.removeOverlay() } }
        }

        fun start(context: Context) {
            if (!LockscreenPreferences(context).enabled) return
            val intent = Intent(context, LockscreenHostService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LockscreenHostService::class.java))
        }

        fun applyMasterToggle(context: Context, enabled: Boolean) {
            LockscreenPreferences(context).enabled = enabled
            if (enabled) start(context) else stop(context)
        }

        /**
         * Tears down and re-attaches the lock overlay so background / accent changes apply
         * while the host is already running and the keyguard is showing.
         */
        fun requestRehost() {
            instance?.let { svc ->
                svc.handler.post {
                    svc.removeOverlay()
                    svc.syncOverlayToKeyguard()
                }
            }
        }
    }
}
