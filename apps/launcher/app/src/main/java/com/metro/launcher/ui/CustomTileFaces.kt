package com.metro.launcher.ui

import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import com.metro.launcher.data.BrandTileFace
import kotlin.math.hypot
import kotlin.math.min

/** Matches toolkit [com.metro.ui.MetroSplashFace] glyph box. */
private val BrandSplashIconSize = 288.dp

/** Renders a composed brand Start face for app-list squares. */
@Composable
fun BrandTileFaceContent(
    face: BrandTileFace,
    modifier: Modifier = Modifier,
) {
    when (face) {
        BrandTileFace.Chrome -> ChromeTileContent(modifier = modifier)
        BrandTileFace.Facebook -> FacebookTileContent(modifier = modifier)
        BrandTileFace.YouTube -> YouTubeTileContent(modifier = modifier)
        BrandTileFace.YouTubeMusic -> YouTubeMusicTileContent(modifier = modifier)
        BrandTileFace.Amazon -> AmazonTileContent(modifier = modifier)
    }
}

/**
 * Open-splash face: full-bleed brand [backgroundColor] with the mark centered in the
 * standard splash glyph box (Start tiles keep their own corner/edge anchoring).
 */
@Composable
fun BrandTileFaceSplashContent(
    face: BrandTileFace,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        when (face) {
            // Tile face bottom-right-anchors the “f”; splash centers the mark alone.
            BrandTileFace.Facebook -> {
                Canvas(modifier = Modifier.size(BrandSplashIconSize)) {
                    drawPath(
                        path = facebookFPath(
                            width = size.width,
                            height = size.height,
                            centered = true,
                        ),
                        color = FacebookTileColors.Mark,
                    )
                }
            }
            else -> {
                BrandTileFaceContent(
                    face = face,
                    modifier = Modifier.size(BrandSplashIconSize),
                )
            }
        }
    }
}

/** Google Chrome brand colors (Material / product palette). */
internal object ChromeTileColors {
    val Red = Color(0xFFEA4335)
    val Yellow = Color(0xFFFBBC04)
    val Green = Color(0xFF34A853)
    val Blue = Color(0xFF4285F4)
    val Ring = Color.White
}

/** Classic Facebook tile blue from the brand SVG (`#3b5998`). */
internal object FacebookTileColors {
    val Blue = Color(0xFF3B5998)
    val Mark = Color.White
}

/** Classic YouTube tile colors from the brand SVG. */
internal object YouTubeTileColors {
    val Background = Color(0xFFE5E5E5)
    val You = Color.Black
    val TubeTop = Color(0xFFCB322C)
    val TubeBottom = Color(0xFF932420)
    val TubeText = Color.White
}

/** Amazon brand tile colors from the logo SVG (`#000` / `#f90`). */
internal object AmazonTileColors {
    val Background = Color.White
    val Wordmark = Color.Black
    val Smile = Color(0xFFFF9900)
}

/** YouTube Music brand red (matches suite CustomTileBranding #FF0000). */
internal object YouTubeMusicTileColors {
    val Red = Color(0xFFFF0000)
    val Mark = Color.White
}

private val ChromePackages = setOf(
    "com.android.chrome",
    "com.chrome.beta",
    "com.chrome.dev",
    "com.chrome.canary",
    "com.google.android.apps.chrome",
)

private val FacebookPackages = setOf(
    "com.facebook.katana",
    "com.facebook.lite",
    "com.facebook.mlite",
)

private val YouTubePackages = setOf(
    "com.google.android.youtube",
    "com.google.android.apps.youtube.mango",
)

private val YouTubeMusicPackages = setOf(
    "com.google.android.apps.youtube.music",
)

private val AmazonPackages = setOf(
    "com.amazon.mShop.android.shopping",
    "com.amazon.mShop.android",
    "in.amazon.mShop.android.shopping",
)

/** True when [packageName] may offer the composed Chrome Start face in tile customize. */
fun isChromeTilePackage(packageName: String): Boolean = packageName in ChromePackages

/** True when [packageName] may offer the composed Facebook Start face in tile customize. */
fun isFacebookTilePackage(packageName: String): Boolean = packageName in FacebookPackages

/** True when [packageName] may offer the composed YouTube Start face in tile customize. */
fun isYouTubeTilePackage(packageName: String): Boolean = packageName in YouTubePackages

