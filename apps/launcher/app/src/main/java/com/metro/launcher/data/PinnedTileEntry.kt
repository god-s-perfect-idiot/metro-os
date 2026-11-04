package com.metro.launcher.data

import android.appwidget.AppWidgetManager
import com.metro.system.MetroMusicLiveTileStyle

/**
 * Tile footprint on the Start grid (4 columns default; 6 when show more columns is on).
 * Reference: references/guides/blueprint.md
 */
enum class PinnedTileSize(
    val storageValue: String,
    val colSpan: Int,
    val rowSpan: Int,
) {
    OneByOne("1x1", 1, 1),
    TwoByTwo("2x2", 2, 2),
    FourByTwo("4x2", 4, 2),
    ;

    companion object {
        fun fromStorage(value: String?): PinnedTileSize = when (value) {
            "1x1", "small" -> OneByOne
            "2x2", "medium" -> TwoByTwo
            "4x2", "wide" -> FourByTwo
            else -> TwoByTwo
        }
    }
}

/** How the Start tile fill is resolved when the brush customize page is saved. */
enum class TileBackgroundMode(val storageValue: String) {
    /** Package / provider / branding defaults (existing Start behavior). */
    Default("default"),
    /** Force the system accent. */
    Accent("accent"),
    /** Fixed hex from the accent palette picker. */
    Custom("custom"),
    ;

    companion object {
        fun fromStorage(value: String?): TileBackgroundMode = when (value) {
            "accent" -> Accent
            "custom" -> Custom
            else -> Default
        }
    }
}

data class PinnedTileEntry(
    val packageName: String,
    val tileId: String = "primary",
    val size: PinnedTileSize = PinnedTileSize.OneByOne,
    /** Top-left grid column; assigned on first layout when null. */
    val gridCol: Int? = null,
    /** Top-left grid row; assigned on first layout when null. */
    val gridRow: Int? = null,
    val backgroundMode: TileBackgroundMode = TileBackgroundMode.Default,
    /** Accent-palette hex when [backgroundMode] is [TileBackgroundMode.Custom]. */
    val customBackgroundHex: String? = null,
    /** When true on 2×2 / 4×2 tiles, host an Android App Widget instead of the live face. */
    val useCustomWidget: Boolean = false,
    /** [android.content.ComponentName.flattenToString] of the selected App Widget provider. */
    val widgetProvider: String? = null,
    val appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID,
    /**
     * Optional package opened on tile tap instead of [packageName].
     * Null / blank = default (this tile's package / live-tile handlers).
     */
    val launchTargetPackage: String? = null,
    /**
     * Optional package whose launcher icon is drawn on this tile.
     * Null / blank = [packageName] (or suite branding / icon pack for that package).
     * Ignored when [useCustomIcon] is true.
     */
    val iconPackage: String? = null,
    /**
     * Optional Android icon-pack package for this tile's glyph only.
     * - Null → follow suite [com.metro.system.MetroPreferences.iconPackPackage]
     *   (Chrome / Facebook / YouTube / YouTube Music / Amazon: default composed brand face until customized)
     * - Blank → force system / suite glyphs (ignore the global pack)
     * - [CHROME_LEGACY_FACE] → composed Chrome wedge face (Chrome packages only)
     * - [FACEBOOK_CUSTOM_FACE] → composed Facebook brand face (Facebook packages only)
     * - [YOUTUBE_CUSTOM_FACE] → composed YouTube brand face (YouTube packages only)
     * - [YOUTUBE_MUSIC_CUSTOM_FACE] → composed YouTube Music face (Music packages only)
     * - [AMAZON_CUSTOM_FACE] → composed Amazon brand face (Amazon Shopping packages only)
     * - Otherwise → that pack for this pin
     *
     * Ignored when [useCustomIcon] is true.
     */
    val iconPackPackage: String? = null,
    /**
     * When true, draw the user-cropped photo from [TileCustomIcon] instead of the app glyph.
     */
    val useCustomIcon: Boolean = false,
    /**
     * Multiplier on the default Start glyph size for this tile. Clamped on read/write.
     */
    val iconScale: Float = DEFAULT_ICON_SCALE,
    /**
     * Optional Start-face name override. Null / blank = package / provider label.
     */
    val customTitle: String? = null,
    /** When true, medium/wide faces omit the bottom-left app name. */
    val hideTitle: Boolean = false,
    /**
     * Optional music now-playing face for connected music pins.
     * Null → suite default ([com.metro.system.MetroPreferences.musicLiveTileStyle]).
     */
    val musicLiveTileStyle: MetroMusicLiveTileStyle? = null,
) {
    companion object {
        const val DEFAULT_ICON_SCALE = 1f
        const val MIN_ICON_SCALE = 0.5f
        const val MAX_ICON_SCALE = 1.5f

        /**
         * Sentinel stored in [iconPackPackage] for the composed Chrome wedge Start face
         * (tile customize → tiles). Not a real Android package name.
         */
        const val CHROME_LEGACY_FACE = "__metro_chrome_legacy__"

        /**
         * Sentinel stored in [iconPackPackage] for the composed Facebook brand Start face
         * (tile customize → tiles). Not a real Android package name.
         */
        const val FACEBOOK_CUSTOM_FACE = "__metro_facebook__"

        /**
         * Sentinel stored in [iconPackPackage] for the composed YouTube brand Start face
         * (tile customize → tiles). Not a real Android package name.
         */
        const val YOUTUBE_CUSTOM_FACE = "__metro_youtube__"

        /**
         * Sentinel stored in [iconPackPackage] for the composed YouTube Music Start face
         * (tile customize → tiles). Not a real Android package name.
         */
        const val YOUTUBE_MUSIC_CUSTOM_FACE = "__metro_youtube_music__"

        /**
         * Sentinel stored in [iconPackPackage] for the composed Amazon brand Start face
         * (tile customize → tiles). Not a real Android package name.
         */
        const val AMAZON_CUSTOM_FACE = "__metro_amazon__"

        fun clampIconScale(value: Float): Float =
            value.coerceIn(MIN_ICON_SCALE, MAX_ICON_SCALE)

        /** True when [value] is a composed brand-face sentinel (not an icon pack). */
        fun isComposedTileFace(value: String?): Boolean =
            value == CHROME_LEGACY_FACE ||
                value == FACEBOOK_CUSTOM_FACE ||
                value == YOUTUBE_CUSTOM_FACE ||
                value == YOUTUBE_MUSIC_CUSTOM_FACE ||
                value == AMAZON_CUSTOM_FACE
    }
}

