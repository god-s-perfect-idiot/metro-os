package com.metro.navbar

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Foreground service that keeps the Metro navigation bar overlay alive.
 * The actual window is hosted by [NavbarOverlayController], preferring
 * [NavbarAccessibilityService] so the bar can cover the system navigation bar.
 *
 * Activation is gated on Android 3-button navigation — gesture / edge-to-edge
 * layouts leave Metro apps with conflicting insets and cut-off content.
 */
class NavbarOverlayService : Service() {

  private var navigationModeObserver: ContentObserver? = null

  override fun onCreate() {
    super.onCreate()
    startForeground(NOTIFICATION_ID, buildNotification())
    if (!SystemNavigationMode.isThreeButton(this)) {
      NavbarOverlayController.publishEnabledState(this, enabled = false)
      stopSelf()
      return
    }
    NavbarOverlayController.activate(this)
    navigationModeObserver = SystemNavigationMode.registerObserver(this) {
      if (!SystemNavigationMode.isThreeButton(this)) {
        stopSelf()
      }
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (!SystemNavigationMode.isThreeButton(this)) {
      NavbarOverlayController.publishEnabledState(this, enabled = false)
      stopSelf()
      return START_NOT_STICKY
    }
    when (intent?.action) {
      ACTION_REFRESH -> NavbarOverlayController.refreshTheme()
      ACTION_TOGGLE_VISIBILITY -> NavbarOverlayController.toggleVisibility()
    }
    return START_STICKY
  }

  override fun onDestroy() {
    navigationModeObserver?.let { observer ->
      runCatching { contentResolver.unregisterContentObserver(observer) }
    }
    navigationModeObserver = null
    NavbarOverlayController.deactivate()
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun buildNotification(): Notification {
    val channelId = "metro_navbar_overlay"
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
    private const val NOTIFICATION_ID = 1001
    private const val ACTION_REFRESH = "com.metro.navbar.action.REFRESH"
    private const val ACTION_TOGGLE_VISIBILITY = "com.metro.navbar.action.TOGGLE_VISIBILITY"

    /**
     * Starts the overlay if the device is on 3-button navigation.
     * @return false when gesture / two-button mode blocks activation
     */
    fun start(context: Context): Boolean {
      if (!SystemNavigationMode.isThreeButton(context)) {
        NavbarOverlayController.publishEnabledState(context, enabled = false)
        return false
      }
      val intent = Intent(context, NavbarOverlayService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
      return true
    }

    fun stop(context: Context) {
      context.stopService(Intent(context, NavbarOverlayService::class.java))
    }

    fun requestRefresh(context: Context) {
      if (!SystemNavigationMode.isThreeButton(context)) return
      val intent = Intent(context, NavbarOverlayService::class.java).apply {
        action = ACTION_REFRESH
      }
      context.startService(intent)
    }
  }
}
