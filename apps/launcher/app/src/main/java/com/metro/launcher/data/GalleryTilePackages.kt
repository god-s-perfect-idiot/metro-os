package com.metro.launcher.data

import android.content.Context
import com.metro.system.MetroConnectedApps
import com.metro.system.MetroPreferences

/**
 * Packages whose pinned Start tiles should use Photos-style gallery live faces
 * (provider photo grid / cycle when available).
 *
 * Source of truth: Settings → connected apps → Gallery apps ([MetroPreferences.galleryAppPackages]).
 */
object GalleryTilePackages {
    fun contains(packageName: String): Boolean =
        packageName in MetroConnectedApps.DEFAULT_GALLERY_PACKAGES

    fun isGalleryApp(context: Context, packageName: String): Boolean =
        packageName in MetroPreferences(context).galleryAppPackages
}
