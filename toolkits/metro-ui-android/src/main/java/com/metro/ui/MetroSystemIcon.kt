package com.metro.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.asin
import kotlin.math.min

/**
 * Shared WP7/8.1 chrome glyphs for the whole suite.
 *
 * Apps must use these (via [MetroSystemIcon] / [MetroAppBarIcon]) instead of inventing
 * per-app Canvas icons for common actions. App identity art lives in [MetroAppGlyphs].
 *
 * Stroke weight: [MetroSystemIconStrokeFraction] of icon min-dimension, square terminals.
 * Reference: METRO-UX-LANGUAGE.md §9.
 */
enum class MetroSystemIconType {
    // Navigation / chrome
    Forward,
    Back,
    Search,
    Close,
    Unpin,
    Resize,
    Add,
    More,
    SwitchView,
    Refresh,

    // Common app-bar actions
    Phone,
    Message,
    Heart,
    DialPad,
    People,
    Delete,
    Check,
    Attach,
    Microphone,

    // SIP / keyboard chrome (prefer showCircle = false on keys / smartbar)
    Shift,
    ShiftLocked,
    Backspace,
    Enter,
    Emoji,
    Undo,
    Redo,
    Settings,
    Clipboard,
    Copy,
    Cut,
    Paste,
    SelectAll,
    Language,
    KeyboardHide,
    Send,
    Autocorrect,
    ChevronUp,
    ChevronDown,
    ChevronLeft,
    ChevronRight,

    // Media transport (also see [MetroMediaGlyph] for shuffle/repeat/queue)
    Play,
    Pause,
    Next,
    Previous,

    // Status / connectivity (also [drawMetroWifiGlyph] for live signal bands)
    Wifi,
}

/** WP8.1 Wi-Fi tray / Settings glyph arc count (excludes the hub). */
const val MetroWifiBandCount = 3

/** Fraction of icon min-dimension used as chrome glyph stroke (WP8.1-weight). */
internal const val MetroSystemIconStrokeFraction = 0.04f

@Composable
fun MetroSystemIcon(
    type: MetroSystemIconType,
    modifier: Modifier = Modifier,
    iconSize: Dp = 40.dp,
    color: Color = MetroTheme.colors.primaryText,
    showCircle: Boolean = true,
) {
    // Microphone uses the traced reference vector (Canvas approximations kept drifting).
    if (type == MetroSystemIconType.Microphone) {
        Box(
            modifier = modifier.size(iconSize),
            contentAlignment = Alignment.Center,
        ) {
            if (showCircle) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = size.minDimension * MetroSystemIconStrokeFraction
                    val circleRadius = size.minDimension * 0.42f - strokeWidth
                    drawCircle(
                        color = color,
                        radius = circleRadius,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                    )
                }
            }
            Image(
                painter = painterResource(id = R.drawable.metro_system_microphone),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                colorFilter = ColorFilter.tint(color),
            )
        }
        return
    }
    Canvas(modifier = modifier.size(iconSize)) {
        val strokeWidth = size.minDimension * MetroSystemIconStrokeFraction
        if (showCircle) {
            val circleRadius = size.minDimension * 0.42f - strokeWidth
            drawCircle(
                color = color,
                radius = circleRadius,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
            )
        }
        drawMetroSystemIconGlyph(type, color)
    }
}

/**
 * Draws a [MetroSystemIconType] glyph into an arbitrary [DrawScope] (no outer ring).
 * Used by [MetroSystemIcon] for shared suite / SIP chrome glyphs.
 *
 * Stroke language matches WP8.1 Segoe UI Symbol chrome: relatively thick lines with
 * square terminals ([StrokeCap.Butt]) and sharp corners ([StrokeJoin.Miter]).
 */
