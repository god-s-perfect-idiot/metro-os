package com.metro.navbar.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.metro.navbar.NavbarSpec
import com.metro.navbar.NavbarThemeSnapshot
import com.metro.navbar.R
import com.metro.ui.MetroSystemIcon
import com.metro.ui.MetroSystemIconType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NavigationBar(
  theme: NavbarThemeSnapshot,
  onBack: () -> Unit,
  onStart: () -> Unit,
  onSearch: () -> Unit,
  onBackLongPress: () -> Unit,
  onSearchLongPress: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(NavbarSpec.BAR_HEIGHT_DP.dp)
      .background(theme.barColor)
      .testTag("metro_navbar"),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    NavKey(
      modifier = Modifier.weight(1f),
      contentDescription = stringResource(R.string.key_back),
      onClick = onBack,
      onLongClick = onBackLongPress,
    ) {
      MetroSystemIcon(
        type = MetroSystemIconType.Back,
        iconSize = NavbarSpec.SOFT_KEY_ICON_SIZE_DP.dp,
        color = theme.iconColor,
        showCircle = false,
      )
    }
    NavKey(
      modifier = Modifier.weight(1f),
      contentDescription = stringResource(R.string.key_start),
      onClick = onStart,
    ) {
      WindowsLogoIcon(color = theme.iconColor)
    }
    NavKey(
      modifier = Modifier.weight(1f),
      contentDescription = stringResource(R.string.key_search),
      onClick = onSearch,
      onLongClick = onSearchLongPress,
    ) {
      MetroSystemIcon(
        type = MetroSystemIconType.Search,
        iconSize = NavbarSpec.SOFT_KEY_ICON_SIZE_DP.dp,
        color = theme.iconColor,
        showCircle = false,
      )
    }
  }
}

@Composable
fun HiddenNavbarRevealStrip(
  theme: NavbarThemeSnapshot,
  onReveal: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(NavbarSpec.REVEAL_STRIP_HEIGHT_DP.dp)
      .background(theme.barColor)
      .pointerInput(Unit) {
        detectVerticalDragGestures { _, dragAmount ->
          if (dragAmount < -12f) {
            onReveal()
          }
        }
      }
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onReveal,
      )
      .testTag("metro_navbar_reveal"),
    contentAlignment = Alignment.TopCenter,
  ) {
    Canvas(modifier = Modifier.size(width = 36.dp, height = 3.dp)) {
      drawRoundRect(
        color = theme.iconColor.copy(alpha = 0.6f),
        topLeft = Offset.Zero,
        size = Size(size.width, size.height),
        cornerRadius = CornerRadius(size.height / 2f),
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NavKey(
  onClick: () -> Unit,
  contentDescription: String,
  modifier: Modifier = Modifier,
  onLongClick: (() -> Unit)? = null,
  content: @Composable () -> Unit,
) {
  val interactionModifier = if (onLongClick != null) {
    Modifier.combinedClickable(
      interactionSource = remember { MutableInteractionSource() },
      indication = null,
      onClick = onClick,
      onLongClick = onLongClick,
    )
  } else {
    Modifier.clickable(
      interactionSource = remember { MutableInteractionSource() },
      indication = null,
      onClick = onClick,
    )
  }

  Box(
    modifier = modifier
      .fillMaxHeight()
      .semantics {
        role = Role.Button
        this.contentDescription = contentDescription
      }
      .then(interactionModifier),
    contentAlignment = Alignment.Center,
  ) {
    content()
  }
}

/** WP8.1 four-pane Windows logo for the Start key. */
@Composable
private fun WindowsLogoIcon(
  color: Color,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.size(NavbarSpec.START_KEY_ICON_SIZE_DP.dp)) {
    val gap = size.minDimension * 0.08f
    val pane = (size.minDimension - gap * 3f) / 2f
    val offsets = listOf(
      Offset(0f, 0f),
      Offset(pane + gap, 0f),
      Offset(0f, pane + gap),
      Offset(pane + gap, pane + gap),
    )
    offsets.forEach { topLeft ->
      drawRect(color = color, topLeft = topLeft, size = Size(pane, pane))
    }
  }
}
