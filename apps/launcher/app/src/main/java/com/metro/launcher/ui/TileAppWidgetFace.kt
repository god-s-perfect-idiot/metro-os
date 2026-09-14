package com.metro.launcher.ui

import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.metro.launcher.data.PinnedTileEntry
import com.metro.launcher.data.TileAppWidgetController
import com.metro.launcher.data.hasActiveCustomWidget

val LocalTileAppWidgetController = staticCompositionLocalOf<TileAppWidgetController?> { null }

/**
 * Hosts an Android App Widget inside a medium/wide Start tile when customize → widget is on.
 * Host chrome stays transparent so the Metro tile fill (accent or Start wallpaper window) shows.
 */
@Composable
fun TileAppWidgetFace(
    entry: PinnedTileEntry,
    modifier: Modifier = Modifier,
) {
    val controller = LocalTileAppWidgetController.current
    if (controller == null || !entry.hasActiveCustomWidget()) return
    val info = remember(entry.widgetProvider, entry.appWidgetId) {
        controller.providerInfo(entry.widgetProvider)
    } ?: return
    val hostView = remember(entry.appWidgetId, info.provider) {
        controller.createHostView(entry.appWidgetId, info).also { view ->
            view.setBackgroundColor(AndroidColor.TRANSPARENT)
            view.setPadding(0, 0, 0, 0)
        }
    }
    DisposableEffect(hostView) {
        onDispose {
            (hostView.parent as? ViewGroup)?.removeView(hostView)
        }
    }
    AndroidView(
        factory = { hostView },
        modifier = modifier.fillMaxSize(),
        update = { view ->
            view.setBackgroundColor(AndroidColor.TRANSPARENT)
            view.setAppWidget(entry.appWidgetId, info)
        },
    )
}