fun DrawScope.drawMetroSystemIconGlyph(
    type: MetroSystemIconType,
    color: Color,
) {
    val strokeWidth = size.minDimension * MetroSystemIconStrokeFraction
    val glyphStroke = Stroke(
        width = strokeWidth,
        cap = StrokeCap.Butt,
        join = StrokeJoin.Miter,
        miter = 4f,
    )
    val forwardGlyphStroke = Stroke(
        width = strokeWidth * 1.1f,
        cap = StrokeCap.Butt,
        join = StrokeJoin.Miter,
        miter = 4f,
    )
    when (type) {
        MetroSystemIconType.Forward -> drawForwardGlyph(color, forwardGlyphStroke)
        MetroSystemIconType.Back -> drawBackGlyph(color, glyphStroke)
        MetroSystemIconType.Search -> drawSearchGlyph(color)
        MetroSystemIconType.Close -> drawCloseGlyph(color)
        MetroSystemIconType.Unpin -> drawUnpinGlyph(color, glyphStroke)
        MetroSystemIconType.Resize -> drawResizeGlyph(color, glyphStroke)
        MetroSystemIconType.Add -> drawAddGlyph(color)
        MetroSystemIconType.More -> drawMoreGlyph(color, glyphStroke)
        MetroSystemIconType.SwitchView -> drawRefreshGlyph(color)
        MetroSystemIconType.Refresh -> drawRefreshGlyph(color)
        MetroSystemIconType.Phone -> drawViewportPath(phoneHandsetPath, color, 0.72f)
        MetroSystemIconType.Message -> drawViewportPath(messagingBubblePath, color, 0.66f)
        MetroSystemIconType.Heart -> drawHeartGlyph(color)
        MetroSystemIconType.DialPad -> drawDialPadGlyph(color, glyphStroke)
        MetroSystemIconType.People -> drawPeopleGlyph(color, glyphStroke)
        MetroSystemIconType.Delete -> drawDeleteGlyph(color, glyphStroke)
        MetroSystemIconType.Check -> drawCheckGlyph(color)
        MetroSystemIconType.Attach -> drawAttachGlyph(color, glyphStroke)
        MetroSystemIconType.Microphone -> drawMicrophoneGlyph(color, glyphStroke)
        MetroSystemIconType.Shift -> drawShiftGlyph(color, locked = false)
        MetroSystemIconType.ShiftLocked -> drawShiftGlyph(color, locked = true)
        MetroSystemIconType.Backspace -> drawBackspaceGlyph(color, glyphStroke)
        MetroSystemIconType.Enter -> drawEnterGlyph(color, glyphStroke)
        MetroSystemIconType.Emoji -> drawEmojiGlyph(color, glyphStroke)
        MetroSystemIconType.Undo -> drawUndoRedoGlyph(color, glyphStroke, redo = false)
        MetroSystemIconType.Redo -> drawUndoRedoGlyph(color, glyphStroke, redo = true)
        MetroSystemIconType.Settings -> drawSettingsGlyph(color, glyphStroke)
        MetroSystemIconType.Clipboard -> drawClipboardGlyph(color, glyphStroke)
        MetroSystemIconType.Copy -> drawCopyGlyph(color, glyphStroke)
        MetroSystemIconType.Cut -> drawCutGlyph(color, glyphStroke)
        MetroSystemIconType.Paste -> drawPasteGlyph(color, glyphStroke)
        MetroSystemIconType.SelectAll -> drawSelectAllGlyph(color, glyphStroke)
        MetroSystemIconType.Language -> drawLanguageGlyph(color, glyphStroke)
        MetroSystemIconType.KeyboardHide -> drawKeyboardHideGlyph(color, glyphStroke)
        MetroSystemIconType.Send -> drawSendGlyph(color)
        MetroSystemIconType.Autocorrect -> drawAutocorrectGlyph(color, glyphStroke)
        MetroSystemIconType.ChevronUp -> drawChevronGlyph(color, glyphStroke, direction = 0)
        MetroSystemIconType.ChevronDown -> drawChevronGlyph(color, glyphStroke, direction = 1)
        MetroSystemIconType.ChevronLeft -> drawChevronGlyph(color, glyphStroke, direction = 2)
        MetroSystemIconType.ChevronRight -> drawChevronGlyph(color, glyphStroke, direction = 3)
        MetroSystemIconType.Play -> drawPlayGlyph(color)
        MetroSystemIconType.Pause -> drawPauseGlyph(color)
        MetroSystemIconType.Next -> drawSkipGlyph(color, forward = true)
        MetroSystemIconType.Previous -> drawSkipGlyph(color, forward = false)
        MetroSystemIconType.Wifi -> drawMetroWifiGlyph(color = color)
    }
}

/**
 * WP8.1 Wi-Fi glyph: hub at bottom-right, three thick quarter-arcs toward top-left.
 * Outer arc intentionally clips the canvas edges (Settings tile / system-tray look).
 *
 * Hub is a full disc that touches the bottom + right edges of the glyph box. Each arc
 * is concentric with the hub and swept slightly past 90° so its butt ends land on those
 * same edges — the hub does not bulge past the bars.
 *
 * Full-strength chrome uses [MetroSystemIconType.Wifi]. Status tray passes [filledBands]
 * for live RSSI; inactive arcs use [inactiveColor].
 */
fun DrawScope.drawMetroWifiGlyph(
    color: Color,
    inactiveColor: Color = color,
    filledBands: Int = MetroWifiBandCount,
    bandCount: Int = MetroWifiBandCount,
) {
    val bands = filledBands.coerceIn(0, bandCount)
    val count = bandCount.coerceAtLeast(1)
    val avail = min(size.width, size.height)
    // Equal stroke ≈ gap. Sized so the outer half-stroke exceeds [avail] and clips
    // the top/left edges — matches the Segoe / WP Settings Wi-Fi tile.
    val strokeWidth = avail * 1.1f / (2f * count + 0.5f)
    val gap = strokeWidth
    val hubR = strokeWidth * 0.5f
    // Inset so the full hub disc stays inside and touches bottom + right.
    val anchor = Offset(size.width - hubR, size.height - hubR)
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
    for (index in 0 until count) {
        val radius = hubR + gap + strokeWidth * 0.5f + index * (strokeWidth + gap)
        // Extend past 180°/270° so butt ends reach the box edges (y = height, x = width).
        val alpha = Math.toDegrees(asin((hubR / radius).coerceIn(0f, 1f)).toDouble()).toFloat()
        drawArc(
            color = if (index < bands) color else inactiveColor,
            startAngle = 180f - alpha,
            sweepAngle = 90f + 2f * alpha,
            useCenter = false,
            topLeft = Offset(anchor.x - radius, anchor.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = stroke,
        )
    }
    // Hub stays active while connected (callers hide the glyph when disconnected).
    drawCircle(color, hubR, anchor)
}

/**
 * Tappable system icon with the WP7 circular-outline affordance.
 */
@Composable
fun MetroCircleIconButton(
    type: MetroSystemIconType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    color: Color = MetroTheme.colors.primaryText,
    backgroundColor: Color? = null,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (backgroundColor != null) {
                    Modifier.background(backgroundColor, CircleShape)
                } else {
                    Modifier
                },
            )
            .semantics {
                role = Role.Button
                contentDescription?.let { this.contentDescription = it }
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        MetroSystemIcon(
            type = type,
            iconSize = size * 0.82f,
            color = if (enabled) color else color.copy(alpha = 0.4f),
        )
    }
}

private fun DrawScope.drawForwardGlyph(color: Color, stroke: Stroke) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val arm = size.minDimension * 0.16f
    drawLine(color, Offset(cx - arm, cy), Offset(cx + arm, cy), stroke.width, StrokeCap.Butt)
    val path = Path().apply {
        moveTo(cx + arm * 0.2f, cy - arm * 0.85f)
        lineTo(cx + arm, cy)
        lineTo(cx + arm * 0.2f, cy + arm * 0.85f)
    }
    drawPath(path, color, style = stroke)
}

