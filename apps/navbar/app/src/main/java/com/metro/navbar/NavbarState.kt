package com.metro.navbar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metro.system.MetroBroadcasts
import com.metro.system.MetroPreferences

class NavbarState(context: Context) {
  private val appContext = context.applicationContext
  private val preferences = MetroPreferences(appContext)

  var theme by mutableStateOf(NavbarThemeResolver.resolve(preferences))
    private set

  var visible by mutableStateOf(true)
    private set

  private val themeReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      if (intent?.action == MetroBroadcasts.ACTION_THEME_CHANGED) {
        refreshTheme()
      }
    }
  }

  fun refreshTheme() {
    theme = NavbarThemeResolver.resolve(preferences)
  }

  fun toggleVisibility() {
    visible = !visible
  }

  fun hide() {
    visible = false
  }

  fun show() {
    visible = true
  }

  fun registerReceivers(context: Context) {
    val filter = IntentFilter(MetroBroadcasts.ACTION_THEME_CHANGED)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      context.registerReceiver(themeReceiver, filter, Context.RECEIVER_EXPORTED)
    } else {
      @Suppress("UnspecifiedRegisterReceiverFlag")
      context.registerReceiver(themeReceiver, filter)
    }
  }

  fun unregisterReceivers(context: Context) {
    runCatching { context.unregisterReceiver(themeReceiver) }
  }
}
