package com.metro.lockscreen

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import com.metro.system.MetroAppBranding
import com.metro.ui.MetroAppSlotDefaults

/**
 * Resolved quick-status icon for [packageName]: suite / tile glyph id, else launcher bitmap.
 */
data class QuickStatusIcon(
    val glyphResId: Int? = null,
    val launcherBitmap: ImageBitmap? = null,
) {
    val hasIcon: Boolean get() = glyphResId != null || launcherBitmap != null
}

fun resolveQuickStatusIcon(
    context: Context,
    packageName: String,
    pixelSize: Int,
): QuickStatusIcon {
    val glyphResId = LockscreenQuickStatusLogic.notificationIconRes(packageName)
    if (glyphResId != null) {
        return QuickStatusIcon(glyphResId = glyphResId)
    }
    val bitmap = MetroAppBranding.loadAppIcon(context, packageName)
        ?.toBitmap(pixelSize, pixelSize)
        ?.asImageBitmap()
    return QuickStatusIcon(launcherBitmap = bitmap)
}

@Composable
fun rememberQuickStatusIcon(
    packageName: String?,
    iconSize: Dp = MetroAppSlotDefaults.Size * MetroAppSlotDefaults.GlyphScale,
): QuickStatusIcon? {
    if (packageName.isNullOrBlank()) return null
    val context = LocalContext.current
    val pixelSize = with(LocalDensity.current) { iconSize.roundToPx().coerceAtLeast(1) }
    return remember(packageName, pixelSize) {
        resolveQuickStatusIcon(context, packageName, pixelSize)
    }
}

@Composable
fun rememberQuickStatusIconPainter(icon: QuickStatusIcon?): Painter? {
    val bitmap = icon?.launcherBitmap ?: return null
    return remember(bitmap) { BitmapPainter(bitmap) }
}
