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
import com.metro.system.MetroTileAgenda
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
    val backFaceTitle: String? = null,
    val backFaceBody: String? = null,
    val photoGrid: MetroTilePhotoGrid? = null,
    val agenda: MetroTileAgenda? = null,
)

class LauncherRepository(private val context: Context) {
    private val store = PinnedTileStore(context)
    private val packageManager = context.packageManager

    fun loadPinnedTiles(): List<PinnedTileEntry> {
        val loaded = store.load()
        val installed = loaded.filter { isPackageInstalled(it.packageName) }
        if (installed.size != loaded.size) {
            store.save(installed)
        }
        return installed
    }

    fun savePinnedTiles(tiles: List<PinnedTileEntry>) = store.save(tiles)

    fun wideTilesEnabled(): Boolean = store.wideTilesEnabled()

    fun discoverApps(pinned: List<PinnedTileEntry>): List<MetroAppInfo> {
        val pinnedPackages = pinned
            .map { it.packageName }
            .filter { isPackageInstalled(it) }
            .toSet()
        return MetroAppDiscovery.discoverInstalledApps(context, pinnedPackages)
    }

    fun filterApps(apps: List<MetroAppInfo>, query: String): List<MetroAppInfo> =
        MetroAppDiscovery.filterApps(apps, query)

    fun resolveDisplayTiles(
        pinned: List<PinnedTileEntry>,
        liveContent: Boolean = true,
    ): List<DisplayTile> =
        pinned.filter { isPackageInstalled(it.packageName) }.map { entry ->
            entry.toDisplayTile(liveContent = liveContent)
        }

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

    private fun PinnedTileEntry.toDisplayTile(liveContent: Boolean): DisplayTile {
        val providerData = if (liveContent) {
            MetroTileContract.readTile(context.contentResolver, packageName, tileId)
        } else {
            null
        }
        val label = resolveAppLabel(packageName)
        val title = providerData?.title ?: label ?: packageName.substringAfterLast('.')
        val background = providerData?.backgroundColorHex?.let { MetroPreferences.parseAccentHex(it) }
            ?: fallbackTileColor(packageName)
        val photoGrid = providerData?.photoGrid
        val agenda = providerData?.agenda?.takeIf { it.hasContent }
        val hasRichFrontFace = photoGrid?.hasContent == true || agenda != null
        val merged = TileNotificationStore.mergeIntoDisplay(
            packageName = packageName,
            providerCounter = providerData?.counter,
            providerBackFaceTitle = providerData?.backFaceTitle,
            hasRichFrontFace = hasRichFrontFace,
        )
        return DisplayTile(
            entry = this,
            title = title,
            backgroundColor = background,
            counter = merged.counter,
            deepLinkUri = providerData?.deepLinkUri,
            hasFlipFace = merged.hasFlipFace,
            backFaceTitle = merged.backFaceTitle,
            backFaceBody = merged.backFaceBody,
            photoGrid = photoGrid,
            agenda = agenda,
        )
    }

    private fun resolveAppLabel(packageName: String): String? = try {
        packageManager.getApplicationLabel(
            packageManager.getApplicationInfo(packageName, 0),
        ).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        SystemAppPlaceholders.label(packageName)
    }

    private fun isPackageInstalled(packageName: String): Boolean = try {
        packageManager.getApplicationInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun fallbackTileColor(packageName: String): Color =
        MetroAppBranding.loadAppIconAsset(context, packageName).backgroundColor
}
