package com.metro.launcher.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.ui.graphics.Color
import com.metro.launcher.data.AppLauncherOption
import com.metro.launcher.data.AppLauncherOptions
import com.metro.system.MetroAppDiscovery
import com.metro.system.MetroAppInfo
import com.metro.system.MetroIntents
import com.metro.system.MetroTileAgenda
import com.metro.system.MetroTileContract
import com.metro.system.MetroTileData
import com.metro.system.MetroTilePhotoGrid
import com.metro.launcher.data.adaptTilesToColumnCount
import com.metro.launcher.data.TILE_GRID_COLUMN_COUNT
import com.metro.system.MetroAppBranding
import com.metro.system.MetroStartBackground
import com.metro.launcher.data.CustomTileBranding
import com.metro.ui.MetroActivities

data class DisplayTile(
    val entry: PinnedTileEntry,
    val title: String,
    val backgroundColor: Color,
    val counter: Int?,
    val deepLinkUri: String?,
    val hasFlipFace: Boolean,
    /**
     * When true and a Start background is set, the tile fill is a viewport window.
     */
    val revealsStartBackground: Boolean = false,
    val backFaceTitle: String? = null,
    /** Middle peek line when a provider/notification supplies three stacked fields. */
    val backFaceSubtitle: String? = null,
    val backFaceBody: String? = null,
    val photoGrid: MetroTilePhotoGrid? = null,
    val agenda: MetroTileAgenda? = null,
    /** Full-bleed front-face photo (contact tiles). Distinct from [photoGrid] mosaics/cycles. */
    val imageUri: String? = null,
    /** When true, medium/wide tiles flip between [imageUri] and the app icon. */
    val flipToIcon: Boolean = false,
    /** Xbox Music–style now-playing face when a music app has an active media session. */
    val musicNowPlaying: MusicNowPlayingInfo? = null,
    /** Progress-bar notification (charging, downloads) drawn on the front of the tile. */
    val progress: TileProgressInfo? = null,
)

class LauncherRepository(private val context: Context) {
    private val store = PinnedTileStore(context)
    private val packageManager = context.packageManager

