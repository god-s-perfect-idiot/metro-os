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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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

@Composable
fun PhoneCallIcon(
    modifier: Modifier = Modifier,
    color: Color = MetroTheme.colors.accent,
    showCircle: Boolean = true,
) {
    Canvas(modifier = modifier.size(40.dp)) {
        val strokeWidth = size.minDimension * 0.06f
        if (showCircle) {
            drawCircle(
                color = color,
                radius = size.minDimension * 0.42f,
                style = Stroke(width = strokeWidth),
            )
        }
        val path = Path().apply {
            moveTo(size.width * 0.62f, size.height * 0.68f)
            cubicTo(
                size.width * 0.52f, size.height * 0.58f,
                size.width * 0.42f, size.height * 0.48f,
                size.width * 0.32f, size.height * 0.38f,
            )
            lineTo(size.width * 0.24f, size.height * 0.46f)
            cubicTo(
                size.width * 0.18f, size.height * 0.52f,
                size.width * 0.18f, size.height * 0.62f,
                size.width * 0.26f, size.height * 0.68f,
            )
            cubicTo(
                size.width * 0.34f, size.height * 0.76f,
                size.width * 0.44f, size.height * 0.76f,
                size.width * 0.50f, size.height * 0.70f,
            )
            lineTo(size.width * 0.58f, size.height * 0.62f)
            cubicTo(
                size.width * 0.64f, size.height * 0.56f,
                size.width * 0.64f, size.height * 0.48f,
                size.width * 0.58f, size.height * 0.42f,
            )
            close()
        }
        drawPath(
            path,
            color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
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
