package com.metro.navbar

import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings

/**
 * Detects the Android system navigation layout.
 *
 * The Metro WP8.1 soft-key overlay only works correctly with classic 3-button
 * navigation. Gesture / edge-to-edge layouts leave apps with conflicting insets
 * and cut-off content, so the overlay must stay disabled in those modes.
 */
object SystemNavigationMode {
  /** Classic Back / Home / Recents buttons. */
  const val THREE_BUTTON = 0

  /** Two-button layout (rare). */
  const val TWO_BUTTON = 1

  /** Full gesture / edge-to-edge navigation. */
  const val GESTURE = 2

  private const val SETTINGS_KEY = "navigation_mode"
  private const val RESOURCE_NAME = "config_navBarInteractionMode"

  /**
   * Returns [THREE_BUTTON], [TWO_BUTTON], or [GESTURE].
   *
   * Prefers `Settings.Secure.navigation_mode` (API 29+), then the AOSP
   * `config_navBarInteractionMode` resource. Unknown devices default to
   * [THREE_BUTTON] so pre-gesture phones still work.
   */
  fun mode(context: Context): Int {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val fromSettings = Settings.Secure.getInt(
        context.contentResolver,
        SETTINGS_KEY,
        -1,
      )
      if (fromSettings in THREE_BUTTON..GESTURE) return fromSettings
    }

    val resId = context.resources.getIdentifier(RESOURCE_NAME, "integer", "android")
    if (resId > 0) {
      return runCatching { context.resources.getInteger(resId) }
        .getOrDefault(THREE_BUTTON)
        .coerceIn(THREE_BUTTON, GESTURE)
    }

    return THREE_BUTTON
  }

  fun isThreeButton(context: Context): Boolean = mode(context) == THREE_BUTTON

  fun settingsUri(): Uri = Settings.Secure.getUriFor(SETTINGS_KEY)

  /**
   * Watches for system navigation-mode changes. Callers must unregister the
   * returned [ContentObserver] via [Context.getContentResolver].
   */
  fun registerObserver(context: Context, onChanged: () -> Unit): ContentObserver {
    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
      override fun onChange(selfChange: Boolean) = onChanged()
      override fun onChange(selfChange: Boolean, uri: Uri?) = onChanged()
    }
    context.contentResolver.registerContentObserver(settingsUri(), false, observer)
    return observer
  }

  /** Best-effort deep link into system navigation-mode settings. */
  fun openSystemNavigationSettings(context: Context) {
    val candidates = listOf(
      Intent("com.android.settings.NAVIGATION_MODE_SETTINGS"),
      Intent(Settings.ACTION_SETTINGS),
    )
    for (intent in candidates) {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
        return
      }
    }
  }
}
