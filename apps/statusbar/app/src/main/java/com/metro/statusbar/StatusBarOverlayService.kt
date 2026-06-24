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
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import com.metro.statusbar.ui.StatusTray
import com.metro.ui.MetroTheme
import java.time.ZonedDateTime

class StatusBarOverlayService : Service(), LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private val trayState = TrayState(this)
    private val handler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            trayState.refreshClock()
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
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(NOTIFICATION_ID, buildNotification())
        showOverlay()
        trayState.registerReceivers(this)
        trayState.refreshTheme()
        trayState.refreshClock()
        scheduleNextClockTick()
        handler.post(autoCollapseRunnable)
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_REFRESH -> trayState.refreshTheme()
            ACTION_SET_PROGRESS -> trayState.setProgressVisible(intent.getBooleanExtra(EXTRA_PROGRESS, false))
            ACTION_SET_VISIBILITY -> {
                val modeName = intent.getStringExtra(EXTRA_VISIBILITY_MODE) ?: TrayVisibilityMode.Opaque.name
                trayState.applyVisibilityMode(TrayVisibilityMode.valueOf(modeName))
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(clockRunnable)
        handler.removeCallbacks(autoCollapseRunnable)
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        removeOverlay()
        trayState.unregisterReceivers(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOverlay() {
        if (overlayView != null) return

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@StatusBarOverlayService)
            setContent {
                MetroTheme(
                    darkTheme = trayState.theme.darkTheme,
                    accent = trayState.theme.accentColor,
                ) {
                    StatusTray(
                        snapshot = trayState.snapshot,
                        onTrayTap = { trayState.toggleExpanded() },
                    )
                }
            }
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP
        }

        windowManager.addView(composeView, layoutParams)
        overlayView = composeView
    }

    private fun removeOverlay() {
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
    }

    private fun scheduleNextClockTick() {
        val now = ZonedDateTime.now()
        val nextMinute = now.plusMinutes(1).withSecond(0).withNano(0)
        val delayMs = java.time.Duration.between(now, nextMinute).toMillis().coerceAtLeast(1000L)
        handler.removeCallbacks(clockRunnable)
        handler.postDelayed(clockRunnable, delayMs)
    }

    private fun overlayWindowType(): Int =
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
        private const val ACTION_REFRESH = "com.metro.statusbar.action.REFRESH"
        private const val ACTION_SET_PROGRESS = "com.metro.statusbar.action.SET_PROGRESS"
        private const val ACTION_SET_VISIBILITY = "com.metro.statusbar.action.SET_VISIBILITY"
        private const val EXTRA_PROGRESS = "progress"
        private const val EXTRA_VISIBILITY_MODE = "visibility_mode"

        fun start(context: Context) {
            val intent = Intent(context, StatusBarOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun requestRefresh(context: Context) {
            val intent = Intent(context, StatusBarOverlayService::class.java).apply {
                action = ACTION_REFRESH
            }
            context.startService(intent)
        }
    }
}
