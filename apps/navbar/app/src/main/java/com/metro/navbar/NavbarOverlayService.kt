package com.metro.navbar

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Foreground service that keeps the Metro navigation bar overlay alive.
 * The actual window is hosted by [NavbarOverlayController], preferring
 * [NavbarAccessibilityService] so the bar can cover the system navigation bar.
 */
class NavbarOverlayService : Service() {

  override fun onCreate() {
    super.onCreate()
    startForeground(NOTIFICATION_ID, buildNotification())
    NavbarOverlayController.activate(this)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_REFRESH -> NavbarOverlayController.refreshTheme()
      ACTION_TOGGLE_VISIBILITY -> NavbarOverlayController.toggleVisibility()
    }
    return START_STICKY
  }

  override fun onDestroy() {
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

    fun start(context: Context) {
      val intent = Intent(context, NavbarOverlayService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun requestRefresh(context: Context) {
      val intent = Intent(context, NavbarOverlayService::class.java).apply {
        action = ACTION_REFRESH
      }
      context.startService(intent)
    }
  }
}
