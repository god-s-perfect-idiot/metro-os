package com.metro.system

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color

/**
 * Resolved launcher icon drawable and square tile background for list/grid surfaces.
 */
data class AppIconAsset(
    val drawable: Drawable?,
    val backgroundColor: Color,
)

/**
 * Resolves installed-app icon and brand color for launcher surfaces.
 *
 * Color precedence: live tile contract → `ic_launcher_background` resource → default accent.
 */
object MetroAppBranding {

    fun loadAppIcon(context: Context, packageName: String): Drawable? =
        loadAppIconAsset(context, packageName).drawable

    fun loadAppIconAsset(context: Context, packageName: String): AppIconAsset {
        val drawable = try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationIcon(appInfo)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
        val backgroundColor = resolveIconBackgroundColor(context, packageName, drawable)
        return AppIconAsset(drawable = drawable, backgroundColor = backgroundColor)
    }

    fun resolveIconBackgroundColor(
        context: Context,
        packageName: String,
        drawable: Drawable?,
    ): Color {
        if (drawable is AdaptiveIconDrawable) {
            when (val background = drawable.background) {
                is ColorDrawable -> return Color(background.color)
                null -> Unit
                else -> sampleDrawableColor(background)?.let { return it }
            }
        }

        loadLauncherBackgroundColor(context, packageName)?.let { return it }
        MetroAppRegistry.brandHex(packageName)?.let { return MetroPreferences.parseAccentHex(it) }

        return MetroPreferences.parseAccentHex(MetroPreferences.DEFAULT_ACCENT_HEX)
    }

    private fun sampleDrawableColor(drawable: Drawable): Color? {
        val size = 64
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        val center = bitmap.getPixel(size / 2, size / 2)
        bitmap.recycle()
        val color = Color(center)
        return color.takeIf { it.alpha > 0f }
    }

    fun resolvePrimaryColor(context: Context, packageName: String): Color {
        MetroTileContract.readTile(context.contentResolver, packageName)
            ?.backgroundColorHex
            ?.let { return MetroPreferences.parseAccentHex(it) }

        loadLauncherBackgroundColor(context, packageName)?.let { return it }

        MetroAppRegistry.brandHex(packageName)?.let { return MetroPreferences.parseAccentHex(it) }

        return MetroPreferences.parseAccentHex(MetroPreferences.DEFAULT_ACCENT_HEX)
    }

    private fun loadLauncherBackgroundColor(context: Context, packageName: String): Color? {
        return try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val resources = packageManager.getResourcesForApplication(appInfo)
            val colorId = resources.getIdentifier("ic_launcher_background", "color", packageName)
            if (colorId == 0) null else {
                @Suppress("DEPRECATION")
                Color(resources.getColor(colorId))
            }
        } catch (_: Exception) {
            null
        }
    }
}
