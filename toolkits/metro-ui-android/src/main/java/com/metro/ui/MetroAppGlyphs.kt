package com.metro.ui

import androidx.annotation.DrawableRes

/**
 * Suite-wide app identity glyphs (Start tiles, launcher icons, notification small icons).
 *
 * Canonical assets live in `metro-ui-android` (`metro_app_*`, `metro_tile_*`,
 * `metro_notification_*`). Apps must reference these instead of shipping duplicate vectors.
 */
object MetroAppGlyphs {
    @DrawableRes
    fun forPackage(packageName: String): Int? = suiteByPackage[packageName]

    @DrawableRes
    fun notificationForPackage(packageName: String): Int? = notificationByPackage[packageName]

    /** Third-party WP-style tile glyph overrides (Start / app list). */
    @DrawableRes
    fun tileOverride(packageName: String): Int? = tileOverrideByPackage[packageName]?.glyphResId

    fun tileOverrideEntry(packageName: String): TileOverride? = tileOverrideByPackage[packageName]

    data class TileOverride(
        @DrawableRes val glyphResId: Int,
        /** When null, Start/app-list use the current system accent. */
        val backgroundHex: String? = null,
    )

    val Browser: Int get() = R.drawable.metro_app_browser
    val Notes: Int get() = R.drawable.metro_app_notes
    val Music: Int get() = R.drawable.metro_app_music
    val Settings: Int get() = R.drawable.metro_app_settings
    val Store: Int get() = R.drawable.metro_app_store
    val Photos: Int get() = R.drawable.metro_app_photos
    val Calendar: Int get() = R.drawable.metro_app_calendar
    val Mail: Int get() = R.drawable.metro_app_mail
    val Messaging: Int get() = R.drawable.metro_app_messaging
    val MessagingUnread: Int get() = R.drawable.metro_app_messaging_unread
    val People: Int get() = R.drawable.metro_app_people
    val Phone: Int get() = R.drawable.metro_app_phone
    val Calculator: Int get() = R.drawable.metro_app_calculator
    val Clock: Int get() = R.drawable.metro_app_clock
    val Files: Int get() = R.drawable.metro_app_files
    val Lockscreen: Int get() = R.drawable.metro_app_lockscreen

    val NotificationPhone: Int get() = R.drawable.metro_notification_phone
    val NotificationMusic: Int get() = R.drawable.metro_notification_music

    private val suiteByPackage: Map<String, Int> = mapOf(
        "com.metro.browser" to R.drawable.metro_app_browser,
        "com.metro.notes" to R.drawable.metro_app_notes,
        "com.metro.music" to R.drawable.metro_app_music,
        "com.metro.settings" to R.drawable.metro_app_settings,
        "com.metro.store" to R.drawable.metro_app_store,
        "com.metro.photos" to R.drawable.metro_app_photos,
        "com.metro.calendar" to R.drawable.metro_app_calendar,
        "com.metro.mail" to R.drawable.metro_app_mail,
        "com.metro.messaging" to R.drawable.metro_app_messaging,
        "com.metro.people" to R.drawable.metro_app_people,
        "com.metro.dialer" to R.drawable.metro_app_phone,
        "com.metro.calculator" to R.drawable.metro_app_calculator,
        "com.metro.clock" to R.drawable.metro_app_clock,
        "com.metro.files" to R.drawable.metro_app_files,
        "com.metro.lockscreen" to R.drawable.metro_app_lockscreen,
    )

    private val notificationByPackage: Map<String, Int> = mapOf(
        "com.metro.dialer" to R.drawable.metro_notification_phone,
        "com.metro.music" to R.drawable.metro_notification_music,
    )

    private val tileOverrideByPackage: Map<String, TileOverride> = mapOf(
        "com.google.android.googlequicksearchbox" to TileOverride(R.drawable.metro_tile_google),
        "com.google.android.gm" to TileOverride(R.drawable.metro_tile_gmail),
        "com.google.android.gm.lite" to TileOverride(R.drawable.metro_tile_gmail),
        "com.google.android.apps.gmail" to TileOverride(R.drawable.metro_tile_gmail),
        "com.google.android.apps.youtube.music" to TileOverride(
            glyphResId = R.drawable.metro_tile_yt_music,
            backgroundHex = "#FF0000",
        ),
        "com.whatsapp" to TileOverride(
            glyphResId = R.drawable.metro_tile_whatsapp,
            backgroundHex = "#25D366",
        ),
        "com.whatsapp.w4b" to TileOverride(
            glyphResId = R.drawable.metro_tile_whatsapp,
            backgroundHex = "#25D366",
        ),
        "com.android.camera2" to TileOverride(R.drawable.metro_tile_camera),
        "com.android.camera" to TileOverride(R.drawable.metro_tile_camera),
        "com.google.android.GoogleCamera" to TileOverride(R.drawable.metro_tile_camera),
        "com.samsung.android.camera" to TileOverride(R.drawable.metro_tile_camera),
        "com.sec.android.app.camera" to TileOverride(R.drawable.metro_tile_camera),
    )
}