private fun DrawScope.drawBackGlyph(color: Color, stroke: Stroke) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val arm = size.minDimension * 0.16f
    drawLine(color, Offset(cx - arm, cy), Offset(cx + arm, cy), stroke.width, StrokeCap.Butt)
    val path = Path().apply {
        moveTo(cx - arm * 0.2f, cy - arm * 0.85f)
        lineTo(cx - arm, cy)
        lineTo(cx - arm * 0.2f, cy + arm * 0.85f)
    }
    drawPath(path, color, style = stroke)
}

/** Filled search — 512 viewBox path from the WP8.1 search reference SVG. */
private const val SEARCH_GLYPH_PATH =
    "M325.8 0C223 0 139.6 83.4 139.6 186.2c0 33.5 9 64.8 24.4 92L0 442.2l23.3 46.5L69.8 512l164-164c27.1 15.5 58.5 24.4 92 24.4C428.6 372.4 512 289 512 186.2S428.6 0 325.8 0zm0 314.2c-70.7 0-128-57.3-128-128s57.3-128 128-128s128 57.3 128 128s-57.3 128-128 128z"

private val searchGlyphPath: Path by lazy {
    PathParser().parsePathString(SEARCH_GLYPH_PATH).toPath().apply {
        fillType = PathFillType.EvenOdd
    }
}

private fun DrawScope.drawSearchGlyph(color: Color) {
    // Scale the filled reference into the ring inset (same footprint as Close / Check / Add).
    val scale = size.minDimension / 512f * 0.38f
    val cx = size.width / 2f
    val cy = size.height / 2f
    withTransform({
        translate(left = cx, top = cy)
        scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        translate(left = -256f, top = -256f)
    }) {
        drawPath(searchGlyphPath, color)
    }
}

/** Filled cancel X — 512 viewBox path from the WP8.1 close reference SVG. */
private const val CLOSE_GLYPH_PATH =
    "M512 76.8L435.2 0L256 179.2L76.8 0L0 76.8L179.2 256L0 435.2L76.8 512L256 332.8L435.2 512l76.8-76.8L332.8 256z"

private val closeGlyphPath: Path by lazy {
    PathParser().parsePathString(CLOSE_GLYPH_PATH).toPath()
}

private fun DrawScope.drawCloseGlyph(color: Color) {
    // Scale the filled reference X into the ring inset (same visual footprint as prior stroke X).
    val scale = size.minDimension / 512f * 0.38f
    val cx = size.width / 2f
    val cy = size.height / 2f
    withTransform({
        translate(left = cx, top = cy)
        scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        translate(left = -256f, top = -256f)
    }) {
        drawPath(closeGlyphPath, color)
    }
}

private fun DrawScope.drawCheckGlyph(color: Color) {
    // Filled accept check — geometry from the WP8.1 check reference SVG (512 viewBox).
    drawMetroCheckGlyph(color, glyphScale = 0.38f)
}

/**
 * Filled WP8.1 checkmark (512 viewBox reference path). [glyphScale] is the fraction of
 * [DrawScope] min-dimension occupied by the path's viewBox — ~0.38 inside app-bar rings,
 * ~0.78 inside [MetroCheckBox].
 */
internal fun DrawScope.drawMetroCheckGlyph(color: Color, glyphScale: Float) {
    val scale = size.minDimension / 512f * glyphScale
    val cx = size.width / 2f
    val cy = size.height / 2f
    withTransform({
        translate(left = cx, top = cy)
        scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        translate(left = -256f, top = -256f)
    }) {
        drawPath(checkGlyphPath, color)
    }
}

/** Filled accept check — 512 viewBox path from the WP8.1 check reference SVG. */
private const val CHECK_GLYPH_PATH =
    "M437.3 30L202.7 339.3L64 200.7l-64 64L213.3 478L512 94z"

private val checkGlyphPath: Path by lazy {
    PathParser().parsePathString(CHECK_GLYPH_PATH).toPath()
}

private fun DrawScope.drawUnpinGlyph(color: Color, stroke: Stroke) {
    val min = size.minDimension
    val cx = size.width / 2f
    val cy = size.height / 2f
    val s = min * 0.28f
    val pivot = Offset(cx, cy)

    rotate(degrees = 38f, pivot = pivot) {
        val pin = Path().apply {
            moveTo(cx - 0.28f * s, cy - 0.95f * s)
            lineTo(cx + 0.28f * s, cy - 0.95f * s)
            lineTo(cx + 0.28f * s, cy - 0.45f * s)
            lineTo(cx + 0.55f * s, cy - 0.15f * s)
            lineTo(cx + 0.55f * s, cy + 0.05f * s)
            lineTo(cx + 0.12f * s, cy + 0.35f * s)
            lineTo(cx, cy + 1.05f * s)
            lineTo(cx - 0.12f * s, cy + 0.35f * s)
            lineTo(cx - 0.55f * s, cy + 0.05f * s)
            lineTo(cx - 0.55f * s, cy - 0.15f * s)
            lineTo(cx - 0.28f * s, cy - 0.45f * s)
            close()
        }
        drawPath(pin, color)
    }

    val arm = s * 1.05f
    drawLine(
        color,
        Offset(cx - arm, cy - arm),
        Offset(cx + arm, cy + arm),
        stroke.width,
        StrokeCap.Butt,
    )
}

/** Diagonal arrow toward tile interior (WP8.1 resize affordance). */
private fun DrawScope.drawResizeGlyph(color: Color, stroke: Stroke) {
    val arm = size.minDimension * 0.14f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val start = Offset(cx + arm * 0.55f, cy - arm * 0.75f)
    val end = Offset(cx - arm * 0.75f, cy + arm * 0.55f)
    drawLine(color, start, end, stroke.width, StrokeCap.Butt)
    val path = Path().apply {
        moveTo(end.x + arm * 0.38f, end.y)
        lineTo(end.x, end.y)
        lineTo(end.x, end.y - arm * 0.38f)
    }
    drawPath(path, color, style = stroke)
}

