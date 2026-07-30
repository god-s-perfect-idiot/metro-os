package com.metro.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

private val TrackWidth = 52.dp
private val TrackHeight = 20.dp
private val BorderWidth = 2.dp
/** Visible white/foreground fill of the thumb (inside the blend border). */
private val ThumbFillWidth = 14.dp
/**
 * Blend-border overhang past the track chrome. On a dark page the black border
 * reads only where it meets the accent (left when on); top/right/bottom match the bg.
 */
private val ThumbOverhang = BorderWidth
private val ThumbOuterWidth = ThumbFillWidth + BorderWidth * 2
private val ThumbOuterHeight = TrackHeight + ThumbOverhang * 2

/**
 * WP8.1 toggle — sharp rectangular track, sliding rectangular thumb, accent fill when on
 * (METRO-UX-LANGUAGE §6.9).
 *
 * Settings layout: optional [label] above, "On"/"Off" status (`ListItemTitle`) left of switch.
 * Pass [showStatus] = false for a trailing switch-only control (e.g. alarm rows).
 */
@Composable
fun MetroToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    showStatus: Boolean = true,
    statusOn: String = "On",
    statusOff: String = "Off",
) {
    val accent = MetroTheme.colors.accent
    val foreground = MetroTheme.colors.primaryText
    val secondary = MetroTheme.colors.secondaryText
    val alpha = if (enabled) 1f else 0.4f

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
    ) {
        if (label != null) {
            MetroText(
                text = label,
                style = MetroTextStyle.ListItemSubtitle,
                color = secondary.copy(alpha = secondary.alpha * alpha),
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        Row(
            modifier = if (showStatus) Modifier.fillMaxWidth() else Modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showStatus) {
                MetroText(
                    text = if (checked) statusOn else statusOff,
                    style = MetroTextStyle.ListItemTitle,
                    color = foreground.copy(alpha = foreground.alpha * alpha),
                )
                Spacer(modifier = Modifier.weight(1f))
            }
            MetroToggleTrack(
                checked = checked,
                enabled = enabled,
                accent = accent,
                foreground = foreground,
                blend = MetroTheme.colors.background,
            )
        }
    }
}

@Composable
private fun MetroToggleTrack(
    checked: Boolean,
    enabled: Boolean,
    accent: Color,
    foreground: Color,
    blend: Color,
) {
    val alpha = if (enabled) 1f else 0.4f
    val borderColor = foreground.copy(alpha = foreground.alpha * alpha)
    val blendColor = blend.copy(alpha = blend.alpha * alpha)
    val thumbColor = if (checked) {
        Color.White.copy(alpha = alpha)
    } else {
        foreground.copy(alpha = foreground.alpha * alpha)
    }
    // Off: blend border hangs past the left track edge.
    // On: blend border hangs past the right track edge.
    val thumbOffX = -ThumbOverhang
    val thumbOnX = TrackWidth - ThumbOuterWidth + ThumbOverhang
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) thumbOnX else thumbOffX,
        animationSpec = MetroTransitions.pivotTween(),
        label = "metroToggleThumb",
    )
    // Accent stops at the thumb fill (inside the blend border), not under it.
    val accentToThumb = (thumbOffset + BorderWidth).coerceAtLeast(0.dp)

    // Room for vertical + horizontal thumb overhang outside the white track.
    Box(
        modifier = Modifier
            .width(TrackWidth + ThumbOverhang * 2)
            .height(ThumbOuterHeight)
            .padding(horizontal = ThumbOverhang),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(width = TrackWidth, height = TrackHeight)
                .align(Alignment.Center)
                .border(BorderWidth, borderColor, RectangleShape),
        ) {
            // Accent inset: white track → equal blend gutter → accent.
            // Use TopStart + explicit sizes only — no vertical centering (that
            // doubled the top gutter and collapsed the bottom).
            if (checked && accentToThumb > BorderWidth * 2) {
                val gutterWidth = accentToThumb - BorderWidth
                val gutterHeight = TrackHeight - BorderWidth * 2
                val accentHeight = gutterHeight - BorderWidth * 2
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = BorderWidth, y = BorderWidth)
                        .width(gutterWidth)
                        .height(gutterHeight)
                        .background(blendColor, RectangleShape),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = BorderWidth * 2, y = BorderWidth * 2)
                        .width(gutterWidth - BorderWidth)
                        .height(accentHeight)
                        .background(accent.copy(alpha = accent.alpha * alpha), RectangleShape),
                )
            }
        }
        // Outer blend rect + inset fill so top/right/bottom match the page bg.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbOffset)
                .width(ThumbOuterWidth)
                .height(ThumbOuterHeight)
                .zIndex(1f)
                .background(blendColor, RectangleShape),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(ThumbFillWidth)
                    .height(TrackHeight)
                    .background(thumbColor, RectangleShape),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MetroToggleSwitchOnPreview() {
    MetroTheme(darkTheme = true) {
        MetroToggleSwitch(
            checked = true,
            onCheckedChange = {},
            label = "Auto hide/show the Nav bar",
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color.Black),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MetroToggleSwitchOffPreview() {
    MetroTheme(darkTheme = true) {
        MetroToggleSwitch(
            checked = false,
            onCheckedChange = {},
            label = "Swipe to hide the Nav bar",
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color.Black),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun MetroToggleSwitchLightPreview() {
    MetroTheme(darkTheme = false) {
        Box(modifier = Modifier.background(Color.White).padding(16.dp)) {
            MetroToggleSwitch(
                checked = true,
                onCheckedChange = {},
                label = "Suggest text",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
