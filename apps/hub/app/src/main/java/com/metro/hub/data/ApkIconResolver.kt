package com.metro.hub.data

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Resolves launcher icons for release APKs.
 *
 * Order: cached PNG → installed package icon → download APK from GitHub +
 * [PackageManager.getPackageArchiveInfo] → persist PNG for next launch.
 */
class ApkIconResolver(
    context: Context,
    private val client: GitHubReleaseClient,
) {
    private val appContext = context.applicationContext
    private val iconDir = File(appContext.cacheDir, "icons").also { it.mkdirs() }
    private val apkDir = ApkInstaller.apkCacheDir(appContext)
    private val gate = Semaphore(permits = 2)
    private val locks = mutableMapOf<String, Mutex>()
    private val locksGuard = Mutex()

    fun cachedIconFile(asset: ReleaseApkAsset): File {
        return File(iconDir, "${asset.name}-${asset.sizeBytes}.png")
    }

    fun installedIcon(packageName: String): Drawable? {
        return try {
            appContext.packageManager.getApplicationIcon(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    suspend fun resolveIconFile(asset: ReleaseApkAsset): File? {
        val cached = cachedIconFile(asset)
        if (cached.exists() && cached.length() > 0L) return cached

        installedIcon(asset.packageName)?.let { drawable ->
            writeDrawable(drawable, cached)
            if (cached.exists()) return cached
        }

        val mutex = locksGuard.withLock { locks.getOrPut(asset.name) { Mutex() } }
        return mutex.withLock {
            if (cached.exists() && cached.length() > 0L) return@withLock cached
            gate.withPermit {
                withContext(Dispatchers.IO) {
                    extractFromApk(asset, cached)
                }
            }
        }
    }

    suspend fun readVersionFromApk(asset: ReleaseApkAsset): String? {
        return withContext(Dispatchers.IO) {
            val apk = localApk(asset) ?: return@withContext null
            val info = appContext.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
            info?.versionName?.takeIf { it.isNotBlank() }
        }
    }

    private fun localApk(asset: ReleaseApkAsset): File? {
        val existing = File(apkDir, asset.name)
        if (existing.exists() && existing.length() == asset.sizeBytes) return existing
        if (existing.exists() && asset.sizeBytes <= 0L && existing.length() > 0L) return existing
        return null
    }

    private fun extractFromApk(asset: ReleaseApkAsset, destIcon: File): File? {
        val apk = localApk(asset) ?: run {
            val download = File(apkDir, asset.name)
            runCatching { client.downloadToFile(asset.downloadUrl, download) }
                .onFailure { return null }
            download
        }
        val pm = appContext.packageManager
        val info = pm.getPackageArchiveInfo(apk.absolutePath, 0) ?: return null
        val appInfo = info.applicationInfo ?: return null
        appInfo.sourceDir = apk.absolutePath
        appInfo.publicSourceDir = apk.absolutePath
        val icon = runCatching { pm.getApplicationIcon(appInfo) }.getOrNull() ?: return null
        writeDrawable(icon, destIcon)
        return destIcon.takeIf { it.exists() && it.length() > 0L }
    }

    private fun writeDrawable(drawable: Drawable, dest: File) {
        val bitmap = when {
            drawable is BitmapDrawable && drawable.bitmap != null -> drawable.bitmap
            else -> drawable.toBitmap(
                width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 192,
                height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 192,
            )
        }
        dest.parentFile?.mkdirs()
        FileOutputStream(dest).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
