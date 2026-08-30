package com.metro.lockscreen

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import com.metro.ui.MetroColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Resolved lock fill: solid accent and/or a full-bleed bitmap.
 */
data class LockscreenFill(
    val mode: LockscreenBackgroundMode,
    val accentColor: Color,
    val bitmap: Bitmap?,
) {
    val usesPhoto: Boolean get() = bitmap != null

    /** Chrome / tray glyph color — white on photos (WP Bing lock), accent contrast otherwise. */
    val contentColor: Color
        get() = if (usesPhoto) Color.White else MetroColors.tileContentColor(accentColor)
}

object LockscreenBackgroundResolver {
    suspend fun resolve(context: Context, accentColor: Color): LockscreenFill {
        val prefs = LockscreenPreferences(context)
        val mode = prefs.backgroundMode
        val bitmap = when (mode) {
            LockscreenBackgroundMode.Accent -> null
            LockscreenBackgroundMode.Custom -> withContext(Dispatchers.IO) {
                LockscreenCustomBackground.decode(context)
            }
            LockscreenBackgroundMode.Bing -> withContext(Dispatchers.IO) {
                BingWallpaperCache.ensureFresh(context)
                BingWallpaperCache.decode(context)
            }
        }
        return LockscreenFill(
            mode = mode,
            accentColor = accentColor,
            bitmap = bitmap,
        )
    }
}
