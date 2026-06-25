package com.metro.launcher.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.ui.graphics.Color
import com.metro.system.MetroAppDiscovery
import com.metro.system.MetroAppInfo
import com.metro.system.MetroIntents
import com.metro.system.MetroPreferences
import com.metro.system.MetroTileContract
import com.metro.system.MetroTileData
import com.metro.system.MetroTilePhotoGrid
import com.metro.system.MetroAppBranding

data class DisplayTile(
    val entry: PinnedTileEntry,
    val title: String,
    val backgroundColor: Color,
    val counter: Int?,
    val deepLinkUri: String?,
    val hasFlipFace: Boolean,
    val photoGrid: MetroTilePhotoGrid? = null,
)

class LauncherRepository(private val context: Context) {
    private val store = PinnedTileStore(context)
    private val packageManager = context.packageManager

    fun loadPinnedTiles(): List<PinnedTileEntry> = store.load()

    fun savePinnedTiles(tiles: List<PinnedTileEntry>) = store.save(tiles)

    fun wideTilesEnabled(): Boolean = store.wideTilesEnabled()

    fun discoverApps(pinned: List<PinnedTileEntry>): List<MetroAppInfo> {
        val pinnedPackages = pinned.map { it.packageName }.toSet()
        return MetroAppDiscovery.discoverInstalledApps(context, pinnedPackages)
    }

    fun filterApps(apps: List<MetroAppInfo>, query: String): List<MetroAppInfo> =
        MetroAppDiscovery.filterApps(apps, query)

    fun resolveDisplayTiles(pinned: List<PinnedTileEntry>): List<DisplayTile> =
        pinned.map { entry -> entry.toDisplayTile() }

    fun refreshTileContent(packageName: String, tileId: String): MetroTileData? =
        MetroTileContract.readTile(context.contentResolver, packageName, tileId)

    fun requestUninstall(hostContext: Context, packageName: String) {
        if (packageName == hostContext.packageName) return
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
        if (hostContext !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        hostContext.startActivity(intent)
    }

    fun launchApp(packageName: String, deepLinkUri: String?) {
        val intent = when {
            !deepLinkUri.isNullOrBlank() -> Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkUri))
            else -> packageManager.getLaunchIntentForPackage(packageName)
        } ?: Intent(MetroIntents.ACTION_LAUNCH_APP).apply {
            setPackage(packageName)
            putExtra(MetroIntents.EXTRA_PACKAGE, packageName)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun PinnedTileEntry.toDisplayTile(): DisplayTile {
        val providerData = MetroTileContract.readTile(context.contentResolver, packageName, tileId)
        val label = resolveAppLabel(packageName)
        val title = providerData?.title ?: label ?: packageName.substringAfterLast('.')
        val background = providerData?.backgroundColorHex?.let { MetroPreferences.parseAccentHex(it) }
            ?: fallbackTileColor(packageName)
        return DisplayTile(
            entry = this,
            title = title,
            backgroundColor = background,
            counter = providerData?.counter,
            deepLinkUri = providerData?.deepLinkUri,
            hasFlipFace = providerData?.hasFlipFace == true,
            photoGrid = providerData?.photoGrid,
        )
    }

    private fun resolveAppLabel(packageName: String): String? = try {
        packageManager.getApplicationLabel(
            packageManager.getApplicationInfo(packageName, 0),
        ).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        SystemAppPlaceholders.label(packageName)
    }

    private fun fallbackTileColor(packageName: String): Color =
        MetroAppBranding.loadAppIconAsset(context, packageName).backgroundColor
}
