package com.metro.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.metro.system.MetroNavBar
import com.metro.system.MetroStatusBar

/**
 * While [active], hides the Metro system tray, Metro soft keys, and Android system bars —
 * WP8.1 fullscreen chrome (photo viewer, in-call, immersive media). Restores both on dispose /
 * when [active] becomes false.
 *
 * Talks to the shell via [MetroStatusBar.requestFullscreen] and [MetroNavBar.requestFullscreen];
 * never import the statusbar or navbar apps.
 */
@Composable
fun MetroStatusBarFullscreenEffect(active: Boolean) {
    val context = LocalContext.current
    val view = LocalView.current

    DisposableEffect(active, context, view) {
        if (!active) {
            return@DisposableEffect onDispose { }
        }

        MetroStatusBar.requestFullscreen(context, fullscreen = true)
        MetroNavBar.requestFullscreen(context, fullscreen = true)
        val controller = insetsController(context, view)
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        onDispose {
            MetroStatusBar.requestFullscreen(context, fullscreen = false)
            MetroNavBar.requestFullscreen(context, fullscreen = false)
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private fun insetsController(context: Context, view: View): WindowInsetsControllerCompat? {
    val activity = context.findFullscreenActivity() ?: return null
    return WindowCompat.getInsetsController(activity.window, view)
}

private tailrec fun Context.findFullscreenActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findFullscreenActivity()
    else -> null
}
