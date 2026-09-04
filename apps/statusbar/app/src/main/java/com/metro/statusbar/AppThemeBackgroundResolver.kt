package com.metro.statusbar

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.TypedValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Reads the color an installed app publishes for top chrome / page fill.
 *
 * This is not a live pixel of the current Compose tree — only what the app's **application
 * theme** declares. Order matches what Android apps typically put behind the status bar:
 * opaque `statusBarColor` → `colorPrimaryDark` → `colorPrimary` → `colorBackground` →
 * solid `windowBackground`.
 */
object AppThemeBackgroundResolver {
    fun resolve(context: Context, packageName: String): Color? {
        if (packageName.isBlank()) return null
        return runCatching {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val resources = pm.getResourcesForApplication(appInfo)
            val theme = resources.newTheme()
            if (appInfo.theme != 0) {
                theme.applyStyle(appInfo.theme, true)
            }
            opaqueColorAttribute(theme, resources, android.R.attr.statusBarColor)
                ?: opaqueColorAttribute(theme, resources, android.R.attr.colorPrimaryDark)
                ?: opaqueColorAttribute(theme, resources, android.R.attr.colorPrimary)
                ?: opaqueColorAttribute(theme, resources, android.R.attr.colorBackground)
                ?: resolveWindowBackgroundColor(theme, resources)
        }.getOrNull()
    }

    /**
     * Whether the app theme asks for dark status-bar icons (light backdrop). Used when the fill
     * is known but we still want the platform light/dark chrome hint.
     */
    fun isLightTheme(context: Context, packageName: String): Boolean? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val resources = pm.getResourcesForApplication(appInfo)
            val theme = resources.newTheme()
            if (appInfo.theme != 0) theme.applyStyle(appInfo.theme, true)
            val value = TypedValue()
            if (!theme.resolveAttribute(android.R.attr.isLightTheme, value, true)) return@runCatching null
            value.data != 0
        }.getOrNull()
    }

    fun isMateriallyDifferent(previous: Color?, next: Color): Boolean {
        if (previous == null) return true
        val dr = kotlin.math.abs(previous.red - next.red)
        val dg = kotlin.math.abs(previous.green - next.green)
        val db = kotlin.math.abs(previous.blue - next.blue)
        return dr + dg + db > 0.04f
    }

    fun fromArgb(argb: Int): Color = Color(argb)

    fun isLight(color: Color): Boolean = color.luminance() > TrayThemeResolver.LIGHT_BACKGROUND_LUMINANCE

    private fun opaqueColorAttribute(
        theme: Resources.Theme,
        resources: Resources,
        attr: Int,
    ): Color? {
        val color = resolveColorAttribute(theme, resources, attr) ?: return null
        // Fully / nearly transparent statusBarColor means "draw content behind" — not a fill.
        if (color.alpha < 0.15f) return null
        return color.copy(alpha = 1f)
    }

    private fun resolveColorAttribute(
        theme: Resources.Theme,
        resources: Resources,
        attr: Int,
    ): Color? {
        val value = TypedValue()
        if (!theme.resolveAttribute(attr, value, true)) return null
        return colorFromTypedValue(resources, value)
    }

    private fun resolveWindowBackgroundColor(
        theme: Resources.Theme,
        resources: Resources,
    ): Color? {
        val value = TypedValue()
        if (!theme.resolveAttribute(android.R.attr.windowBackground, value, true)) return null
        colorFromTypedValue(resources, value)?.takeIf { it.alpha >= 0.15f }?.let {
            return it.copy(alpha = 1f)
        }
        if (value.type == TypedValue.TYPE_REFERENCE || value.resourceId != 0) {
            val drawable = runCatching { resources.getDrawable(value.resourceId, theme) }.getOrNull()
            if (drawable is ColorDrawable) {
                val color = Color(drawable.color)
                if (color.alpha >= 0.15f) return color.copy(alpha = 1f)
            }
        }
        return null
    }

    private fun colorFromTypedValue(resources: Resources, value: TypedValue): Color? {
        when {
            value.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT ->
                return Color(value.data)
            value.type == TypedValue.TYPE_REFERENCE || value.resourceId != 0 -> {
                val colorInt = runCatching { resources.getColor(value.resourceId, null) }.getOrNull()
                    ?: return null
                return Color(colorInt)
            }
        }
        return null
    }
}
