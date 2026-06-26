package com.metro.navbar

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.content.Intent
import com.metro.system.MetroBroadcasts
import com.metro.system.MetroPreferences
import com.metro.ui.MetroTheme

/**
 * Hosts the Metro navigation bar overlay window.
 *
 * [WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY] is intentionally below the system
 * navigation bar. When the accessibility service is connected we use
 * [WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY] so the Metro bar covers Android nav
 * chrome.
 */
object NavbarOverlayController {
  private var overlayView: ComposeView? = null
  private var windowManager: WindowManager? = null
  private var navbarState: NavbarState? = null
  private var overlayLifecycle: OverlayLifecycle? = null
  private var currentWindowType: Int? = null
  private var receiversRegistered = false
  private var appContext: Context? = null

  var isActive: Boolean = false
    private set

  fun activate(context: Context) {
    isActive = true
    appContext = context.applicationContext
    val host = NavbarAccessibilityService.getInstance()
    if (host != null) {
      show(host, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
    } else {
      show(context.applicationContext, applicationOverlayWindowType())
    }
    publishEnabledState(context.applicationContext, enabled = true)
  }

  fun deactivate() {
    isActive = false
    val context = appContext
    if (receiversRegistered && context != null) {
      navbarState?.unregisterReceivers(context)
    }
    hide()
    navbarState = null
    receiversRegistered = false
    context?.let { publishEnabledState(it, enabled = false) }
    appContext = null
  }

  /**
   * Persists the current state and announces it to every app so they can reserve (or release)
   * the bottom space the overlay occupies. Also answers [MetroBroadcasts.ACTION_NAVBAR_QUERY].
   */
  fun publishEnabledState(context: Context, enabled: Boolean = isActive) {
    MetroPreferences(context.applicationContext).navBarEnabled = enabled
    val intent = Intent(MetroBroadcasts.ACTION_NAVBAR_CHANGED).apply {
      putExtra(MetroBroadcasts.EXTRA_NAVBAR_ENABLED, enabled)
    }
    context.applicationContext.sendBroadcast(intent)
  }

  fun onAccessibilityServiceConnected(service: NavbarAccessibilityService) {
    if (!isActive) return
    hide()
    show(service, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
  }

  fun onAccessibilityServiceDisconnected() {
    if (!isActive) return
    hide()
    appContext?.let { context ->
      show(context, applicationOverlayWindowType())
    }
  }

  fun refreshTheme() {
    navbarState?.refreshTheme()
  }

  fun toggleVisibility() {
    navbarState?.toggleVisibility()
  }

  private fun show(context: Context, windowType: Int) {
    if (overlayView != null && currentWindowType == windowType) return
    hide()

    val lifecycle = OverlayLifecycle().also { overlayLifecycle = it }
    lifecycle.start()

    val state = navbarState ?: NavbarState(context.applicationContext).also { created ->
      navbarState = created
      if (!receiversRegistered) {
        created.registerReceivers(context.applicationContext)
        receiversRegistered = true
      }
      created.refreshTheme()
    }

    val composeView = ComposeView(context).apply {
      setBackgroundColor(AndroidColor.TRANSPARENT)
      suppressSystemBarInsets()
      setViewTreeLifecycleOwner(lifecycle)
      setViewTreeSavedStateRegistryOwner(lifecycle)
      setContent {
        MetroTheme(
          darkTheme = state.theme.darkTheme,
          accent = state.theme.barColor,
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .wrapContentHeight()
              .then(
                if (state.visible) {
                  Modifier.pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                      if (dragAmount < -24f) {
                        state.hide()
                      }
                    }
                  }
                } else {
                  Modifier
                },
              ),
          ) {
            if (state.visible) {
              NavbarWithSystemChrome(
                theme = state.theme,
                onBack = { NavbarActions.dispatchBack(context) },
                onBackLongPress = { NavbarActions.dispatchRecents(context) },
                onStart = { NavbarActions.launchStart(context) },
                onSearch = { NavbarActions.launchGoogleSearch(context) },
                onSearchLongPress = { NavbarActions.launchGemini(context) },
              )
            } else {
              HiddenNavbarWithSystemChrome(
                theme = state.theme,
                onReveal = { state.show() },
              )
            }
          }
        }
      }
    }

    val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val layoutParams = createLayoutParams(windowType)
    manager.addView(composeView, layoutParams)
    windowManager = manager
    overlayView = composeView
    currentWindowType = windowType
  }

  private fun hide() {
    overlayView?.let { view ->
      runCatching { windowManager?.removeView(view) }
    }
    overlayView = null
    windowManager = null
    currentWindowType = null
    overlayLifecycle?.destroy()
    overlayLifecycle = null
  }

  private fun applicationOverlayWindowType(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
      @Suppress("DEPRECATION")
      WindowManager.LayoutParams.TYPE_PHONE
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
      gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
      applyBottomOverlayInsets()
    }

  private fun View.suppressSystemBarInsets() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      setOnApplyWindowInsetsListener { _, _ -> WindowInsets.CONSUMED }
    } else {
      @Suppress("DEPRECATION")
      systemUiVisibility = (
        systemUiVisibility
          or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
          or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        )
    }
  }

  private fun WindowManager.LayoutParams.applyBottomOverlayInsets() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      @Suppress("NewApi")
      fitInsetsTypes = 0
      @Suppress("NewApi")
      fitInsetsSides = 0
    }
  }

  private class OverlayLifecycle : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    init {
      savedStateRegistryController.performRestore(null)
      lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override val lifecycle: Lifecycle
      get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
      get() = savedStateRegistryController.savedStateRegistry

    fun start() {
      lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    fun destroy() {
      lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
  }
}
