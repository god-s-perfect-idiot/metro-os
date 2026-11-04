package com.metro.launcher.data

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color

/**
 * How an app's glyph should appear in the app list and open-splash — matching Start when
 * the package has a pin with icon overrides, or the package-level Metro tile branding.
 */
enum class BrandTileFace {
    Chrome,
    Facebook,
    YouTube,
    YouTubeMusic,
    Amazon,
}

data class AppGlyphAppearance(
    /** Full-bleed composed brand face (Chrome / Facebook / …). */
    val brandFace: BrandTileFace? = null,
    /** User-cropped photo from [TileCustomIcon] for this pin. */
    val useCustomIcon: Boolean = false,
    val customIconPackageName: String? = null,
    val customIconTileId: String? = null,
    /** Package whose launcher / pack icon is drawn (defaults to the app). */
    val iconPackage: String,
    /**
     * Icon-pack override for this appearance.
     * Null → suite global pack; blank → force system icons; otherwise that pack.
     * Ignored when [brandFace] or [useCustomIcon] is set.
     */
    val iconPackPackage: String? = null,
    /** [CustomTileBranding] / [com.metro.ui.MetroAppGlyphs.tileOverride] glyph. */
    @DrawableRes val glyphResId: Int? = null,
    /** Square / splash fill when not using a full-bleed brand face or photo. */
    val backgroundColor: Color? = null,
)

/**
 * Resolves app-list / open-splash glyph appearance for [packageName].
 *
 * @param pinned Prefer the tapped Start pin when launching from a tile; otherwise any pin
 *   for the package (primary first) so app-list rows match Start.
 * @param backgroundColor Hint from the pinned [DisplayTile] when known (used for pin
 *   brush customize — accent / custom fills). Composed brand faces keep their own fill.
 */
fun resolveAppGlyphAppearance(
    context: Context,
    packageName: String,
    pinned: PinnedTileEntry? = null,
    backgroundColor: Color? = null,
): AppGlyphAppearance {
    val brandFace = brandTileFaceFor(packageName, pinned)
    if (brandFace != null) {
        // Face fill wins over DisplayTile accent — Start paints the brand color inside
        // the face, so list/splash must match that, not the under-tile brush.
        val pinBrush = pinned != null &&
            pinned.backgroundMode != TileBackgroundMode.Default &&
            backgroundColor != null
        return AppGlyphAppearance(
            brandFace = brandFace,
            iconPackage = packageName,
            backgroundColor = if (pinBrush) backgroundColor else brandFaceBackground(brandFace),
        )
    }

    if (pinned?.useCustomIcon == true) {
        return AppGlyphAppearance(
            useCustomIcon = true,
            customIconPackageName = pinned.packageName,
            customIconTileId = pinned.tileId,
            iconPackage = packageName,
            backgroundColor = backgroundColor,
        )
    }

    val iconPackage = pinned?.resolvedIconPackage() ?: packageName
    val iconPackOverride = pinned?.resolvedIconPackOverride()
    // Skip Metro tile glyphs when the pin forces system icons or a real icon pack.
    val skipTileGlyph = when (val pack = pinned?.iconPackPackage) {
        null -> false
        else -> !PinnedTileEntry.isComposedTileFace(pack)
    }

    // Google apps: white list/splash squares. Metro tile glyphs are white-on-accent for
    // Start — skip them here so the colorful system / pack icon reads on white.
    if (usesWhiteGoogleChrome(packageName)) {
        val pinBrush = pinned != null &&
            pinned.backgroundMode != TileBackgroundMode.Default &&
            backgroundColor != null
        return AppGlyphAppearance(
            iconPackage = iconPackage,
            iconPackPackage = iconPackOverride,
            glyphResId = null,
            backgroundColor = if (pinBrush) backgroundColor else Color.White,
        )
    }

    val branding = if (skipTileGlyph) null else CustomTileBranding.entry(iconPackage)
    val glyphResId = branding?.glyphResId
    val brandingBg = branding?.let {
        CustomTileBranding.resolveBackgroundColor(context, iconPackage)
    }

    return AppGlyphAppearance(
        iconPackage = iconPackage,
        iconPackPackage = iconPackOverride,
        glyphResId = glyphResId,
        backgroundColor = backgroundColor ?: brandingBg,
    )
}

