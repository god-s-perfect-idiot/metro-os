package com.metro.system

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
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
 *
 * Adaptive icons are unwrapped to their **foreground** layer only. WP8.1 tiles and the app list
 * are square; Android's adaptive squircle/circle mask must not clip Metro glyphs.
 *
 * Foregrounds are authored for a 108dp viewport with a 72dp safe zone. Drawing the full layer
 * into a square leaves third-party glyphs looking too small, so the safe zone is scaled to fill.
 */
object MetroAppBranding {

    /** Adaptive icon viewport (108) / safe zone (72). */
    internal const val ADAPTIVE_SAFE_ZONE_SCALE = 108f / 72f

    fun loadAppIcon(context: Context, packageName: String): Drawable? =
        loadAppIconAsset(context, packageName).drawable

    fun loadAppIconAsset(context: Context, packageName: String): AppIconAsset {
        val raw = try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationIcon(appInfo)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
        val backgroundColor = resolveIconBackgroundColor(context, packageName, raw)
        return AppIconAsset(drawable = metroGlyphDrawable(raw), backgroundColor = backgroundColor)
    }

    /**
     * Prefer the adaptive foreground (unmasked) so Start/app-list surfaces stay square.
     * Scales the safe zone to fill the drawable bounds.
     */
    fun metroGlyphDrawable(drawable: Drawable?): Drawable? {
        if (drawable is AdaptiveIconDrawable) {
            val foreground = drawable.foreground ?: return null
            val mutated = foreground.constantState?.newDrawable()?.mutate() ?: foreground.mutate()
            return AdaptiveSafeZoneDrawable(mutated, ADAPTIVE_SAFE_ZONE_SCALE)
        }
        return drawable
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

/**
 * Draws [source] scaled about its center so adaptive safe-zone padding is cropped away.
 */
internal class AdaptiveSafeZoneDrawable(
    private val source: Drawable,
    private val scale: Float,
) : Drawable(), Drawable.Callback {

    init {
        source.callback = this
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        val save = canvas.save()
        canvas.clipRect(b)
        // Scale about center so the 72dp safe zone fills the square.
        canvas.scale(scale, scale, b.exactCenterX(), b.exactCenterY())
        source.bounds = b
        source.draw(canvas)
        canvas.restoreToCount(save)
    }

    override fun setAlpha(alpha: Int) {
        source.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        source.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = source.opacity.takeIf { it != PixelFormat.UNKNOWN }
        ?: PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth(): Int = source.intrinsicWidth

    override fun getIntrinsicHeight(): Int = source.intrinsicHeight

    override fun invalidateDrawable(who: Drawable) {
        invalidateSelf()
    }

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
        scheduleSelf(what, `when`)
    }

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
        unscheduleSelf(what)
    }
}