/** Filled plus — 512 viewBox path from the WP8.1 add reference SVG. */
private const val ADD_GLYPH_PATH =
    "M298.7 213.3V0h-85.4v213.3H0v85.4h213.3V512h85.4V298.7H512v-85.4z"

private val addGlyphPath: Path by lazy {
    PathParser().parsePathString(ADD_GLYPH_PATH).toPath()
}

private fun DrawScope.drawAddGlyph(color: Color) {
    // Scale the filled reference plus into the ring inset (same footprint as Close / Check).
    val scale = size.minDimension / 512f * 0.38f
    val cx = size.width / 2f
    val cy = size.height / 2f
    withTransform({
        translate(left = cx, top = cy)
        scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        translate(left = -256f, top = -256f)
    }) {
        drawPath(addGlyphPath, color)
    }
}

private fun DrawScope.drawMoreGlyph(color: Color, stroke: Stroke) {
    val r = stroke.width * 1.15f
    val cy = size.height / 2f
    // Center-to-center spacing — loose enough that dots stay distinct at SIP sizes.
    val spacing = size.minDimension * 0.26f
    val cx = size.width / 2f
    drawCircle(color, r, Offset(cx - spacing, cy))
    drawCircle(color, r, Offset(cx, cy))
    drawCircle(color, r, Offset(cx + spacing, cy))
}

/** Filled refresh / rotate — 512 viewBox path from the WP8.1 refresh reference SVG. */
private const val REFRESH_GLYPH_PATH =
    "M247.6 393.8c-37.4 0-71.1-15.2-95.9-39.4h95.9l-59.1-59.1H31v157.5L90.1 512V403.6c39.5 42.2 95.2 69 157.5 69c113 0 205.7-86.5 215.6-196.9h-79.8c-9.6 66.7-66.4 118.1-135.8 118.1zM405.2 0v108.4c-39.5-42.2-95.2-69-157.5-69C134.6 39.4 42 125.9 32 236.3h79.8c9.6-66.7 66.5-118.2 135.9-118.2c37.4 0 71.1 15.2 95.9 39.4h-95.9l59.1 59.1h157.5V59.1L405.2 0z"

private val refreshGlyphPath: Path by lazy {
    PathParser().parsePathString(REFRESH_GLYPH_PATH).toPath()
}

/** Calendar switch-view and general refresh — filled circular arrows. */
internal fun DrawScope.drawRefreshGlyph(color: Color) {
    // Scale the filled reference into the ring inset (same footprint as Close / Check / Add).
    val scale = size.minDimension / 512f * 0.38f
    val cx = size.width / 2f
    val cy = size.height / 2f
    withTransform({
        translate(left = cx, top = cy)
        scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        translate(left = -256f, top = -256f)
    }) {
        drawPath(refreshGlyphPath, color)
    }
}

private const val PHONE_HANDSET_PATH =
    "M34.24,18.2 C33.27,18.41 32.13,19.11 30.54,20.49 C29.71,21.19 28.58,22.17 27.99,22.67 " +
        "C25.33,24.9 22.95,28.47 21.77,32 C20.85,34.81 20.66,36.02 20.66,39.56 C20.65,42.9 " +
        "20.78,43.99 21.51,46.88 C23.49,54.84 29.04,63.8 37.78,73.18 C43.54,79.36 49.88,84.15 " +
        "55.5,86.58 C63.12,89.87 70.19,90 76.54,86.94 C78.52,85.99 80.14,84.87 82.18,83.06 " +
        "C84.81,80.71 85.97,79.49 86.34,78.7 C87.35,76.52 86.51,74.16 83.71,71.35 C82.32,69.94 " +
        "78.75,66.89 77.26,65.83 C74.53,63.89 72.21,63.13 70.08,63.45 C68.18,63.75 66.45,64.93 " +
        "63.02,68.3 C61.91,69.4 61.31,69.87 60.63,70.18 C56.98,71.91 51.58,68.58 44.6,60.29 " +
        "C39.57,54.3 37.35,49.69 38.01,46.57 C38.29,45.24 39.06,44.21 40.66,43.03 C42.67,41.54 " +
        "45.01,39.45 45.68,38.53 C47.18,36.5 47.35,34.09 46.21,31.03 C45.23,28.4 41.56,22.65 " +
        "39.45,20.43 C38.51,19.45 38.03,19.08 37.25,18.69 C36.17,18.15 35.2,18 34.24,18.2 Z"

private val phoneHandsetPath: Path by lazy {
    PathParser().parsePathString(PHONE_HANDSET_PATH).toPath()
}

private const val MESSAGING_BUBBLE_PATH =
    "M18,28c0,-3 2.4,-5.4 5.4,-5.4h56c3,0 5.4,2.4 5.4,5.4v34c0,3 -2.4,5.4 -5.4,5.4H58l14,16l-8,-16H23.4c-3,0 -5.4,-2.4 -5.4,-5.4V28z" +
        "M35.5,40.5a3.2,3.2 0 1,0 6.4,0a3.2,3.2 0 1,0 -6.4,0z" +
        "M35.5,53.5a3.2,3.2 0 1,0 6.4,0a3.2,3.2 0 1,0 -6.4,0z" +
        "M47,45.2h12c1.2,0 2.2,1 2.2,2.2s-1,2.2 -2.2,2.2h-12c-1.2,0 -2.2,-1 -2.2,-2.2s1,-2.2 2.2,-2.2z"

private val messagingBubblePath: Path by lazy {
    PathParser().parsePathString(MESSAGING_BUBBLE_PATH).toPath().apply {
        fillType = PathFillType.EvenOdd
    }
}

