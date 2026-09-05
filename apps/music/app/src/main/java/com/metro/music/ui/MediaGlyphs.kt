package com.metro.music.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroMediaGlyph
import com.metro.ui.MetroMediaGlyphButton
import com.metro.ui.MetroMediaGlyphIcon
import com.metro.ui.MetroMediaTransportButton
import com.metro.ui.MetroMediaTransportButtonSize
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTheme

/** App alias for the shared suite media glyph set. */
typealias MediaGlyph = MetroMediaGlyph

@Composable
fun MediaGlyphIcon(
    glyph: MediaGlyph,
    modifier: Modifier = Modifier,
    glyphSize: Dp = 26.dp,
    color: Color = MetroTheme.colors.primaryText,
) {
    MetroMediaGlyphIcon(glyph = glyph, modifier = modifier, glyphSize = glyphSize, color = color)
}

@Composable
fun MediaGlyphButton(
    glyph: MediaGlyph,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    color: Color = MetroTheme.colors.primaryText,
    touchTarget: Dp = 48.dp,
    glyphSize: Dp = 26.dp,
) {
    MetroMediaGlyphButton(
        glyph = glyph,
        onClick = onClick,
        contentDescription = contentDescription,
        modifier = modifier,
        color = color,
        touchTarget = touchTarget,
        glyphSize = glyphSize,
    )
}

val MediaTransportButtonSize = MetroMediaTransportButtonSize

@Composable
fun MediaTransportButton(
    glyph: MediaGlyph,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    buttonSize: Dp = MediaTransportButtonSize,
    color: Color = MetroTheme.colors.primaryText,
) {
    MetroMediaTransportButton(
        glyph = glyph,
        onClick = onClick,
        contentDescription = contentDescription,
        modifier = modifier,
        buttonSize = buttonSize,
        color = color,
    )
}

/** Transport ring using toolkit chrome play / pause / previous / next. */
@Composable
fun MediaTransportButton(
    type: MetroSystemIconType,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    buttonSize: Dp = MediaTransportButtonSize,
    color: Color = MetroTheme.colors.primaryText,
) {
    MetroMediaTransportButton(
        type = type,
        onClick = onClick,
        contentDescription = contentDescription,
        modifier = modifier,
        buttonSize = buttonSize,
        color = color,
    )
}