    fun loadPinnedTiles(columns: Int = TILE_GRID_COLUMN_COUNT): List<PinnedTileEntry> {
        val loaded = store.load()
        val installed = loaded.filter { isPackageInstalled(it.packageName) }
        val positioned = adaptTilesToColumnCount(installed, columns)
        if (installed.size != loaded.size || positioned != installed) {
            store.save(positioned)
        }
        return positioned
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

    fun queryAppOptions(packageName: String): List<AppLauncherOption> =
        AppLauncherOptions.query(context, packageName)

    fun launchAppOption(option: AppLauncherOption, launchContext: Context = context) =
        AppLauncherOptions.launch(launchContext, option)

    fun requestUninstall(hostContext: Context, packageName: String) {
        if (packageName == hostContext.packageName) return
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
        if (hostContext !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        hostContext.startActivity(intent)
    }

    fun launchApp(
        packageName: String,
        deepLinkUri: String?,
        launchContext: Context = context,
    ) {
        val intent = when {
            !deepLinkUri.isNullOrBlank() -> Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkUri))
            else -> packageManager.getLaunchIntentForPackage(packageName)
        } ?: Intent(MetroIntents.ACTION_LAUNCH_APP).apply {
            setPackage(packageName)
            putExtra(MetroIntents.EXTRA_PACKAGE, packageName)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // No platform slide — Start launches are instant until a custom open anim ships.
        MetroActivities.startActivityWithoutTransition(launchContext, intent)
    }

    private fun PinnedTileEntry.toDisplayTile(liveContent: Boolean): DisplayTile {
        val providerData = if (liveContent) {
            MetroTileContract.readTile(context.contentResolver, packageName, tileId)
        } else {
            null
        }
        val label = resolveAppLabel(packageName)
        val title = providerData?.title ?: label ?: packageName.substringAfterLast('.')
        val background = CustomTileBranding.resolveBackgroundColor(context, packageName)
            ?: MetroAppBranding.resolveTileBackgroundColor(
                context = context,
                packageName = packageName,
                providerBackgroundHex = providerData?.backgroundColorHex,
            )
        val revealsStartBackground = tileRevealsStartBackground(packageName)
        val photoGrid = resolvePhotoGrid(
            packageName = packageName,
            providerGrid = providerData?.photoGrid,
            liveContent = liveContent,
        )
        val agenda = providerData?.agenda?.takeIf { it.hasContent }
        val imageUri = providerData?.imageUri?.takeIf { it.isNotBlank() }
        val musicNowPlaying = if (liveContent && MusicTilePackages.isMusicApp(context, packageName)) {
            MusicNowPlayingStore.snapshot(packageName)?.takeIf { it.hasTrack || it.isPlaying }
        } else {
            null
        }
        val hasRichFrontFace =
            photoGrid?.hasContent == true ||
                agenda != null ||
                imageUri != null ||
                musicNowPlaying != null
        val merged = TileNotificationStore.mergeIntoDisplay(
            packageName = packageName,
            providerCounter = providerData?.counter,
            providerBackFaceTitle = providerData?.backFaceTitle,
            hasRichFrontFace = hasRichFrontFace,
        )
        val flipToIcon = imageUri != null && musicNowPlaying == null
        val progress = if (musicNowPlaying != null) null else merged.progress
        return DisplayTile(
            entry = this,
            title = title,
            backgroundColor = background,
            revealsStartBackground = revealsStartBackground,
            // Now-playing owns the tile; progress overlays the front but still peeks/flips.
            counter = if (musicNowPlaying != null) null else merged.counter,
            deepLinkUri = providerData?.deepLinkUri,
            hasFlipFace = if (musicNowPlaying != null) {
                false
            } else {
                merged.hasFlipFace || flipToIcon
            },
            backFaceTitle = if (musicNowPlaying != null) null else merged.backFaceTitle,
            backFaceSubtitle = if (musicNowPlaying != null) null else merged.backFaceSubtitle,
            backFaceBody = if (musicNowPlaying != null) null else merged.backFaceBody,
            photoGrid = photoGrid,
            agenda = agenda,
            imageUri = imageUri,
            flipToIcon = flipToIcon,
            musicNowPlaying = musicNowPlaying,
            progress = progress,
        )
    }

    /**
     * Accent-following tiles become transparent windows when a Start background is set.
     * Fixed-brand custom tiles stay opaque.
     */
    private fun tileRevealsStartBackground(packageName: String): Boolean {
        if (!MetroStartBackground.isEnabled(context)) return false
        CustomTileBranding.entry(packageName)?.let { entry ->
            // Explicit brand hex → opaque; accent-tracking custom glyph → transparent.
            return entry.backgroundHex == null
        }
        return MetroStartBackground.revealsThroughPackage(context, packageName)
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

    /**
     * Connected gallery apps get Photos-style cycle tiles from MediaStore (shared library).
     * Prefer a provider cycle grid that already has photo URIs (e.g. com.metro.photos with
     * permission); otherwise synthesize. People mosaics stay provider-only.
     */
    private fun resolvePhotoGrid(
        packageName: String,
        providerGrid: MetroTilePhotoGrid?,
        liveContent: Boolean,
    ): MetroTilePhotoGrid? {
        val isGallery = GalleryTilePackages.isGalleryApp(context, packageName)
        if (isGallery) {
            if (!liveContent) {
                return providerGrid?.takeIf { it.cycle }
                    ?: GalleryLiveTileStore.photoGrid(context)
            }
            val synthesized = GalleryLiveTileStore.photoGrid(context)
            val providerCycle = providerGrid?.takeIf { it.cycle && it.hasContent }
            val providerHasPhotos = providerCycle?.cells?.any { !it.imageUri.isNullOrBlank() } == true
            val synthesizedHasPhotos =
                synthesized?.cells?.any { !it.imageUri.isNullOrBlank() } == true
            return when {
                synthesizedHasPhotos -> synthesized
                providerHasPhotos -> providerCycle
                synthesized != null -> synthesized
                else -> providerCycle
            }
        }
        // Non-gallery: keep People mosaics; strip Photos-style cycle faces.
        return providerGrid?.let { grid ->
            if (grid.cycle) null else grid
        }
    }
}
