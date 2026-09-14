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
    /** Vector drawable XML from Firestore (preferred logo when present). */
    val logoXml: String? = null,
    /** PNG logo as base64 from Firestore (e.g. People). */
    val logoPngBase64: String? = null,
    /** App tile / icon square fill as `#RRGGBB` when known from Firestore. */
    val backgroundColor: String? = null,
    val firestoreId: String? = null,
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
