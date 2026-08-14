package com.metro.dialer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroAppBarDefaults
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
                MetroCircleIconButton(
                    type = MetroSystemIconType.DialPad,
                    onClick = onDialPadClick,
                    contentDescription = "dial pad",
                )
            }
            MetroCircleIconButton(
                type = MetroSystemIconType.People,
                onClick = onPeopleClick,
                contentDescription = "people",
            )
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
fun PhoneCallIcon(
    modifier: Modifier = Modifier,
    color: Color = MetroTheme.colors.accent,
    showCircle: Boolean = true,
    iconSize: Dp = 40.dp,
) {
    MetroSystemIcon(
        type = MetroSystemIconType.Phone,
        modifier = modifier.size(iconSize),
        iconSize = iconSize,
        color = color,
        showCircle = showCircle,
    )
}

@Composable
fun MessageIcon(
    modifier: Modifier = Modifier,
    color: Color = MetroTheme.colors.accent,
    iconSize: Dp = 40.dp,
) {
    MetroSystemIcon(
        type = MetroSystemIconType.Message,
        modifier = modifier.size(iconSize),
        iconSize = iconSize,
        color = color,
        showCircle = false,
    )
}

@Composable
fun AppBarCallGlyph(color: Color) {
    MetroSystemIcon(
        type = MetroSystemIconType.Phone,
        iconSize = MetroAppBarDefaults.GlyphSize,
        color = color,
        showCircle = false,
    )
}

@Composable
fun AppBarMessageGlyph(color: Color) {
    MetroSystemIcon(
        type = MetroSystemIconType.Message,
        iconSize = MetroAppBarDefaults.GlyphSize,
        color = color,
        showCircle = false,
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
