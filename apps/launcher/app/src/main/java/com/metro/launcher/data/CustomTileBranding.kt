package com.metro.launcher.data

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.metro.launcher.R
import com.metro.system.MetroPreferences

/**
 * WP-style custom Start / app-list tiles for selected third-party packages.
 *
 * Default look: white monochrome glyph on the system accent. Set [Entry.backgroundHex]
 * only when a fixed brand fill is required (e.g. WhatsApp green, YouTube Music red).
 *
 * Fully custom composed faces (e.g. Chrome wedges) live in `CustomTileFaces.kt`.
 */
object CustomTileBranding {
    data class Entry(
        @DrawableRes val glyphResId: Int,
        /** When null, Start/app-list use the current system accent. */
        val backgroundHex: String? = null,
    )

    private val byPackage: Map<String, Entry> = mapOf(
        // Google Search / Google app — white G on accent
        "com.google.android.googlequicksearchbox" to Entry(R.drawable.ic_tile_google),

        // YouTube Music — white play/waveform on brand red
        "com.google.android.apps.youtube.music" to Entry(
            glyphResId = R.drawable.ic_tile_yt_music,
            backgroundHex = "#FF0000",
        ),

        // WhatsApp (+ Business) — white bubble/handset on brand green
        "com.whatsapp" to Entry(
            glyphResId = R.drawable.ic_tile_whatsapp,
            backgroundHex = "#25D366",
        ),
        "com.whatsapp.w4b" to Entry(
            glyphResId = R.drawable.ic_tile_whatsapp,
            backgroundHex = "#25D366",
        ),

        // Camera — white camera glyph on system accent (WP system-app treatment)
        "com.android.camera2" to Entry(R.drawable.ic_tile_camera),
        "com.android.camera" to Entry(R.drawable.ic_tile_camera),
        "com.google.android.GoogleCamera" to Entry(R.drawable.ic_tile_camera),
        "com.samsung.android.camera" to Entry(R.drawable.ic_tile_camera),
        "com.sec.android.app.camera" to Entry(R.drawable.ic_tile_camera),
    )

    fun entry(packageName: String): Entry? = byPackage[packageName]

    fun hasCustomTile(packageName: String): Boolean = packageName in byPackage

    @DrawableRes
    fun glyphResId(packageName: String): Int? = byPackage[packageName]?.glyphResId

    fun resolveBackgroundColor(context: Context, packageName: String): Color? {
        val entry = byPackage[packageName] ?: return null
        val prefs = MetroPreferences(context)
        return entry.backgroundHex?.let { MetroPreferences.parseAccentHex(it) } ?: prefs.accentColor
    }
}