internal fun DrawScope.drawViewportPath(path: Path, color: Color, glyphScale: Float) {
    val scale = size.minDimension / 108f * glyphScale
    val cx = size.width / 2f
    val cy = size.height / 2f
    withTransform({
        translate(left = cx, top = cy)
        scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        translate(left = -54f, top = -54f)
    }) {
        drawPath(path, color)
    }
}

/** Filled heart — 512 viewBox path from the WP8.1 heart reference SVG. */
private const val HEART_GLYPH_PATH =
    "M384 28.3c-64 0-96.2 27.6-128 64c-31.8-36.4-64-64-128-64S0 71 0 199c0 64 64 192 256 298.7C448 391 512 263 512 199c0-128-64-170.7-128-170.7"

private val heartGlyphPath: Path by lazy {
    PathParser().parsePathString(HEART_GLYPH_PATH).toPath()
}

internal fun DrawScope.drawHeartGlyph(color: Color) {
    // Scale the filled reference heart into the ring inset (same footprint as Close / Check / Add).
    val scale = size.minDimension / 512f * 0.38f
    val cx = size.width / 2f
    val cy = size.height / 2f
    withTransform({
        translate(left = cx, top = cy)
        scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        translate(left = -256f, top = -256f)
    }) {
        drawPath(heartGlyphPath, color)
    }
}

private fun DrawScope.drawDialPadGlyph(color: Color, stroke: Stroke) {
    val gap = size.width * 0.28f
    val tile = size.width * 0.22f
    for (row in 0..2) {
        for (col in 0..2) {
            drawRect(
                color = color,
                topLeft = Offset(col * gap, row * gap),
                size = Size(tile, tile),
                style = stroke,
            )
        }
    }
}

private fun DrawScope.drawPeopleGlyph(color: Color, stroke: Stroke) {
    val cx = size.width / 2f
    drawCircle(color = color, radius = size.minDimension * 0.14f, style = stroke)
    val path = Path().apply {
        moveTo(cx - size.minDimension * 0.22f, size.height * 0.72f)
        quadraticBezierTo(
            cx,
            size.height * 0.48f,
            cx + size.minDimension * 0.22f,
            size.height * 0.72f,
        )
    }
    drawPath(path, color, style = stroke)
}

private fun DrawScope.drawDeleteGlyph(color: Color, stroke: Stroke) {
    val s = size.minDimension
    val cx = size.width / 2f
    val top = size.height * 0.22f
    val lidY = size.height * 0.32f
    val bottom = size.height * 0.78f
    val left = cx - s * 0.22f
    val right = cx + s * 0.22f
    val cap = StrokeCap.Butt
    drawLine(color, Offset(cx - s * 0.12f, top), Offset(cx + s * 0.12f, top), stroke.width, cap)
    drawLine(color, Offset(left - s * 0.06f, lidY), Offset(right + s * 0.06f, lidY), stroke.width, cap)
    drawLine(color, Offset(left, lidY), Offset(left + s * 0.04f, bottom), stroke.width, cap)
    drawLine(color, Offset(right, lidY), Offset(right - s * 0.04f, bottom), stroke.width, cap)
    drawLine(color, Offset(left + s * 0.04f, bottom), Offset(right - s * 0.04f, bottom), stroke.width, cap)
    drawLine(color, Offset(cx, lidY + s * 0.06f), Offset(cx, bottom - s * 0.06f), stroke.width, cap)
}

/** WP paperclip / attach affordance. */
private fun DrawScope.drawAttachGlyph(color: Color, stroke: Stroke) {
    val s = size.minDimension
    val cx = size.width / 2f
    val cy = size.height / 2f
    val path = Path().apply {
        moveTo(cx + s * 0.08f, cy - s * 0.28f)
        lineTo(cx + s * 0.08f, cy + s * 0.12f)
        cubicTo(
            cx + s * 0.08f, cy + s * 0.28f,
            cx - s * 0.20f, cy + s * 0.28f,
            cx - s * 0.20f, cy + s * 0.08f,
        )
        lineTo(cx - s * 0.20f, cy - s * 0.18f)
        cubicTo(
            cx - s * 0.20f, cy - s * 0.30f,
            cx - s * 0.02f, cy - s * 0.30f,
            cx - s * 0.02f, cy - s * 0.16f,
        )
        lineTo(cx - s * 0.02f, cy + s * 0.06f)
    }
    drawPath(
        path,
        color,
        style = Stroke(
            width = stroke.width * 1.1f,
            cap = StrokeCap.Butt,
            join = StrokeJoin.Miter,
            miter = 4f,
        ),
    )
}

/**
 * Studio mic — same geometry as [R.drawable.metro_system_microphone]
 * (24×24 viewport traced from the provided SIP voice reference).
 */
