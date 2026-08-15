package com.metro.ui

import android.app.Activity
import android.os.Build
import androidx.annotation.StyleRes
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.metro.system.MetroAccentPalette
import com.metro.system.MetroPreferences

/**
 * WP8.1-style launch splash: full-bleed suite accent + app glyph.
 *
 * Setup (each app):
 * 1. Provide `@drawable/ic_launcher_foreground` (overrides the toolkit placeholder).
 * 2. Set the launcher activity `android:theme` to `@style/Theme.Metro.Splash`.
 * 3. Call [install] before `super.onCreate()`.
 *
 * On API 31+, [install] persists the current accent splash theme so the next cold
 * start matches the user's accent from [MetroPreferences].
 */
object MetroSplash {
    /** Installs the splash screen and remembers the current accent for the next launch. */
    fun install(activity: Activity): SplashScreen {
        val splash = activity.installSplashScreen()
        persistAccentSplashTheme(activity)
        return splash
    }

    /**
     * Persists the accent-colored splash theme for the next cold start (API 31+).
     * Safe to call when [com.metro.system.MetroBroadcasts.ACTION_THEME_CHANGED] arrives.
     */
    fun persistAccentSplashTheme(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val hex = MetroPreferences(activity).accentColorHex
        activity.splashScreen.setSplashScreenTheme(styleForAccentHex(hex))
    }

    @StyleRes
    fun styleForAccentHex(accentHex: String): Int {
        val name = MetroAccentPalette.findByHex(accentHex)?.name ?: "cyan"
        return when (name) {
            "lime" -> R.style.Theme_Metro_Splash_Lime
            "green" -> R.style.Theme_Metro_Splash_Green
            "emerald" -> R.style.Theme_Metro_Splash_Emerald
            "teal" -> R.style.Theme_Metro_Splash_Teal
            "cobalt" -> R.style.Theme_Metro_Splash_Cobalt
            "indigo" -> R.style.Theme_Metro_Splash_Indigo
            "violet" -> R.style.Theme_Metro_Splash_Violet
            "pink" -> R.style.Theme_Metro_Splash_Pink
            "magenta" -> R.style.Theme_Metro_Splash_Magenta
            "crimson" -> R.style.Theme_Metro_Splash_Crimson
            "red" -> R.style.Theme_Metro_Splash_Red
            "orange" -> R.style.Theme_Metro_Splash_Orange
            "amber" -> R.style.Theme_Metro_Splash_Amber
            "yellow" -> R.style.Theme_Metro_Splash_Yellow
            "brown" -> R.style.Theme_Metro_Splash_Brown
            "olive" -> R.style.Theme_Metro_Splash_Olive
            "steel" -> R.style.Theme_Metro_Splash_Steel
            "mauve" -> R.style.Theme_Metro_Splash_Mauve
            "taupe" -> R.style.Theme_Metro_Splash_Taupe
            else -> R.style.Theme_Metro_Splash_Cyan
        }
    }

    /** ARGB for the current suite accent (tests / custom drawables). */
    fun currentAccentArgb(activity: Activity): Int =
        android.graphics.Color.parseColor(MetroPreferences(activity).accentColorHex)
}
