package com.metro.camera.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class CameraChromeGlyph {
    Gallery,
    Flash,
    FlashOff,
    FlipCamera,
    Lens,
    Iso,
    Photo,
    Burst,
    Video,
    More,
}

@Composable
fun CameraChromeIcon(
    glyph: CameraChromeGlyph,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    color: Color = Color.White,
) {
    Canvas(modifier = modifier.size(size)) {
        drawCameraChromeGlyph(glyph, color)
    }
}

fun DrawScope.drawCameraChromeGlyph(glyph: CameraChromeGlyph, color: Color) {
    when (glyph) {
        CameraChromeGlyph.Gallery -> drawSvgFill(galleryPath, viewBox = 1024f, color = color)
        CameraChromeGlyph.Flash -> drawSvgFill(flashPath, viewBox = 24f, color = color)
        CameraChromeGlyph.FlashOff -> {
            drawSvgFill(flashPath, viewBox = 24f, color = color)
            val stroke = size.minDimension * 0.08f
            drawLine(
                color = color,
                start = Offset(size.width * 0.22f, size.height * 0.78f),
                end = Offset(size.width * 0.78f, size.height * 0.22f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
        CameraChromeGlyph.FlipCamera -> {
            drawSvgFill(flipLensPath, viewBox = 24f, color = color)
            drawSvgFill(flipArrowsPath, viewBox = 24f, color = color)
        }
        CameraChromeGlyph.Lens -> drawLensStroke(color)
        CameraChromeGlyph.Iso -> {
            drawSvgFill(isoPath, viewBox = 24f, color = color)
        }
        CameraChromeGlyph.Photo -> {
            drawSvgFill(photoBodyPath, viewBox = 24f, color = color)
            // Center shutter pupil from the SVG <circle cx="12" cy="12" r="3"/>.
            val scale = size.minDimension / 24f * 0.82f
            drawCircle(
                color = color,
                radius = 3f * scale,
                center = Offset(size.width / 2f, size.height / 2f),
            )
        }
        CameraChromeGlyph.Burst -> drawSvgFill(burstPath, viewBox = 24f, color = color)
        CameraChromeGlyph.Video -> drawSvgFill(videoPath, viewBox = 24f, color = color)
        CameraChromeGlyph.More -> drawMore(color)
    }
}

private fun DrawScope.drawSvgFill(path: Path, viewBox: Float, color: Color, inset: Float = 0.82f) {
    val scale = size.minDimension / viewBox * inset
    val cx = size.width / 2f
    val cy = size.height / 2f
    val half = viewBox / 2f
    withTransform({
        translate(left = cx, top = cy)
        scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        translate(left = -half, top = -half)
    }) {
        drawPath(path, color = color, style = Fill)
    }
}

private fun DrawScope.drawLensStroke(color: Color) {
    val viewBox = 24f
    val scale = size.minDimension / viewBox * 0.82f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val stroke = Stroke(
        width = 1.5f,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
    )
    withTransform({
        translate(left = cx, top = cy)
        scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        translate(left = -12f, top = -12f)
    }) {
        drawPath(lensOuterPath, color = color, style = stroke)
        drawPath(lensInnerPath, color = color, style = stroke)
    }
}

/** Horizontal ellipsis — never orientation-rotated. */
private fun DrawScope.drawMore(color: Color) {
    val r = size.minDimension * 0.07f
    val y = size.height * 0.5f
    val xs = listOf(size.width * 0.28f, size.width * 0.5f, size.width * 0.72f)
    xs.forEach { x -> drawCircle(color = color, radius = r, center = Offset(x, y)) }
}

// Gallery — 1024 viewBox (image file with landscape + sun).
private const val GALLERY_PATH =
    "m553.1 509.1l-77.8 99.2l-41.1-52.4a8 8 0 0 0-12.6 0l-99.8 127.2a7.98 7.98 0 0 0 6.3 12.9H696c6.7 0 10.4-7.7 6.3-12.9l-136.5-174a8.1 8.1 0 0 0-12.7 0M360 442a40 40 0 1 0 80 0a40 40 0 1 0-80 0m494.6-153.4L639.4 73.4c-6-6-14.1-9.4-22.6-9.4H192c-17.7 0-32 14.3-32 32v832c0 17.7 14.3 32 32 32h640c17.7 0 32-14.3 32-32V311.3c0-8.5-3.4-16.7-9.4-22.7M790.2 326H602V137.8zm1.8 562H232V136h302v216a42 42 0 0 0 42 42h216z"

// Flash — 24 viewBox.
private const val FLASH_PATH = "M7 2v11h3v9l7-12h-4l4-8z"

// Flip — 24 viewBox (lens + swap arrows).
private const val FLIP_LENS_PATH =
    "M9 12c0 1.66 1.34 3 3 3s3-1.34 3-3s-1.34-3-3-3s-3 1.34-3 3m4 0c0 .55-.45 1-1 1s-1-.45-1-1s.45-1 1-1s1 .45 1 1"
private const val FLIP_ARROWS_PATH =
    "M8 10V8H5.09C6.47 5.61 9.05 4 12 4c3.72 0 6.85 2.56 7.74 6h2.06c-.93-4.56-4.96-8-9.8-8c-3.27 0-6.18 1.58-8 4.01V4H2v6zm8 4v2h2.91c-1.38 2.39-3.96 4-6.91 4c-3.72 0-6.85-2.56-7.74-6H2.2c.93 4.56 4.96 8 9.8 8c3.27 0 6.18-1.58 8-4.01V20h2v-6z"

// Lens — 24 viewBox, stroked arcs.
private const val LENS_OUTER_PATH =
    "M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2S2 6.477 2 12s4.477 10 10 10"
private const val LENS_INNER_PATH =
    "M17.197 9q-.15-.259-.323-.5m.937 5a6.01 6.01 0 0 1-4.311 4.311"

// ISO — 24 viewBox.
private const val ISO_PATH =
    "M21 3H3v18h18zM5.5 7.5h2v-2H9v2h2V9H9v2H7.5V9h-2zM19 19H5L19 5zm-2-2v-1.5h-5V17z"

// Shutter / photo — 24 viewBox (body; pupil drawn as a circle).
private const val PHOTO_BODY_PATH =
    "M9 2L7.17 4H2v16h20V4h-5.17L15 2zm3 15c-2.76 0-5-2.24-5-5s2.24-5 5-5s5 2.24 5 5s-2.24 5-5 5"

// Burst — 24 viewBox.
private const val BURST_PATH =
    "M1 19V5h2v14zm4 0V5h2v14zm4 0V5h14v14zm2-2h10V7H11zm1-2h8l-2.6-3.5l-1.9 2.5l-1.4-1.85zm-1 2V7z"

// Video — 24 viewBox.
private const val VIDEO_PATH =
    "M18 10.48V4H2v16h16v-6.48l4 3.98v-11zM10 8c1.1 0 2 .9 2 2s-.9 2-2 2s-2-.9-2-2s.9-2 2-2m4 8H6v-.57c0-.81.48-1.53 1.22-1.85a6.95 6.95 0 0 1 5.56 0A2.01 2.01 0 0 1 14 15.43z"

private fun parsePath(data: String, evenOdd: Boolean = false): Path =
    PathParser().parsePathString(data).toPath().also {
        if (evenOdd) it.fillType = PathFillType.EvenOdd
    }

private val galleryPath: Path by lazy { parsePath(GALLERY_PATH, evenOdd = true) }
private val flashPath: Path by lazy { parsePath(FLASH_PATH) }
private val flipLensPath: Path by lazy { parsePath(FLIP_LENS_PATH, evenOdd = true) }
private val flipArrowsPath: Path by lazy { parsePath(FLIP_ARROWS_PATH) }
private val lensOuterPath: Path by lazy { parsePath(LENS_OUTER_PATH) }
private val lensInnerPath: Path by lazy { parsePath(LENS_INNER_PATH) }
private val isoPath: Path by lazy { parsePath(ISO_PATH, evenOdd = true) }
private val photoBodyPath: Path by lazy { parsePath(PHOTO_BODY_PATH, evenOdd = true) }
private val burstPath: Path by lazy { parsePath(BURST_PATH, evenOdd = true) }
private val videoPath: Path by lazy { parsePath(VIDEO_PATH, evenOdd = true) }
