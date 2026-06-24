package com.metro.launcher.data

import androidx.annotation.DrawableRes
import com.metro.launcher.R
import com.metro.system.MetroAppRegistry

/**
 * Pre-defined WP8.1-style glyphs for Metro suite apps that are not yet installed.
 * Installed apps always use their package launcher icon instead.
 */
object SystemAppPlaceholders {
    private val iconByPackage: Map<String, Int> = mapOf(
        "com.metro.browser" to R.drawable.ic_system_browser,
        "com.metro.notes" to R.drawable.ic_system_notes,
        "com.metro.music" to R.drawable.ic_system_music,
        "com.metro.settings" to R.drawable.ic_system_settings,
        "com.metro.store" to R.drawable.ic_system_store,
        "com.metro.photos" to R.drawable.ic_system_photos,
        "com.metro.calendar" to R.drawable.ic_system_calendar,
        "com.metro.mail" to R.drawable.ic_system_mail,
        "com.metro.messaging" to R.drawable.ic_system_messaging,
        "com.metro.people" to R.drawable.ic_system_people,
        "com.metro.calculator" to R.drawable.ic_system_calculator,
        "com.metro.clock" to R.drawable.ic_system_clock,
        "com.metro.files" to R.drawable.ic_system_files,
    )

    @DrawableRes
    fun iconResId(packageName: String): Int? = iconByPackage[packageName]

    fun label(packageName: String): String? = MetroAppRegistry.label(packageName)

    fun hasPlaceholder(packageName: String): Boolean = packageName in iconByPackage
}
