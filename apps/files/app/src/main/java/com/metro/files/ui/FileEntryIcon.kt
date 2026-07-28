package com.metro.files.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.files.data.FileEntry
import com.metro.files.data.FileIconKind
import com.metro.files.data.FilesLogic
import com.metro.ui.MetroColors
import com.metro.ui.MetroFontFamily
import com.metro.ui.MetroTheme

private val FileIconSize = 48.dp
private val FileTileGrayDark = Color(0xFF3A3A3A)
private val FileTileGrayLight = Color(0xFFC8C8C8)

/**
 * WP8.1 Files-style leading tile:
 * - Volumes: accent square with phone / SD card glyph
 * - Folders: accent square with child-count badge
 * - Files: gray square with document / media glyph + Office-style type badge when applicable
 */
@Composable
fun FileEntryIcon(
    entry: FileEntry,
    modifier: Modifier = Modifier,
    size: Dp = FileIconSize,
) {
    val kind = FilesLogic.iconKind(entry)
    when (kind) {
        FileIconKind.PHONE, FileIconKind.SD_CARD -> {
            VolumeTile(
                kind = kind,
                modifier = modifier.size(size),
            )
        }
        FileIconKind.FOLDER -> {
            FolderTile(
                countLabel = FilesLogic.folderCountLabel(entry.childCount),
                modifier = modifier.size(size),
            )
        }
        else -> {
            FileTypeTile(
                kind = kind,
                modifier = modifier.size(size),
            )
        }
    }
}

@Composable
private fun VolumeTile(
    kind: FileIconKind,
    modifier: Modifier = Modifier,
) {
    val accent = MetroTheme.colors.accent
    val glyph = MetroColors.tileContentColor(accent)
    Box(modifier = modifier.background(accent)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (kind) {
                FileIconKind.SD_CARD -> drawSdCardGlyph(glyph)
                else -> drawPhoneGlyph(glyph)
            }
        }
    }
}

@Composable
private fun FolderTile(
    countLabel: String?,
    modifier: Modifier = Modifier,
) {
    val accent = MetroTheme.colors.accent
    val content = MetroColors.tileContentColor(accent)
    Box(modifier = modifier.background(accent)) {
        if (countLabel != null) {
            BasicText(
                text = countLabel,
                style = TextStyle(
                    color = content,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MetroFontFamily,
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 2.dp),
            )
        }
    }
}

@Composable
private fun FileTypeTile(
    kind: FileIconKind,
    modifier: Modifier = Modifier,
) {
    val dark = MetroTheme.colors.background == MetroColors.DarkBackground
    val tileFill = if (dark) FileTileGrayDark else FileTileGrayLight
    val glyphColor = if (dark) Color.White else Color(0xFF1A1A1A)
    val lineColor = if (dark) FileTileGrayDark else FileTileGrayLight
    val badge = badgeFor(kind)

    Box(modifier = modifier.background(tileFill)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (kind) {
                FileIconKind.MUSIC -> drawMusicNote(glyphColor)
                FileIconKind.PICTURE -> drawPictureGlyph(glyphColor, lineColor)
                FileIconKind.VIDEO -> drawVideoGlyph(glyphColor)
                else -> drawDocumentGlyph(glyphColor, lineColor)
            }
            if (badge != null) {
                drawTypeBadgeFill(badge.color)
            }
        }
        if (badge != null) {
            BasicText(
                text = badge.label,
                style = TextStyle(
                    color = Color.White,
                    fontSize = if (badge.label.length > 1) 7.sp else 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MetroFontFamily,
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 3.dp),
            )
        }
    }
}

private data class TypeBadge(val label: String, val color: Color)

private fun badgeFor(kind: FileIconKind): TypeBadge? = when (kind) {
    FileIconKind.WORD -> TypeBadge("W", MetroColors.AccentBlue)
    FileIconKind.EXCEL -> TypeBadge("X", MetroColors.AccentGreen)
    FileIconKind.POWERPOINT -> TypeBadge("P", MetroColors.AccentOrange)
    FileIconKind.ONENOTE -> TypeBadge("N", MetroColors.AccentViolet)
    FileIconKind.PDF -> TypeBadge("PDF", MetroColors.AccentRed)
    else -> null
}

