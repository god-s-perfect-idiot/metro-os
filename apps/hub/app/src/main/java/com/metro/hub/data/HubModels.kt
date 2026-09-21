package com.metro.hub.data

import com.metro.ui.MetroAppGlyphs
import com.metro.system.MetroAppRegistry

/**
 * Suite APK categories shown on the Hub quick-links tiles.
 * Asset names from GitHub releases look like `launcher-debug.apk`.
 */
enum class HubAppCategory(val label: String) {
    Core("core"),
    Shell("shell"),
    SecondParty("second party"),
    ThirdParty("third party"),
}

data class ReleaseApkAsset(
    val name: String,
    val displayName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val category: HubAppCategory,
    val packageName: String,
    val description: String,
    val publisher: String,
    /** Per-app Android versionName when known (not the suite release tag). */
    val versionName: String? = null,
    val versionCode: Int? = null,
    /** Optional remote icon (catalog / release asset). */
    val iconUrl: String? = null,
    /** Local glyph fallback from metro-ui-android. */
    val glyphResId: Int? = null,
    /** Vector drawable XML from Firestore (preferred logo when present). May also hold an https PNG URL. */
    val logoXml: String? = null,
    /** PNG logo as base64 from Firestore (e.g. People). */
    val logoPngBase64: String? = null,
    /** App tile / icon square fill as `#RRGGBB` when known from Firestore. */
    val backgroundColor: String? = null,
    val firestoreId: String? = null,
    /** Public GitHub repo URL when known (Firestore `githubRepo`). */
    val githubRepo: String? = null,
)

data class GitHubRelease(
    val tagName: String,
    val name: String?,
    val targetCommitish: String?,
    val assets: List<ReleaseApkAsset>,
    /** Present when the release ships a hub-catalog.json (preferred OTA metadata). */
    val catalogUrl: String? = null,
)

/** Optional release sidecar — preferred source for per-app version + icon URLs. */
data class HubCatalog(
    val tag: String?,
    val apps: Map<String, HubCatalogApp>,
)

data class HubCatalogApp(
    val packageName: String? = null,
    val versionName: String? = null,
    val versionCode: Int? = null,
    val description: String? = null,
    val publisher: String? = null,
    val iconUrl: String? = null,
)

object HubAppCatalog {
    private const val DefaultPublisher = "Entropy"
    const val CatalogAssetName = "hub-catalog.json"

    private val shell = setOf(
        "launcher", "statusbar", "notifications", "navbar", "volume", "lockscreen", "keyboard",
    )
    private val core = setOf(
        "browser", "notes", "music",
        "calculator", "clock", "files", "settings", "store", "hub",
        "photos", "calendar", "mail", "messaging", "people", "dialer",
    )

    private val descriptions = mapOf(
        "launcher" to "Start screen, live tiles, and app list for metro-os.",
        "statusbar" to "System tray overlay with clock, signal, and battery.",
        "notifications" to "WP8.1-style toast banners and notification surface.",
        "navbar" to "Soft keys: Back, Start, and Search.",
        "volume" to "Hardware rocker volume HUD for ringer and media.",
        "lockscreen" to "WP8.1 lock screen overlay above the system keyguard.",
        "keyboard" to "Metro SIP / Word Flow–style touch keyboard.",
        "browser" to "IE Mobile–style browser with tabs and favorites.",
        "notes" to "OneNote-style notebooks, sections, and pages.",
        "music" to "Xbox Music–style player with local and streaming library.",
        "photos" to "Photo hub with date and album pivots.",
        "calendar" to "Agenda, day, and month calendar views.",
        "mail" to "Linked inboxes and conversation mail.",
        "messaging" to "SMS and MMS message threads.",
        "people" to "Contacts hub and people directory.",
        "dialer" to "Phone dialer, call history, and in-call UI.",
        "store" to "App discovery shell for the metro-os suite.",
        "settings" to "System settings in WP8.1 hierarchy.",
        "calculator" to "Portrait calculator with scientific landscape mode.",
        "clock" to "Alarms, world clock, timer, and stopwatch.",
        "files" to "File explorer with pivot filters.",
        "hub" to "About metro-os and suite app downloads.",
    )

    fun categoryForAssetName(assetName: String): HubAppCategory {
        val id = assetId(assetName)
        return when {
            id in shell -> HubAppCategory.Shell
            id in core -> HubAppCategory.Core
            else -> HubAppCategory.Core
        }
    }