/** True when [packageName] may offer the composed YouTube Music Start face in tile customize. */
fun isYouTubeMusicTilePackage(packageName: String): Boolean = packageName in YouTubeMusicPackages

/** True when [packageName] may offer the composed Amazon Start face in tile customize. */
fun isAmazonTilePackage(packageName: String): Boolean = packageName in AmazonPackages

/**
 * Custom Chrome Start face: square split into three brand wedges with a blue center disc.
 *
 * Wedges use a circumradius so the three colors fill the tile corners (clipped to the square).
 * Optional [title] overlays bottom-left on medium/wide tiles.
 */
@Composable
fun ChromeTileContent(
    title: String? = null,
    modifier: Modifier = Modifier,
) {
    // Circumradius wedges paint past the square edges so corners stay filled — clip here
    // so they never bleed into Start gutters or the tiles-tab cell padding.
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerRadius = hypot(size.width.toDouble(), size.height.toDouble()).toFloat() / 2f
            val minSide = min(size.width, size.height)
            val blueRadius = minSide * 0.22f
            val ringRadius = blueRadius * 1.28f
            val arcTopLeft = Offset(cx - outerRadius, cy - outerRadius)
            val arcSize = Size(outerRadius * 2f, outerRadius * 2f)

            // Canvas angles: 0° = east, clockwise. Red top-right, green bottom, yellow left.
            drawArc(
                color = ChromeTileColors.Red,
                startAngle = 270f,
                sweepAngle = 120f,
                useCenter = true,
                topLeft = arcTopLeft,
                size = arcSize,
            )
            drawArc(
                color = ChromeTileColors.Green,
                startAngle = 30f,
                sweepAngle = 120f,
                useCenter = true,
                topLeft = arcTopLeft,
                size = arcSize,
            )
            drawArc(
                color = ChromeTileColors.Yellow,
                startAngle = 150f,
                sweepAngle = 120f,
                useCenter = true,
                topLeft = arcTopLeft,
                size = arcSize,
            )
            drawCircle(
                color = ChromeTileColors.Ring,
                radius = ringRadius,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = ChromeTileColors.Blue,
                radius = blueRadius,
                center = Offset(cx, cy),
            )
        }
        BrandFaceTitle(
            title = title,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

/**
 * Custom Facebook Start face: sharp blue square with the classic white “f” mark (SVG path).
 * Optional [title] overlays bottom-left on medium/wide tiles.
 */
@Composable
fun FacebookTileContent(
    title: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // BG fills/stretches; “f” stays uniform and bottom-right anchored.
            drawRect(color = FacebookTileColors.Blue)
            drawPath(path = facebookFPath(size.width, size.height), color = FacebookTileColors.Mark)
        }
        BrandFaceTitle(
            title = title,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

/**
 * Custom YouTube Start face: light tile with SVG “You” / sharp red “Tube” bar wordmark.
 * Optional [title] overlays bottom-left (white on the red bar).
 */
@Composable
fun YouTubeTileContent(
    title: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // BG fills/stretches; wordmark + red bar stay uniform with padding.
            drawRect(color = YouTubeTileColors.Background)
            val fit = fitLogoInTile(
                tileW = size.width,
                tileH = size.height,
                srcW = YouTubeSvgSize,
                srcH = YouTubeSvgSize,
                paddingFraction = BrandLogoPadding,
            )
            drawPath(
                path = youtubeSvgPaths(
                    pathData = YouTubeYouPathData,
                    fit = fit,
                    preTranslateX = -YouTubeLayerTranslateX,
                    preTranslateY = -YouTubeLayerTranslateY,
                ),
                color = YouTubeTileColors.You,
            )
            // Sharp red bar (SVG used rounded caps — flatten to a rectangle).
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(YouTubeTileColors.TubeTop, YouTubeTileColors.TubeBottom),
                    startY = fit.offsetY + YouTubeTubeTop * fit.scale,
                    endY = fit.offsetY + YouTubeTubeBottom * fit.scale,
                ),
                topLeft = Offset(
                    fit.offsetX + YouTubeTubeLeft * fit.scale,
                    fit.offsetY + YouTubeTubeTop * fit.scale,
                ),
                size = Size(
                    (YouTubeTubeRight - YouTubeTubeLeft) * fit.scale,
                    (YouTubeTubeBottom - YouTubeTubeTop) * fit.scale,
                ),
            )
            drawPath(
                path = youtubeSvgPaths(
                    pathData = YouTubeTubePathData,
                    fit = fit,
                ),
                color = YouTubeTileColors.TubeText,
            )
        }
        BrandFaceTitle(
            title = title,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

/**
 * Custom Amazon Start face: white tile with the SVG wordmark + orange smile.
 * Optional [title] overlays bottom-left in dark ink.
 */
@Composable
fun AmazonTileContent(
    title: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // BG fills/stretches; logo stays uniform with padding.
            drawRect(color = AmazonTileColors.Background)
            val fit = fitLogoInTile(
                tileW = size.width,
                tileH = size.height,
                srcW = AmazonSvgWidth,
                srcH = AmazonSvgHeight,
                paddingFraction = BrandLogoPadding,
            )
            drawPath(
                path = amazonSvgPath(
                    pathData = AmazonWordmarkPathData,
                    fit = fit,
                    evenOdd = true,
                ),
                color = AmazonTileColors.Wordmark,
            )
            drawPath(
                path = amazonSvgPath(
                    pathData = AmazonSmilePathData,
                    fit = fit,
                    evenOdd = false,
                ),
                color = AmazonTileColors.Smile,
            )
        }
        BrandFaceTitle(
            title = title,
            color = AmazonTileColors.Wordmark,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

/**
 * Custom YouTube Music Start face: full-bleed brand red with centered ring + play mark.
 * The red fills the whole tile (not a circular badge). Optional [title] overlays bottom-left.
 */
@Composable
fun YouTubeMusicTileContent(
    title: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = YouTubeMusicTileColors.Red)
            val cx = size.width / 2f
            val cy = size.height / 2f
            val minSide = min(size.width, size.height)
            // Mark sized to the short edge so 1×1 / 2×2 / 4×2 keep the same relative logo.
            val ringRadius = minSide * 0.22f
            val stroke = minSide * 0.038f
            drawCircle(
                color = YouTubeMusicTileColors.Mark,
                radius = ringRadius,
                center = Offset(cx, cy),
                style = Stroke(width = stroke),
            )
            // Play triangle — slightly left-weighted tip, matching the product mark.
            val triLeft = cx - ringRadius * 0.28f
            val triRight = cx + ringRadius * 0.45f
            val triHalfH = ringRadius * 0.40f
            val play = Path().apply {
                moveTo(triLeft, cy - triHalfH)
                lineTo(triLeft, cy + triHalfH)
                lineTo(triRight, cy)
                close()
            }
            drawPath(path = play, color = YouTubeMusicTileColors.Mark)
        }
        BrandFaceTitle(
            title = title,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

@Composable
private fun BrandFaceTitle(
    title: String?,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
) {
    if (title.isNullOrBlank()) return
    val chrome = LocalTileChrome.current
    TileText(
        text = title,
        style = chrome.titleStyle,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = chrome.titlePaddingH,
                vertical = chrome.titlePaddingV,
            ),
    )
}

/** SVG viewBox edge for the Facebook brand mark (300×300). */
private const val FacebookSvgSize = 300f

/** SVG viewBox edge for the classic YouTube tile mark (256×256). */
private const val YouTubeSvgSize = 256f

/** Layer1 translate from the YouTube SVG (`translate(-373.642,-318.344)`). */
private const val YouTubeLayerTranslateX = 373.642f
private const val YouTubeLayerTranslateY = 318.344f

/**
 * Sharp “Tube” bar bounds in viewBox space (layer1 coords translated).
 * Derived from rect3708 extents including what were rounded caps.
 */
private const val YouTubeTubeLeft = 25.254f
private const val YouTubeTubeTop = 127.24f
private const val YouTubeTubeRight = 229.304f
private const val YouTubeTubeBottom = 229.164f

/** Black “You” letter paths in layer1 absolute coords (pre-translate). */
private val YouTubeYouPathData = listOf(
    "M433.279,353.763h15.304l1.97,7.879l1.667,5.818l1.364,5.697l1.364,5.939l1.061,4.97h0.606l1.061-4.97l1.212-5.939l1.515-5.697l1.515-5.818l2.121-7.879h14.849l-15.758,47.274v33.577h-14.091v-33.577L433.279,353.763z",
    "M497.826,373.642c11.97,0,18.031,5.697,18.031,16.97v27.88c0,11.273-6.061,16.97-18.031,16.97s-18.031-5.697-18.031-16.97v-27.88C479.795,379.34,485.855,373.642,497.826,373.642z M497.826,383.34c-3.182,0-4.849,1.939-4.849,5.576v31.272c0,3.515,1.818,5.576,4.849,5.576c3.031,0,4.849-2.061,4.849-5.576v-31.272C502.674,385.279,500.856,383.34,497.826,383.34z",
    "M553.433,434.614l-1.061-5.212c-5.455,4.485-8.638,6.061-12.426,6.061c-3.182,0-6.515-1.455-8.029-3.394c-1.668-2.061-2.121-4.243-2.121-10.425V374.37h13.182v46.911c0,3.03,1.061,4.485,3.484,4.485c1.668,0,3.183-0.606,5.91-2.424V374.37h13.183v60.244H553.433z",
)

/** White “Tube” letter paths already in 256×256 viewBox space. */
private val YouTubeTubePathData = listOf(
    "M56.337,150.482H44.354v-9.588h37.328v9.588H69.147v63.914h-12.81V150.482z",
    "M105.372,214.396l-0.964-4.737c-4.958,4.077-7.851,5.51-11.294,5.51c-2.893,0-5.923-1.321-7.3-3.086c-1.515-1.873-1.929-3.856-1.929-9.478v-42.978h11.984v42.646c0,2.756,0.964,4.078,3.168,4.078c1.515,0,2.893-0.552,5.372-2.203v-44.521h11.984v54.77H105.372L105.372,214.396z",
    "M129.34,140.895h11.983v23.914c4.683-4.188,7.851-5.841,11.433-5.841c2.756,0,5.785,1.322,7.162,3.085c1.516,1.983,1.93,3.969,1.93,9.809v30.414c0,5.73-0.414,7.715-1.93,9.697c-1.377,1.875-4.406,3.196-7.3,3.196c-3.582,0-6.612-1.653-11.295-5.951v5.181H129.34V140.895z M146.695,206.354c2.203,0,3.168-1.322,3.168-4.078v-30.416c0-2.864-0.965-4.076-3.168-4.076c-1.516,0-2.894,0.551-5.372,2.203v34.162C143.803,205.802,145.18,206.354,146.695,206.354z",
    "M207.163,195.223v1.983c0.138,1.433,0.138,2.425,0.138,2.976c0,9.697-5.923,14.987-16.805,14.987c-10.33,0-16.115-5.51-16.115-15.428v-25.456c0-10.358,5.51-15.317,16.666-15.317c9.918,0,16.116,5.179,16.116,13.664v16.089h-20.799v12.012c0,3.969,1.239,5.621,4.407,5.621c1.516,0,2.893-0.662,3.58-1.652c0.689-0.992,0.828-2.204,0.828-6.171c0-0.992,0-1.765,0-3.308H207.163z M195.18,181.007v-9.257c0-2.534-1.652-3.967-4.408-3.967c-2.754,0-4.406,1.433-4.406,3.967v9.257H195.18z",
)

/** Padding (fraction of the short tile edge) around centered YouTube / Amazon logos. */
private const val BrandLogoPadding = 0.12f

/** Uniform logo placement inside a tile. */
private data class LogoFit(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
)

/**
 * Fits a [srcW]×[srcH] logo into the tile with uniform scale and [paddingFraction]
 * inset from the short edge, then centers the result.
 */
private fun fitLogoInTile(
    tileW: Float,
    tileH: Float,
    srcW: Float,
    srcH: Float,
    paddingFraction: Float,
): LogoFit {
    val pad = min(tileW, tileH) * paddingFraction
    val availW = (tileW - 2f * pad).coerceAtLeast(1f)
    val availH = (tileH - 2f * pad).coerceAtLeast(1f)
    val scale = min(availW / srcW, availH / srcH)
    return LogoFit(
        scale = scale,
        offsetX = (tileW - srcW * scale) / 2f,
        offsetY = (tileH - srcH * scale) / 2f,
    )
}

/**
 * Parses SVG path data into a Compose [Path], mapped through a uniform [LogoFit].
 */
private fun youtubeSvgPaths(
    pathData: List<String>,
    fit: LogoFit,
    preTranslateX: Float = 0f,
    preTranslateY: Float = 0f,
): Path {
    val combined = android.graphics.Path()
    val matrix = Matrix().apply {
        // path → (optional layer translate) → uniform scale → center offset
        setTranslate(fit.offsetX, fit.offsetY)
        preScale(fit.scale, fit.scale)
        if (preTranslateX != 0f || preTranslateY != 0f) {
            preTranslate(preTranslateX, preTranslateY)
        }
    }
    pathData.forEach { data ->
        val parsed = PathParser.createPathFromPathData(data) ?: return@forEach
        parsed.transform(matrix)
        combined.addPath(parsed)
    }
    return combined.asComposePath()
}

/** Amazon logo SVG viewBox (`2.167 .438 251.038 259.969`). */
private const val AmazonSvgMinX = 2.167f
private const val AmazonSvgMinY = 0.438f
private const val AmazonSvgWidth = 251.038f
private const val AmazonSvgHeight = 259.969f

/** Black Amazon wordmark path from the brand SVG. */
private const val AmazonWordmarkPathData =
    "m150.744 108.13c0 13.141.332 24.1-6.31 35.77-5.361 9.489-13.853 15.324-23.341 15.324-12.952 0-20.495-9.868-20.495-24.432 0-28.75 25.76-33.968 50.146-33.968zm34.015 82.216c-2.23 1.992-5.456 2.135-7.97.806-11.196-9.298-13.189-13.615-19.356-22.487-18.502 18.882-31.596 24.527-55.601 24.527-28.37 0-50.478-17.506-50.478-52.565 0-27.373 14.85-46.018 35.96-55.126 18.313-8.066 43.884-9.489 63.43-11.718v-4.365c0-8.018.616-17.506-4.08-24.432-4.128-6.215-12.003-8.777-18.93-8.777-12.856 0-24.337 6.594-27.136 20.257-.57 3.037-2.799 6.026-5.835 6.168l-32.735-3.51c-2.751-.618-5.787-2.847-5.028-7.07 7.543-39.66 43.36-51.616 75.43-51.616 16.415 0 37.858 4.365 50.81 16.795 16.415 15.323 14.849 35.77 14.849 58.02v52.565c0 15.798 6.547 22.724 12.714 31.264 2.182 3.036 2.657 6.69-.095 8.966-6.879 5.74-19.119 16.415-25.855 22.393l-.095-.095"

/** Orange Amazon smile/arrow path from the brand SVG (`#f90`). */
private const val AmazonSmilePathData =
    "m221.503 210.324c-105.235 50.083-170.545 8.18-212.352-17.271-2.587-1.604-6.984.375-3.169 4.757 13.928 16.888 59.573 57.593 119.153 57.593 59.621 0 95.09-32.532 99.527-38.207 4.407-5.627 1.294-8.731-3.16-6.872zm29.555-16.322c-2.826-3.68-17.184-4.366-26.22-3.256-9.05 1.078-22.634 6.609-21.453 9.93.606 1.244 1.843.686 8.06.127 6.234-.622 23.698-2.826 27.337 1.931 3.656 4.79-5.57 27.608-7.255 31.288-1.628 3.68.622 4.629 3.68 2.178 3.016-2.45 8.476-8.795 12.14-17.774 3.639-9.028 5.858-21.622 3.71-24.424z"

/** Maps an Amazon SVG path from viewBox space through a uniform [LogoFit]. */
private fun amazonSvgPath(
    pathData: String,
    fit: LogoFit,
    evenOdd: Boolean,
): Path {
    val parsed = PathParser.createPathFromPathData(pathData) ?: return Path()
    if (evenOdd) {
        parsed.fillType = android.graphics.Path.FillType.EVEN_ODD
    }
    val matrix = Matrix().apply {
        setTranslate(fit.offsetX, fit.offsetY)
        preScale(fit.scale, fit.scale)
        preTranslate(-AmazonSvgMinX, -AmazonSvgMinY)
    }
    parsed.transform(matrix)
    return parsed.asComposePath().also {
        if (evenOdd) it.fillType = PathFillType.EvenOdd
    }
}

/**
 * Maps a point from the SVG “f” group local space into the 300×300 viewBox.
 * Mirrors the Wikimedia icon transforms (translate → Y-flip scale → translate).
 */
private fun facebookLocalToViewBox(x: Float, y: Float): Offset {
    val p1x = x + 307.7046f
    val p1y = y + 120.5527f
    val p2x = 1.1165523f * p1x - 103.48743f
    val p2y = -1.1165523f * p1y + 863.08638f
    return Offset(p2x - 33.466291f, p2y - 429.48076f)
}

/**
 * Classic Facebook “f” from the brand SVG path.
 *
 * Start tiles: uniform scale from the short edge, bottom-right anchored so the stem stays
 * flush. Open splash: [centered] fits the mark’s real bounds into the glyph box.
 */
private fun facebookFPath(
    width: Float,
    height: Float,
    centered: Boolean = false,
): Path {
    // Absolute points parsed from the relative SVG path under g4682 (viewBox space).
    val local = listOf(
        0f to 0f,
        0f to 103.355f,
        34.693f to 103.355f,
        39.887f to 143.635f,
        0f to 143.635f,
        0f to 169.352f,
        0f to 181.014f, // c1a
        3.238f to 188.961f, // c1b
        19.962f to 188.961f,
        41.292f to 188.971f,
        41.292f to 224.997f,
        37.602f to 225.487f, // c2a
        24.941f to 226.584f, // c2b
        10.211f to 226.584f,
        -20.542f to 226.584f, // c3a
        -41.595f to 207.813f, // c3b
        -41.595f to 173.340f,
        -41.595f to 143.635f,
        -76.376f to 143.635f,
        -76.376f to 103.355f,
        -41.595f to 103.355f,
        -41.595f to 0f,
    ).map { (x, y) -> facebookLocalToViewBox(x, y) }

    val scale: Float
    val offsetX: Float
    val offsetY: Float
    if (centered) {
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for (p in local) {
            minX = min(minX, p.x)
            minY = min(minY, p.y)
            maxX = maxOf(maxX, p.x)
            maxY = maxOf(maxY, p.y)
        }
        val srcW = (maxX - minX).coerceAtLeast(1f)
        val srcH = (maxY - minY).coerceAtLeast(1f)
        // Inset so the mark matches MetroSplashFace glyph weight (~70% of the box).
        val pad = min(width, height) * 0.15f
        val avail = (min(width, height) - 2f * pad).coerceAtLeast(1f)
        scale = avail / maxOf(srcW, srcH)
        offsetX = (width - srcW * scale) / 2f - minX * scale
        offsetY = (height - srcH * scale) / 2f - minY * scale
    } else {
        scale = min(width, height) / FacebookSvgSize
        offsetX = width - FacebookSvgSize * scale
        offsetY = height - FacebookSvgSize * scale
    }

    fun pt(index: Int): Offset {
        val v = local[index]
        return Offset(offsetX + v.x * scale, offsetY + v.y * scale)
    }

    val p0 = pt(0)
    val p1 = pt(1)
    val p2 = pt(2)
    val p3 = pt(3)
    val p4 = pt(4)
    val p5 = pt(5)
    val c1a = pt(6)
    val c1b = pt(7)
    val p6 = pt(8)
    val p7 = pt(9)
    val p8 = pt(10)
    val c2a = pt(11)
    val c2b = pt(12)
    val p9 = pt(13)
    val c3a = pt(14)
    val c3b = pt(15)
    val p10 = pt(16)
    val p11 = pt(17)
    val p12 = pt(18)
    val p13 = pt(19)
    val p14 = pt(20)
    val p15 = pt(21)
    return Path().apply {
        moveTo(p0.x, p0.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        lineTo(p4.x, p4.y)
        lineTo(p5.x, p5.y)
        cubicTo(c1a.x, c1a.y, c1b.x, c1b.y, p6.x, p6.y)
        lineTo(p7.x, p7.y)
        lineTo(p8.x, p8.y)
        cubicTo(c2a.x, c2a.y, c2b.x, c2b.y, p9.x, p9.y)
        cubicTo(c3a.x, c3a.y, c3b.x, c3b.y, p10.x, p10.y)
        lineTo(p11.x, p11.y)
        lineTo(p12.x, p12.y)
        lineTo(p13.x, p13.y)
        lineTo(p14.x, p14.y)
        lineTo(p15.x, p15.y)
        close()
    }
}

