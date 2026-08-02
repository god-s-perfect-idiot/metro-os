package com.metro.volume

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
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
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
import com.metro.volume.ui.VolumeHud

/**
 * Foreground service hosting the WP8.1 volume HUD overlay.
 *
 * The WindowManager view exists only while the HUD is visible — an empty always-on overlay
 * previously could leave a stuck hit target / dead window after process issues. Volume keys
 * without a running instance are not consumed (see [VolumeAccessibilityService]).
 */
class VolumeOverlayService :
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
    private var hostContext: Context? = null

    private val controller by lazy { VolumeHudController(this) }
    private val handler = Handler(Looper.getMainLooper())
    private val rehostLock = Any()
    private val tickRunnable = object : Runnable {
        override fun run() {
            runCatching { controller.tickDismiss() }
            handler.postDelayed(this, 500L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        startForeground(NOTIFICATION_ID, buildNotification())
        controller.register()
        // No window until a volume press shows the HUD.
        handler.post(tickRunnable)
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_REFRESH -> runCatching { controller.refreshTheme() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        handler.removeCallbacks(tickRunnable)
        handler.removeCallbacksAndMessages(null)
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        removeOverlay()
        runCatching { controller.unregister() }
        viewModelStore.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureOverlayShowing() {
        synchronized(rehostLock) {
            if (overlayView != null) return
            attachOverlay()
        }
    }

    private fun removeOverlayIfHidden() {
        synchronized(rehostLock) {
            if (controller.visible) return
            removeOverlayLocked()
        }
    }

    private fun rehostOverlay() {
        synchronized(rehostLock) {
            val wasVisible = controller.visible
            removeOverlayLocked()
            if (wasVisible) {
                attachOverlay()
            }
        }
    }

    private fun attachOverlay() {
        val accessibilityHost = VolumeAccessibilityService.getInstance()
        val host: Context = accessibilityHost ?: this
        val windowType =
            if (accessibilityHost != null) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                applicationOverlayWindowType()
            }

        val topOffsetPx = statusBarInsetPx()
        val composeView = ComposeView(host).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            suppressSystemBarInsets()
            setViewTreeLifecycleOwner(this@VolumeOverlayService)
            setViewTreeSavedStateRegistryOwner(this@VolumeOverlayService)
            setViewTreeViewModelStoreOwner(this@VolumeOverlayService)
            setContent {
                val snapshot = controller.snapshot
                LaunchedEffect(snapshot.visible) {
                    if (!snapshot.visible) {
                        // Drop the window once the HUD auto-dismisses so nothing remains
                        // that can steal touches or leave a zombie overlay.
                        handler.post { removeOverlayIfHidden() }
                    }
                }
                VolumeHud(
                    snapshot = snapshot,
                    onToggleExpanded = { controller.toggleExpanded() },
                    onCollapse = { controller.collapse() },
                    onRingerLevel = { controller.updateRingerLevel(it) },
                    onMediaLevel = { controller.updateMediaLevel(it) },
                    onCallLevel = { controller.updateCallLevel(it) },
                    onToggleRingerMute = { controller.toggleRingerMute() },
                    onToggleMediaMute = { controller.toggleMediaMute() },
                    onToggleVibrate = { controller.toggleVibrate() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        val manager = host.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try {
            manager.addView(composeView, createLayoutParams(windowType, topOffsetPx))
            overlayView = composeView
            overlayManager = manager
            currentWindowType = windowType
            hostContext = host
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to attach volume overlay", t)
            runCatching { manager.removeView(composeView) }
            overlayView = null
            overlayManager = null
            currentWindowType = null
            hostContext = null
        }
    }

    private fun removeOverlay() {
        synchronized(rehostLock) {
            removeOverlayLocked()
        }
    }

    private fun removeOverlayLocked() {
        val view = overlayView
        val manager = overlayManager
        overlayView = null
        overlayManager = null
        currentWindowType = null
        hostContext = null
        if (view != null && manager != null) {
            runCatching { manager.removeView(view) }
                .onFailure { Log.w(TAG, "removeView failed", it) }
        }
    }

    private fun createLayoutParams(
        windowType: Int,
        topOffsetPx: Int,
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
            y = topOffsetPx
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                fitInsetsTypes = 0
                fitInsetsSides = 0
            }
        }

    private fun statusBarInsetPx(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
                (24 * resources.displayMetrics.density).toInt()
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
        val channelId = "metro_volume_overlay"
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
        private const val TAG = "VolumeOverlay"
        private const val NOTIFICATION_ID = 1004
        private const val ACTION_REFRESH = "com.metro.volume.action.REFRESH"

        @Volatile
        private var instance: VolumeOverlayService? = null

        fun isRunning(): Boolean = instance != null

        fun onAccessibilityServiceConnected() {
            instance?.let { svc -> svc.handler.post { svc.rehostOverlay() } }
        }

        fun onAccessibilityServiceDisconnected() {
            instance?.let { svc -> svc.handler.post { svc.rehostOverlay() } }
        }

        fun onVolumeKey(delta: Int): Boolean {
            val svc = instance ?: return false
            svc.handler.post {
                runCatching {
                    svc.controller.onVolumeKey(delta)
                    svc.ensureOverlayShowing()
                }.onFailure { Log.e(TAG, "onVolumeKey failed", it) }
            }
            return true
        }

        fun start(context: Context) {
            val intent = Intent(context, VolumeOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun requestRefresh(context: Context) {
            val intent = Intent(context, VolumeOverlayService::class.java).apply {
                action = ACTION_REFRESH
            }
            context.startService(intent)
        }
    }
}