    fun displayNameForAsset(assetName: String): String {
        return assetId(assetName)
            .split('-', ' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    fun packageNameForAsset(assetName: String): String {
        val id = assetId(assetName)
        return "com.metro.$id"
    }

    fun descriptionForAsset(assetName: String): String {
        val id = assetId(assetName)
        return descriptions[id] ?: "metro-os suite app."
    }

    fun publisherForAsset(@Suppress("UNUSED_PARAMETER") assetName: String): String = "Entropy"

    fun glyphResIdForAsset(assetName: String): Int? {
        return MetroAppGlyphs.forPackage(packageNameForAsset(assetName))
    }

    /**
     * Catalog tile fill when Firestore omits `backgroundColor`.
     * Uses [MetroAppRegistry.brandHex], then local suite defaults (same as sync script).
     */
    fun backgroundColorForPackage(packageName: String): String? {
        MetroAppRegistry.brandHex(packageName)?.let { return it }
        val id = packageName.removePrefix("com.metro.")
        return brandHexFallback[id]
    }

    fun backgroundColorForAsset(assetName: String): String? =
        backgroundColorForPackage(packageNameForAsset(assetName))

    fun assetId(assetName: String): String {
        return assetName
            .removeSuffix(".apk")
            .removeSuffix("-debug")
            .removeSuffix("-release")
            .lowercase()
    }

    /** Case-insensitive match on display name, description, publisher, package, or APK name. */
    fun matchesQuery(asset: ReleaseApkAsset, query: String): Boolean {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return false
        return asset.displayName.lowercase().contains(needle) ||
            asset.description.lowercase().contains(needle) ||
            asset.publisher.lowercase().contains(needle) ||
            asset.packageName.lowercase().contains(needle) ||
            asset.name.lowercase().contains(needle)
    }

    fun filterByQuery(assets: List<ReleaseApkAsset>, query: String): List<ReleaseApkAsset> {
        val needle = query.trim()
        if (needle.isEmpty()) return emptyList()
        return assets.filter { matchesQuery(it, needle) }
    }

    /**
     * True when the catalog entry is strictly newer than the installed package.
     *
     * Prefers [versionCode] when both sides have one — never marks an update when the
     * installed code is greater or equal. Falls back to [versionName] only when codes
     * cannot decide; refuses to treat non-semver tags (e.g. `alpha-8`) as newer than a
     * dotted app version.
     */
    fun isNewerThanInstalled(
        catalogVersionCode: Int?,
        catalogVersionName: String?,
        installedVersionCode: Long,
        installedVersionName: String?,
    ): Boolean {
        val catalogCode = catalogVersionCode?.takeIf { it > 0 }?.toLong()
        val installedCode = installedVersionCode.takeIf { it > 0L }

        if (catalogCode != null && installedCode != null) {
            return catalogCode > installedCode
        }

        val remote = catalogVersionName?.trim().orEmpty()
        val local = installedVersionName?.trim().orEmpty()
        if (remote.isEmpty() || local.isEmpty()) return false

        // Installed build has a real versionCode but catalog only has a name — do not
        // claim the catalog APK is newer (local sideloads are often ahead of release).
        if (catalogCode == null && installedCode != null) {
            return false
        }

        return compareVersionNames(remote, local) > 0
    }

    /**
     * Compare dotted / tagged version names. Positive if [a] is newer than [b].
     * Non-semver tags (not starting with a digit) never beat a dotted app version.
     */
    fun compareVersionNames(a: String, b: String): Int {
        val aSemver = a.firstOrNull()?.isDigit() == true
        val bSemver = b.firstOrNull()?.isDigit() == true
        if (aSemver != bSemver) {
            return when {
                aSemver && !bSemver -> 1
                !aSemver && bSemver -> -1
                else -> 0
            }
        }
        val left = versionParts(a)
        val right = versionParts(b)
        if (left.isEmpty() && right.isEmpty()) return 0
        val n = maxOf(left.size, right.size)
        for (i in 0 until n) {
            val l = left.getOrElse(i) { 0 }
            val r = right.getOrElse(i) { 0 }
            if (l != r) return l.compareTo(r)
        }
        return 0
    }

    private fun versionParts(value: String): List<Int> =
        Regex("""\d+""").findAll(value).map { it.value.toIntOrNull() ?: 0 }.toList()

    /**
     * Featured pane: up to [count] random apps from the combined first/second/third pool.
     * When [force] is false and [existing] still resolve in [pool], refresh metadata only.
     */
    fun pickFeatured(
        pool: List<ReleaseApkAsset>,
        count: Int,
        existing: List<ReleaseApkAsset> = emptyList(),
        force: Boolean = true,
    ): List<ReleaseApkAsset> {
        if (pool.isEmpty()) return if (force) emptyList() else existing
        if (!force && existing.isNotEmpty()) {
            val refreshed = existing.mapNotNull { pick ->
                pool.find { it.name == pick.name }
            }
            return if (refreshed.size == existing.size) refreshed else pool.shuffled().take(count)
        }
        return pool.shuffled().take(count)
    }

    fun gradlePathForAsset(assetName: String): String {
        val id = assetId(assetName)
        return if (id == "keyboard") {
            "apps/keyboard/gradle.properties"
        } else {
            "apps/$id/app/build.gradle.kts"
        }
    }

    private val brandHexFallback = mapOf(
        "browser" to "#1BA1E2",
        "notes" to "#A200FF",
        "music" to "#E3008C",
        "settings" to "#F09609",
        "store" to "#7CB342",
        "photos" to "#EB3C00",
        "calendar" to "#0078D7",
        "mail" to "#0078D7",
        "messaging" to "#0078D7",
        "people" to "#D34829",
        "dialer" to "#0078D7",
        "calculator" to "#007500",
        "clock" to "#0078D7",
        "files" to "#0078D7",
        "hub" to "#1BA1E2",
        "launcher" to "#1BA1E2",
        "statusbar" to "#1BA1E2",
        "notifications" to "#1BA1E2",
        "navbar" to "#1BA1E2",
        "volume" to "#1BA1E2",
        "lockscreen" to "#1BA1E2",
        "keyboard" to "#1BA1E2",
    )
}

/** Installed launchable / suite app row for Hub → local → device. */
data class DeviceAppRow(
    val packageName: String,
    val label: String,
    val installedVersionName: String?,
    val installedVersionCode: Long,
    /** Matching Hub/GitHub catalog row when this package is in the suite catalog. */
    val catalogAsset: ReleaseApkAsset? = null,
    /** Catalog asset when a newer build is available (subset of [catalogAsset]). */
    val updateAsset: ReleaseApkAsset? = null,
) {
    val hasUpdate: Boolean get() = updateAsset != null

    /** Download / reinstall from Hub when a catalog APK URL exists. */
    val canDownload: Boolean
        get() = catalogAsset?.downloadUrl?.isNotBlank() == true

    /** Asset to download — prefer the update when present. */
    val downloadAsset: ReleaseApkAsset?
        get() = updateAsset ?: catalogAsset?.takeIf { it.downloadUrl.isNotBlank() }
}