private fun DrawScope.drawMicrophoneGlyph(color: Color, stroke: Stroke) {
    val dim = size.minDimension
    val origin = Offset((size.width - dim) / 2f, (size.height - dim) / 2f)
    withTransform({
        translate(left = origin.x, top = origin.y)
        scale(scaleX = dim / 24f, scaleY = dim / 24f, pivot = Offset.Zero)
    }) {
        val micLeft = 8.860f
        val micRight = 15.140f
        val micTop = 1.500f
        val micBot = 14.925f
        val r = 3.140f
        drawRoundRect(
            color = color,
            topLeft = Offset(micLeft, micTop),
            size = Size(micRight - micLeft, micBot - micTop),
            cornerRadius = CornerRadius(r, r),
        )
        val sw = 2.110f
        val cradleStroke = Stroke(
            width = sw,
            cap = StrokeCap.Butt,
            join = StrokeJoin.Round,
            miter = 4f,
        )
        val cx = 12f
        val cradleHalf = 5.346f
        val armTop = 9.747f
        val arcCy = 11.784f
        val cradle = Path().apply {
            moveTo(cx - cradleHalf, armTop)
            lineTo(cx - cradleHalf, arcCy)
            arcTo(
                rect = Rect(
                    left = cx - cradleHalf,
                    top = arcCy - cradleHalf,
                    right = cx + cradleHalf,
                    bottom = arcCy + cradleHalf,
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false,
            )
            lineTo(cx + cradleHalf, armTop)
        }
        drawPath(cradle, color, style = cradleStroke)
        drawLine(color, Offset(cx, 18.185f), Offset(cx, 21.397f), sw, StrokeCap.Butt)
        drawLine(color, Offset(7.757f, 21.397f), Offset(16.243f, 21.397f), sw, StrokeCap.Butt)
    }
}

/**
 * WP SIP shift arrow — open chevron head with a stem through the center
 * (Segoe UI Symbol / WP8.1 SIP). No filled body and no base bar on the head.
 * [locked] adds the caps-lock underline under the stem.
 *
 * Filled outline (not stroked lines): a stem butt at the tip clips the miter peak
 * and leaves the notch visible on device.
 */
private fun DrawScope.drawShiftGlyph(color: Color, locked: Boolean) {
    val s = size.minDimension
    // Stem/arm weight ≈ WP8.1 reference (~1/5 of glyph width).
    val t = s * 0.16f
    val cx = size.width / 2f
    val tipY = size.height * 0.16f
    val baseY = size.height * 0.52f
    val stemBot = size.height * (if (locked) 0.70f else 0.84f)
    val headHalf = s * 0.32f
    val left = cx - headHalf
    val right = cx + headHalf
    val armDx = headHalf
    val armDy = baseY - tipY
    val theta = kotlin.math.atan2(armDy, armDx)
    val horizInset = t / kotlin.math.sin(theta).toFloat()
    val halfTip = kotlin.math.atan2(armDx, armDy)
    val tipLift = t / kotlin.math.sin(halfTip).toFloat()

    // Filled up-chevron with flat arm ends and a solid sharp tip.
    val head = Path().apply {
        moveTo(left, baseY)
        lineTo(cx, tipY)
        lineTo(right, baseY)
        lineTo(right - horizInset, baseY)
        lineTo(cx, tipY + tipLift)
        lineTo(left + horizInset, baseY)
        close()
    }
    drawPath(head, color)

    // Stem through the opening; starts at the inner tip so it does not amputate the peak.
    val stemTop = tipY + tipLift
    drawRect(
        color = color,
        topLeft = Offset(cx - t / 2f, stemTop),
        size = Size(t, stemBot - stemTop),
    )
    if (locked) {
        val barY = size.height * 0.78f
        val barHalf = headHalf * 0.55f
        drawRect(
            color = color,
            topLeft = Offset(cx - barHalf, barY - t / 2f),
            size = Size(barHalf * 2f, t),
        )
    }
}

/** WP SIP backspace — left-pointing key body with an X. */
private fun DrawScope.drawBackspaceGlyph(color: Color, stroke: Stroke) {
    val s = size.minDimension
    val left = size.width * 0.18f
    val right = size.width * 0.82f
    val top = size.height * 0.30f
    val bottom = size.height * 0.70f
    val midY = (top + bottom) / 2f
    // SIP backspace reads thin vs shift/enter chrome (~WP8.1 outline weight).
    val thin = Stroke(
        width = stroke.width * 0.72f,
        cap = StrokeCap.Butt,
        join = StrokeJoin.Miter,
        miter = 4f,
    )
    val body = Path().apply {
        moveTo(left, midY)
        lineTo(left + s * 0.16f, top)
        lineTo(right, top)
        lineTo(right, bottom)
        lineTo(left + s * 0.16f, bottom)
        close()
    }
    drawPath(body, color, style = thin)
    val xArm = s * 0.09f
    val xCx = left + (right - left) * 0.58f
    drawLine(color, Offset(xCx - xArm, midY - xArm), Offset(xCx + xArm, midY + xArm), thin.width, StrokeCap.Butt)
    drawLine(color, Offset(xCx + xArm, midY - xArm), Offset(xCx - xArm, midY + xArm), thin.width, StrokeCap.Butt)
}

/** WP SIP enter / return arrow. */
private fun DrawScope.drawEnterGlyph(color: Color, stroke: Stroke) {
    val s = size.minDimension
    val thick = Stroke(
        width = stroke.width * 1.1f,
        cap = StrokeCap.Butt,
        join = StrokeJoin.Miter,
        miter = 4f,
    )
    val right = size.width * 0.72f
    val left = size.width * 0.28f
    val top = size.height * 0.32f
    val midY = size.height * 0.58f
    // Single path so Miter join fills the outer corner (two drawLine butts leave a gap).
    val shaft = Path().apply {
        moveTo(right, top)
        lineTo(right, midY)
        lineTo(left, midY)
    }
    drawPath(shaft, color, style = thick)
    val head = Path().apply {
        moveTo(left + s * 0.14f, midY - s * 0.12f)
        lineTo(left, midY)
        lineTo(left + s * 0.14f, midY + s * 0.12f)
    }
    drawPath(head, color, style = thick)
}

/** WP SIP emoji / emoticon key — thinner outline, slightly larger face. */
private fun DrawScope.drawEmojiGlyph(color: Color, stroke: Stroke) {
    val s = size.minDimension
    val cx = size.width / 2f
    val cy = size.height / 2f
    val thin = Stroke(
        width = stroke.width * 0.65f,
        cap = StrokeCap.Butt,
        join = StrokeJoin.Miter,
        miter = 4f,
    )
    val r = s * 0.42f
    drawCircle(color, r, Offset(cx, cy), style = thin)
    val eyeY = cy - s * 0.09f
    val eyeR = s * 0.042f
    drawCircle(color, eyeR, Offset(cx - s * 0.14f, eyeY))
    drawCircle(color, eyeR, Offset(cx + s * 0.14f, eyeY))
    val smile = Path().apply {
        moveTo(cx - s * 0.18f, cy + s * 0.09f)
        quadraticBezierTo(cx, cy + s * 0.28f, cx + s * 0.18f, cy + s * 0.09f)
    }
    drawPath(smile, color, style = thin)
}

/**
 * Circular undo/redo arrow (Segoe-style). [redo] is clockwise with the head at top-left;
 * undo is the horizontal mirror.
 */
private fun DrawScope.drawUndoRedoGlyph(color: Color, stroke: Stroke, redo: Boolean) {
    val s = size.minDimension
    val cx = size.width / 2f
    val cy = size.height / 2f
    withTransform({
        if (!redo) {
            scale(scaleX = -1f, scaleY = 1f, pivot = Offset(cx, cy))
        }
    }) {
        val r = s * 0.28f
        val thick = Stroke(
            width = stroke.width * 1.2f,
            cap = StrokeCap.Butt,
            join = StrokeJoin.Miter,
            miter = 4f,
        )
        // Compose angles: 0° = 3 o'clock, positive = clockwise. Head sits at top-left.
        val endAngle = 228f
        val sweep = 300f
        val startAngle = endAngle - sweep
        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(cx - r, cy - r),
            size = Size(r * 2f, r * 2f),
            style = thick,
        )
        val endRad = Math.toRadians(endAngle.toDouble())
        val cosE = kotlin.math.cos(endRad).toFloat()
        val sinE = kotlin.math.sin(endRad).toFloat()
        val ex = cx + r * cosE
        val ey = cy + r * sinE
        // Clockwise tangent at endAngle.
        val tx = -sinE
        val ty = cosE
        val nx = -ty
        val ny = tx
        val headLen = s * 0.18f
        val headHalf = s * 0.12f
        val tipX = ex + tx * headLen * 0.35f
        val tipY = ey + ty * headLen * 0.35f
        val baseX = tipX - tx * headLen
        val baseY = tipY - ty * headLen
        val head = Path().apply {
            moveTo(tipX, tipY)
            lineTo(baseX + nx * headHalf, baseY + ny * headHalf)
            lineTo(baseX - nx * headHalf, baseY - ny * headHalf)
            close()
        }
        drawPath(head, color)
    }
}