/** Prefer primary pin, else first pin for [packageName]. */
fun List<PinnedTileEntry>.pinForPackage(packageName: String): PinnedTileEntry? {
    val matches = filter { it.packageName == packageName }
    return matches.firstOrNull { it.tileId == "primary" } ?: matches.firstOrNull()
}

fun brandTileFaceFor(packageName: String, pinned: PinnedTileEntry?): BrandTileFace? {
    if (pinned?.useCustomIcon == true) return null
    return when {
        isChromeBrandPackage(packageName) &&
            (pinned == null || pinned.usesChromeLegacyFace()) -> BrandTileFace.Chrome
        isFacebookBrandPackage(packageName) &&
            (pinned == null || pinned.usesFacebookCustomFace()) -> BrandTileFace.Facebook
        isYouTubeBrandPackage(packageName) &&
            (pinned == null || pinned.usesYouTubeCustomFace()) -> BrandTileFace.YouTube
        isYouTubeMusicBrandPackage(packageName) &&
            (pinned == null || pinned.usesYouTubeMusicCustomFace()) -> BrandTileFace.YouTubeMusic
        isAmazonBrandPackage(packageName) &&
            (pinned == null || pinned.usesAmazonCustomFace()) -> BrandTileFace.Amazon
        else -> null
    }
}

/** Fill behind a composed brand face on Start / list / splash. */
fun brandFaceBackground(face: BrandTileFace): Color = when (face) {
    // Chrome mark is self-colored; list/splash chrome is white (Google family).
    BrandTileFace.Chrome -> Color.White
    BrandTileFace.Facebook -> Color(0xFF3B5998)
    BrandTileFace.YouTube -> Color(0xFFE5E5E5)
    BrandTileFace.YouTubeMusic -> Color(0xFFFF0000)
    BrandTileFace.Amazon -> Color.White
}

/**
 * Google family packages that use a white app-list / open-splash square.
 * Composed YouTube / Chrome / … faces resolve earlier via [brandTileFaceFor].
 * YouTube Music keeps its red [CustomTileBranding] when the composed face is off.
 */
fun usesWhiteGoogleChrome(packageName: String): Boolean {
    if (isYouTubeMusicBrandPackage(packageName)) return false
    return packageName.startsWith("com.google.") ||
        packageName.startsWith("com.android.chrome") ||
        packageName.startsWith("com.chrome.") ||
        isChromeBrandPackage(packageName)
}

// Package sets mirrored from CustomTileFaces (kept here so data-layer resolve needs no UI import).
private fun isChromeBrandPackage(packageName: String): Boolean = packageName in setOf(
    "com.android.chrome",
    "com.chrome.beta",
    "com.chrome.dev",
    "com.chrome.canary",
    "com.google.android.apps.chrome",
)

private fun isFacebookBrandPackage(packageName: String): Boolean = packageName in setOf(
    "com.facebook.katana",
    "com.facebook.lite",
    "com.facebook.mlite",
)

private fun isYouTubeBrandPackage(packageName: String): Boolean = packageName in setOf(
    "com.google.android.youtube",
    "com.google.android.apps.youtube.mango",
)

private fun isYouTubeMusicBrandPackage(packageName: String): Boolean =
    packageName == "com.google.android.apps.youtube.music"

private fun isAmazonBrandPackage(packageName: String): Boolean = packageName in setOf(
    "com.amazon.mShop.android.shopping",
    "com.amazon.mShop.android",
    "in.amazon.mShop.android.shopping",
)