/** Package used for [com.metro.launcher.ui.MetroAppIcon] on this pin. */
fun PinnedTileEntry.resolvedIconPackage(): String =
    iconPackage?.takeIf { it.isNotBlank() } ?: packageName

/**
 * Icon-pack override for [com.metro.launcher.ui.MetroAppIcon].
 * Null → caller should use the suite global pack; blank → force system icons;
 * composed brand-face sentinels → not an icon pack; otherwise → that pack.
 */
fun PinnedTileEntry.resolvedIconPackOverride(): String? =
    iconPackPackage.takeUnless { PinnedTileEntry.isComposedTileFace(it) }

/**
 * True when this pin should draw the composed Chrome wedge face.
 * Default for never-customized pins ([iconPackPackage] null); blank / pack = opted out.
 */
fun PinnedTileEntry.usesChromeLegacyFace(): Boolean =
    when (iconPackPackage) {
        null, PinnedTileEntry.CHROME_LEGACY_FACE -> true
        else -> false
    }

/**
 * True when this pin should draw the composed Facebook brand face.
 * Default for never-customized pins ([iconPackPackage] null); blank / pack = opted out.
 */
fun PinnedTileEntry.usesFacebookCustomFace(): Boolean =
    when (iconPackPackage) {
        null, PinnedTileEntry.FACEBOOK_CUSTOM_FACE -> true
        else -> false
    }

/**
 * True when this pin should draw the composed YouTube brand face.
 * Default for never-customized pins ([iconPackPackage] null); blank / pack = opted out.
 */
fun PinnedTileEntry.usesYouTubeCustomFace(): Boolean =
    when (iconPackPackage) {
        null, PinnedTileEntry.YOUTUBE_CUSTOM_FACE -> true
        else -> false
    }

/**
 * True when this pin should draw the composed YouTube Music brand face.
 * Default for never-customized pins ([iconPackPackage] null); blank / pack = opted out.
 */
fun PinnedTileEntry.usesYouTubeMusicCustomFace(): Boolean =
    when (iconPackPackage) {
        null, PinnedTileEntry.YOUTUBE_MUSIC_CUSTOM_FACE -> true
        else -> false
    }

/**
 * True when this pin should draw the composed Amazon brand face.
 * Default for never-customized pins ([iconPackPackage] null); blank / pack = opted out.
 */
fun PinnedTileEntry.usesAmazonCustomFace(): Boolean =
    when (iconPackPackage) {
        null, PinnedTileEntry.AMAZON_CUSTOM_FACE -> true
        else -> false
    }

fun PinnedTileEntry.resolvedIconScale(): Float =
    PinnedTileEntry.clampIconScale(iconScale)

fun PinnedTileEntry.hasGridPosition(): Boolean = gridCol != null && gridRow != null

fun PinnedTileEntry.supportsCustomWidget(): Boolean =
    size == PinnedTileSize.TwoByTwo || size == PinnedTileSize.FourByTwo

fun PinnedTileEntry.hasActiveCustomWidget(): Boolean =
    useCustomWidget &&
        supportsCustomWidget() &&
        !widgetProvider.isNullOrBlank() &&
        appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID

object TileSizeCycle {
    fun nextSize(current: PinnedTileSize): PinnedTileSize = when (current) {
        PinnedTileSize.OneByOne -> PinnedTileSize.TwoByTwo
        PinnedTileSize.TwoByTwo -> PinnedTileSize.FourByTwo
        PinnedTileSize.FourByTwo -> PinnedTileSize.OneByOne
    }
}