private fun DrawScope.drawSettingsGlyph(color: Color, stroke: Stroke) {
    val s = size.minDimension
    val cx = size.width / 2f
    val cy = size.height / 2f
    drawCircle(color, s * 0.10f, Offset(cx, cy), style = stroke)
    drawCircle(color, s * 0.26f, Offset(cx, cy), style = stroke)
    for (i in 0 until 6) {
        val a = Math.toRadians((i * 60).toDouble()).toFloat()
        val inner = s * 0.26f
        val outer = s * 0.34f
        drawLine(
            color,
            Offset(cx + kotlin.math.cos(a) * inner, cy + kotlin.math.sin(a) * inner),
            Offset(cx + kotlin.math.cos(a) * outer, cy + kotlin.math.sin(a) * outer),
            stroke.width,
            StrokeCap.Butt,
        )
    }
}

private fun DrawScope.drawClipboardGlyph(color: Color, stroke: Stroke) {
    val s = size.minDimension
    val left = size.width * 0.28f
    val top = size.height * 0.28f
    drawRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(s * 0.44f, s * 0.52f),
        style = stroke,
    )
    drawRect(
        color = color,
        topLeft = Offset(size.width * 0.36f, size.height * 0.20f),
        size = Size(s * 0.28f, s * 0.12f),
        style = stroke,
    )
}

private fun DrawScope.drawCopyGlyph(color: Color, stroke: Stroke) {
    val s = size.minDimension
    drawRect(
        color = color,
        topLeft = Offset(size.width * 0.34f, size.height * 0.26f),
        size = Size(s * 0.38f, s * 0.44f),
        style = stroke,
    )
    drawRect(
        color = color,
        topLeft = Offset(size.width * 0.26f, size.height * 0.34f),
        size = Size(s * 0.38f, s * 0.44f),
        style = stroke,
    )
}

private fun DrawScope.drawCutGlyph(color: Color, stroke: Stroke) {
    val s = size.minDimension
    val cx = size.width / 2f
    val cy = size.height / 2f
    drawCircle(color, s * 0.08f, Offset(cx - s * 0.14f, cy + s * 0.12f), style = stroke)
    drawCircle(color, s * 0.08f, Offset(cx + s * 0.14f, cy + s * 0.12f), style = stroke)
    drawLine(color, Offset(cx - s * 0.14f, cy + s * 0.12f), Offset(cx + s * 0.18f, cy - s * 0.22f), stroke.width, StrokeCap.Butt)
    drawLine(color, Offset(cx + s * 0.14f, cy + s * 0.12f), Offset(cx - s * 0.18f, cy - s * 0.22f), stroke.width, StrokeCap.Butt)
}

private fun DrawScope.drawPasteGlyph(color: Color, stroke: Stroke) {
    drawClipboardGlyph(color, stroke)
    val s = size.minDimension
    drawLine(
        color,
        Offset(size.width * 0.42f, size.height * 0.48f),
        Offset(size.width * 0.58f, size.height * 0.48f),
        stroke.width,
        StrokeCap.Butt,
    )
    drawLine(
        color,
        Offset(size.width * 0.42f, size.height * 0.58f),
        Offset(size.width * 0.54f, size.height * 0.58f),
        stroke.width,
        StrokeCap.Butt,
    )
}

private fun DrawScope.drawSelectAllGlyph(color: Color, stroke: Stroke) {
    val s = size.minDimension
    drawRect(
        color = color,
        topLeft = Offset(size.width * 0.26f, size.height * 0.26f),
        size = Size(s * 0.48f, s * 0.48f),
        style = stroke,
    )
    drawCheckGlyph(color)
}