private fun DrawScope.drawDocumentGlyph(fill: Color, lineColor: Color) {
    val padX = size.width * 0.22f
    val padY = size.height * 0.16f
    val docW = size.width - padX * 2f
    val docH = size.height - padY * 2f
    val fold = docW * 0.28f
    val path = Path().apply {
        moveTo(padX, padY)
        lineTo(padX + docW - fold, padY)
        lineTo(padX + docW, padY + fold)
        lineTo(padX + docW, padY + docH)
        lineTo(padX, padY + docH)
        close()
    }
    drawPath(path, color = fill.copy(alpha = 0.92f))
    val foldPath = Path().apply {
        moveTo(padX + docW - fold, padY)
        lineTo(padX + docW, padY + fold)
        lineTo(padX + docW - fold, padY + fold)
        close()
    }
    drawPath(foldPath, color = fill.copy(alpha = 0.55f))
    val lineLeft = padX + docW * 0.18f
    val lineRight = padX + docW * 0.72f
    val lineStartY = padY + docH * 0.42f
    val stroke = size.minDimension * 0.045f
    for (i in 0..2) {
        val y = lineStartY + i * (docH * 0.14f)
        drawLine(
            color = lineColor,
            start = Offset(lineLeft, y),
            end = Offset(if (i == 2) lineLeft + (lineRight - lineLeft) * 0.55f else lineRight, y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawTypeBadgeFill(color: Color) {
    val badgeW = size.width * 0.40f
    val badgeH = size.height * 0.30f
    val left = size.width - badgeW - size.width * 0.05f
    val top = size.height - badgeH - size.height * 0.05f
    drawRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(badgeW, badgeH),
    )
}

private fun DrawScope.drawMusicNote(color: Color) {
    val cx = size.width * 0.42f
    val cy = size.height * 0.58f
    val r = size.minDimension * 0.11f
    drawCircle(color = color, radius = r, center = Offset(cx, cy))
    drawCircle(color = color, radius = r * 0.9f, center = Offset(cx + r * 2.4f, cy - r * 0.35f))
    val stemW = size.minDimension * 0.055f
    drawLine(
        color = color,
        start = Offset(cx + r * 0.75f, cy),
        end = Offset(cx + r * 0.75f, cy - r * 3.2f),
        strokeWidth = stemW,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = Offset(cx + r * 2.4f + r * 0.65f, cy - r * 0.35f),
        end = Offset(cx + r * 2.4f + r * 0.65f, cy - r * 3.55f),
        strokeWidth = stemW,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = Offset(cx + r * 0.75f, cy - r * 3.2f),
        end = Offset(cx + r * 2.4f + r * 0.65f, cy - r * 3.55f),
        strokeWidth = stemW,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawPictureGlyph(frame: Color, fill: Color) {
    val left = size.width * 0.18f
    val top = size.height * 0.22f
    val w = size.width * 0.64f
    val h = size.height * 0.56f
    drawRect(color = frame, topLeft = Offset(left, top), size = Size(w, h))
    drawCircle(
        color = fill,
        radius = w * 0.1f,
        center = Offset(left + w * 0.28f, top + h * 0.3f),
    )
    val mountain = Path().apply {
        moveTo(left + w * 0.08f, top + h * 0.88f)
        lineTo(left + w * 0.36f, top + h * 0.48f)
        lineTo(left + w * 0.52f, top + h * 0.66f)
        lineTo(left + w * 0.72f, top + h * 0.4f)
        lineTo(left + w * 0.94f, top + h * 0.88f)
        close()
    }
    drawPath(mountain, color = fill)
}

private fun DrawScope.drawVideoGlyph(color: Color) {
    val left = size.width * 0.22f
    val top = size.height * 0.28f
    val w = size.width * 0.42f
    val h = size.height * 0.44f
    drawRect(color = color, topLeft = Offset(left, top), size = Size(w, h))
    val play = Path().apply {
        moveTo(left + w + size.width * 0.04f, top + h * 0.12f)
        lineTo(left + w + size.width * 0.22f, top + h * 0.5f)
        lineTo(left + w + size.width * 0.04f, top + h * 0.88f)
        close()
    }
    drawPath(play, color = color)
}

/** Portrait phone silhouette for the volume root “phone” tile. */
private fun DrawScope.drawPhoneGlyph(color: Color) {
    val left = size.width * 0.32f
    val top = size.height * 0.14f
    val w = size.width * 0.36f
    val h = size.height * 0.72f
    val body = Path().apply {
        fillType = PathFillType.EvenOdd
        moveTo(left + w * 0.12f, top)
        lineTo(left + w * 0.88f, top)
        lineTo(left + w, top + h * 0.06f)
        lineTo(left + w, top + h * 0.94f)
        lineTo(left + w * 0.88f, top + h)
        lineTo(left + w * 0.12f, top + h)
        lineTo(left, top + h * 0.94f)
        lineTo(left, top + h * 0.06f)
        close()
        // Earpiece cutout
        addRect(
            Rect(
                left = left + w * 0.28f,
                top = top + h * 0.09f,
                right = left + w * 0.72f,
                bottom = top + h * 0.15f,
            ),
        )
        // Home button cutout
        addOval(
            Rect(
                left = left + w * 0.38f,
                top = top + h * 0.82f,
                right = left + w * 0.62f,
                bottom = top + h * 0.92f,
            ),
        )
    }
    drawPath(body, color = color)
}

/** Classic SD card silhouette for the volume root “sd card” tile. */
private fun DrawScope.drawSdCardGlyph(color: Color) {
    val left = size.width * 0.28f
    val top = size.height * 0.16f
    val w = size.width * 0.44f
    val h = size.height * 0.68f
    val cut = w * 0.28f
    val card = Path().apply {
        fillType = PathFillType.EvenOdd
        moveTo(left + cut, top)
        lineTo(left + w, top)
        lineTo(left + w, top + h)
        lineTo(left, top + h)
        lineTo(left, top + cut)
        close()
        // Contact-pad cutouts
        val padTop = top + h * 0.12f
        val padBottom = top + h * 0.40f
        val padLeft = left + w * 0.18f
        val padW = w * 0.64f
        val gap = padW * 0.08f
        val padCount = 4
        val singleW = (padW - gap * (padCount - 1)) / padCount
        for (i in 0 until padCount) {
            val x = padLeft + i * (singleW + gap)
            addRect(
                Rect(
                    left = x,
                    top = padTop,
                    right = x + singleW,
                    bottom = padBottom,
                ),
            )
        }
    }
    drawPath(card, color = color)
}
