package com.metro.statusbar

import android.content.Context
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.os.Build
import android.util.TypedValue
import androidx.compose.ui.graphics.Color

/**
 * Resolves a **logical** backdrop for transparent-tray glyph contrast (not a painted fill).
 *
 * No screenshots. Signals from the foreground app, in order:
 * 1. `windowLightStatusBar` — Android’s own “dark icons on light chrome” flag
 * 2. Opaque `statusBarColor` luminance (when the app still paints a solid bar)
 * 3. Page fill (`colorBackground` / `windowBackground`) luminance — not `colorPrimary` (brand)
 * 4. `isLightTheme`
 * 5. Package [Configuration.UI_MODE_NIGHT_MASK] via `createPackageContext`
 * 6. Metro system theme
 *
 * Icon / tile brand colors are never used — brand green ≠ page white under the tray.
 */
object TransparentBackdropResolver {
    fun fromAppTheme(
        context: Context,
        packageName: String?,
        systemDarkTheme: Boolean,
    ): Color {
        val pkg = packageName?.takeUnless { ForegroundAppDetector.isIgnored(it) }
        if (!pkg.isNullOrBlank()) {
            resolveFromPackage(context, pkg)?.let { return it }
        }
        return if (systemDarkTheme) Color.Black else Color.White
    }

    private fun resolveFromPackage(context: Context, packageName: String): Color? {
        return runCatching {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val resources = pm.getResourcesForApplication(appInfo)
            val theme = resources.newTheme()
            if (appInfo.theme != 0) {
                theme.applyStyle(appInfo.theme, true)
            }

            // 1. Explicit light-status-bar request (dark glyphs on light content).
            windowLightStatusBar(theme)?.let { lightBar ->
                return@runCatching if (lightBar) Color.White else Color.Black
            }

            // 2–3. Opaque chrome / page colors.
            opaqueAttr(theme, resources, android.R.attr.statusBarColor)?.let { return@runCatching it }
            opaqueAttr(theme, resources, android.R.attr.colorBackground)?.let { return@runCatching it }
            windowBackgroundColor(theme, resources)?.let { return@runCatching it }

            // 4. Theme light/dark bit.
            isLightTheme(theme)?.let { light ->
                return@runCatching if (light) Color.White else Color.Black
            }

            // 5. Package night-mode configuration.
            packageNightMode(context, packageName)?.let { night ->
                return@runCatching if (night) Color.Black else Color.White
            }

            null
        }.getOrNull()
    }

    private fun windowLightStatusBar(theme: android.content.res.Resources.Theme): Boolean? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        val value = TypedValue()
        if (!theme.resolveAttribute(android.R.attr.windowLightStatusBar, value, true)) return null
        return when (value.type) {
            TypedValue.TYPE_INT_BOOLEAN -> value.data != 0
            TypedValue.TYPE_INT_COLOR_ARGB8,
            TypedValue.TYPE_INT_COLOR_RGB8,
            TypedValue.TYPE_FIRST_INT,
            -> value.data != 0
            else -> value.data != 0
        }
    }

    private fun isLightTheme(theme: android.content.res.Resources.Theme): Boolean? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val value = TypedValue()
        if (!theme.resolveAttribute(android.R.attr.isLightTheme, value, true)) return null
        return value.data != 0
    }

    private fun packageNightMode(context: Context, packageName: String): Boolean? {
        return runCatching {
            val pkgContext = context.createPackageContext(packageName, 0)
            when (pkgContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> true
                Configuration.UI_MODE_NIGHT_NO -> false
                else -> null
            }
        }.getOrNull()
    }

    private fun opaqueAttr(
        theme: android.content.res.Resources.Theme,
        resources: android.content.res.Resources,
        attr: Int,
    ): Color? {
        val color = AppThemeBackgroundResolver.resolveColorAttr(theme, resources, attr) ?: return null
        if (color.alpha < 0.15f) return null
        return color.copy(alpha = 1f)
    }

    private fun windowBackgroundColor(
        theme: android.content.res.Resources.Theme,
        resources: android.content.res.Resources,
    ): Color? = AppThemeBackgroundResolver.resolveWindowBackground(theme, resources)
}