private fun DrawScope.drawLanguageGlyph(color: Color, stroke: Stroke) {
    val s = size.minDimension
    val cx = size.width / 2f
    val cy = size.height / 2f
    drawCircle(color, s * 0.28f, Offset(cx, cy), style = stroke)
    drawOval(
        color = color,
        topLeft = Offset(cx - s * 0.14f, cy - s * 0.28f),
        size = Size(s * 0.28f, s * 0.56f),
        style = stroke,
    )
    drawLine(color, Offset(cx - s * 0.28f, cy), Offset(cx + s * 0.28f, cy), stroke.width, StrokeCap.Butt)
}

private fun DrawScope.drawKeyboardHideGlyph(color: Color, stroke: Stroke) {
    // WP8.1 SIP dismiss is a lone downward chevron (not a keyboard body + arrow).
    drawChevronGlyph(color, stroke, direction = 1)
}

private fun DrawScope.drawSendGlyph(color: Color) {
    val s = size.minDimension
    val path = Path().apply {
        moveTo(size.width * 0.22f, size.height * 0.28f)
        lineTo(size.width * 0.78f, size.height * 0.50f)
        lineTo(size.width * 0.22f, size.height * 0.72f)
        lineTo(size.width * 0.22f, size.height * 0.58f)
        lineTo(size.width * 0.52f, size.height * 0.50f)
        lineTo(size.width * 0.22f, size.height * 0.42f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawAutocorrectGlyph(color: Color, stroke: Stroke) {
    val s = size.minDimension
    drawLine(
        color,
        Offset(size.width * 0.28f, size.height * 0.68f),
        Offset(size.width * 0.42f, size.height * 0.32f),
        stroke.width,
        StrokeCap.Butt,
    )
    drawLine(
        color,
        Offset(size.width * 0.42f, size.height * 0.32f),
        Offset(size.width * 0.56f, size.height * 0.68f),
        stroke.width,
        StrokeCap.Butt,
    )
    drawLine(
        color,
        Offset(size.width * 0.34f, size.height * 0.54f),
        Offset(size.width * 0.50f, size.height * 0.54f),
        stroke.width,
        StrokeCap.Butt,
    )
    drawCircle(color, s * 0.06f, Offset(size.width * 0.68f, size.height * 0.38f), style = stroke)
}

/**
 * WP8.1 chevron — wide shallow V with flat (axis-aligned) end caps and a sharp tip.
 * [direction]: 0 up, 1 down, 2 left, 3 right.
 */
private fun DrawScope.drawChevronGlyph(color: Color, stroke: Stroke, direction: Int) {
    val s = size.minDimension
    val cx = size.width / 2f
    val cy = size.height / 2f
    val halfW = s * 0.30f
    val halfH = s * 0.16f
    val t = stroke.width * 1.35f
    // Build a filled down-chevron with horizontal end caps, then rotate into place.
    val top = cy - halfH
    val bot = cy + halfH
    val left = cx - halfW
    val right = cx + halfW
    val armDx = halfW
    val armDy = halfH * 2f
    val theta = kotlin.math.atan2(armDy, armDx)
    val horizInset = t / kotlin.math.sin(theta).toFloat()
    val halfTip = kotlin.math.atan2(armDx, armDy)
    val tipLift = t / kotlin.math.sin(halfTip).toFloat()
    val down = Path().apply {
        moveTo(left, top)
        lineTo(cx, bot)
        lineTo(right, top)
        lineTo(right - horizInset, top)
        lineTo(cx, bot - tipLift)
        lineTo(left + horizInset, top)
        close()
    }
    val degrees = when (direction) {
        0 -> 180f
        1 -> 0f
        2 -> 90f
        else -> -90f
    }
    rotate(degrees, pivot = Offset(cx, cy)) {
        drawPath(down, color)
    }
}

internal fun DrawScope.drawPlayGlyph(color: Color) {
    val d = size.minDimension
    val height = d * 0.34f
    val width = d * 0.28f
    val left = size.width / 2f - width / 2f + d * 0.03f
    val cy = size.height / 2f
    val path = Path().apply {
        moveTo(left, cy - height / 2f)
        lineTo(left + width, cy)
        lineTo(left, cy + height / 2f)
        close()
    }
    drawPath(path, color)
}

internal fun DrawScope.drawPauseGlyph(color: Color) {
    val d = size.minDimension
    val height = d * 0.34f
    val barWidth = d * MetroSystemIconStrokeFraction
    val gap = d * MetroSystemIconStrokeFraction
    val cy = size.height / 2f
    val left = size.width / 2f - (barWidth * 2f + gap) / 2f
    drawRect(color, Offset(left, cy - height / 2f), Size(barWidth, height))
    drawRect(color, Offset(left + barWidth + gap, cy - height / 2f), Size(barWidth, height))
}

internal fun DrawScope.drawSkipGlyph(color: Color, forward: Boolean) {
    val d = size.minDimension
    val cx = size.width / 2f
    val cy = size.height / 2f
    val height = d * 0.34f
    val barWidth = d * 0.07f
    val triangleWidth = d * 0.19f
    val gap = d * 0.02f
    val totalWidth = barWidth + triangleWidth * 2f + gap * 2f
    var x = cx - totalWidth / 2f

    fun triangle(left: Float) {
        val apexX = if (forward) left + triangleWidth else left
        val baseX = if (forward) left else left + triangleWidth
        val path = Path().apply {
            moveTo(apexX, cy)
            lineTo(baseX, cy - height / 2f)
            lineTo(baseX, cy + height / 2f)
            close()
        }
        drawPath(path, color)
    }

    if (forward) {
        triangle(x)
        x += triangleWidth + gap
        triangle(x)
        x += triangleWidth + gap
        drawRect(color, Offset(x, cy - height / 2f), Size(barWidth, height))
    } else {
        drawRect(color, Offset(x, cy - height / 2f), Size(barWidth, height))
        x += barWidth + gap
        triangle(x)
        x += triangleWidth + gap
        triangle(x)
    }
}
