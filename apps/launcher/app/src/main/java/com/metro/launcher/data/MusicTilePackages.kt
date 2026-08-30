package com.metro.launcher.data

import android.content.Context
import com.metro.system.MetroConnectedApps
import com.metro.system.MetroPreferences

/**
 * Packages whose pinned Start tiles should render the Xbox Music–style now-playing face
 * when an active [android.media.session.MediaSession] is available.
 *
 * Source of truth: Settings → connected apps → Music apps ([MetroPreferences.musicAppPackages]).
 */
object MusicTilePackages {
    fun contains(packageName: String): Boolean =
        packageName in MetroConnectedApps.DEFAULT_MUSIC_PACKAGES

    fun isMusicApp(context: Context, packageName: String): Boolean =
        packageName in MetroPreferences(context).musicAppPackages
}
