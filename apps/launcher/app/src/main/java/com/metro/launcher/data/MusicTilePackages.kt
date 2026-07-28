package com.metro.launcher.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

/**
 * Packages whose pinned Start tiles should render the Xbox Music–style now-playing face
 * when an active [android.media.session.MediaSession] is available.
 */
object MusicTilePackages {
    private val known = setOf(
        "com.metro.music",
        "com.google.android.apps.youtube.music",
        "com.google.android.music",
        "com.spotify.music",
        "com.apple.android.music",
        "com.amazon.mp3",
        "deezer.android.app",
        "com.aspiro.tidal",
        "com.soundcloud.android",
        "com.android.music",
        "com.sec.android.app.music",
        "com.samsung.android.app.music",
    )

    fun contains(packageName: String): Boolean = packageName in known

    /** Known list first; otherwise treat [ApplicationInfo.CATEGORY_AUDIO] as a music app. */
    fun isMusicApp(context: Context, packageName: String): Boolean {
        if (contains(packageName)) return true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            info.category == ApplicationInfo.CATEGORY_AUDIO
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}
