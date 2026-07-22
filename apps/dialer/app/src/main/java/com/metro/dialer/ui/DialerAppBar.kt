package com.metro.dialer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroSystemIcon
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTheme

@Composable
fun DialerAppBar(
    showDialPad: Boolean,
    showSearch: Boolean,
    onDialPadClick: () -> Unit,
    onPeopleClick: () -> Unit,
    onSearchClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MetroTheme.colors.accent.copy(alpha = 0.08f))
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (showDialPad) {
                DialPadIconButton(onClick = onDialPadClick)
            }
            PeopleIconButton(onClick = onPeopleClick)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (showSearch) {
                MetroCircleIconButton(
                    type = MetroSystemIconType.Search,
                    onClick = onSearchClick,
                    contentDescription = "search",
                )
            }
            MetroCircleIconButton(
                type = MetroSystemIconType.More,
                onClick = onMoreClick,
                contentDescription = "more",
            )
        }
    }
}

@Composable
private fun DialPadIconButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .semantics {
                role = Role.Button
                contentDescription = "dial pad"
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(26.dp)) {
            val stroke = Stroke(width = size.minDimension * 0.08f, cap = StrokeCap.Round)
            val gap = size.width * 0.28f
            val tile = size.width * 0.22f
            for (row in 0..2) {
                for (col in 0..2) {
                    val left = col * gap
                    val top = row * gap
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(tile, tile),
                        style = stroke,
                    )
                }
            }
        }
    }
}

@Composable
private fun PeopleIconButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .semantics {
                role = Role.Button
                contentDescription = "people"
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(26.dp)) {
            val stroke = Stroke(width = size.minDimension * 0.08f, cap = StrokeCap.Round)
            val cx = size.width / 2f
            drawCircle(color = Color.White, radius = size.minDimension * 0.14f, style = stroke)
            val path = Path().apply {
                moveTo(cx - size.minDimension * 0.22f, size.height * 0.72f)
                quadraticBezierTo(
                    cx,
                    size.height * 0.48f,
                    cx + size.minDimension * 0.22f,
                    size.height * 0.72f,
                )
            }
            drawPath(path, Color.White, style = stroke)
        }
    }
}

/** WP8.1 Phone handset glyph — same path as [ic_launcher_foreground]. */
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

/**
 * WP8.1 Messaging :-) bubble — same path as launcher [ic_system_messaging], without the
 * tile phone overlay.
 */
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

private const val AppBarCallGlyphSizeDp = 28
private const val AppBarMessageGlyphSizeDp = 26
private const val AppBarCallGlyphScale = 0.72f
private const val AppBarMessageGlyphScale = 0.66f

private fun DrawScope.drawViewportGlyph(path: Path, color: Color, glyphScale: Float) {
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

@Composable
fun PhoneCallIcon(
    modifier: Modifier = Modifier.size(40.dp),
    color: Color = MetroTheme.colors.accent,
    showCircle: Boolean = true,
    glyphScale: Float = 0.62f,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.05f
        if (showCircle) {
            val circleRadius = size.minDimension * 0.42f - strokeWidth
            drawCircle(
                color = color,
                radius = circleRadius,
                style = Stroke(width = strokeWidth),
            )
        }
        drawViewportGlyph(phoneHandsetPath, color, glyphScale)
    }
}

@Composable
fun MessageIcon(
    modifier: Modifier = Modifier.size(40.dp),
    color: Color = MetroTheme.colors.accent,
    glyphScale: Float = 0.62f,
) {
    Canvas(modifier = modifier) {
        drawViewportGlyph(messagingBubblePath, color, glyphScale)
    }
}

@Composable
fun AppBarCallGlyph(color: Color) {
    PhoneCallIcon(
        modifier = Modifier.size(AppBarCallGlyphSizeDp.dp),
        color = color,
        showCircle = false,
        glyphScale = AppBarCallGlyphScale,
    )
}

@Composable
fun AppBarMessageGlyph(color: Color) {
    MessageIcon(
        modifier = Modifier.size(AppBarMessageGlyphSizeDp.dp),
        color = color,
        glyphScale = AppBarMessageGlyphScale,
    )
}

@Composable
fun ListDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.2f)),
    )
}
