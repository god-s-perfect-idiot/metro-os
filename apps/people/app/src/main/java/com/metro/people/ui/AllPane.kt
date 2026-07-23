package com.metro.people.ui

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metro.people.R
import com.metro.people.data.PersonSummary
import com.metro.ui.MetroContextMenuActiveShift
import com.metro.ui.MetroContextMenuClearOnDismiss
import com.metro.ui.MetroContextMenuDimmedAlpha
import com.metro.ui.MetroContextMenuItem
import com.metro.ui.MetroContextMenuPopup
import com.metro.ui.MetroJumpListLogic
import com.metro.ui.MetroLetterTile
import com.metro.ui.MetroShowingLabel
import com.metro.ui.MetroSystemIcon
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions
import com.metro.ui.metroStickyLetterHeader

/** Mutable holder so layout callbacks can update without triggering recomposition. */
private class RectRef {
    var value: Rect = Rect.Zero
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllPane(
    filterLabel: String,
    grouped: Map<Char, List<PersonSummary>>,
    flatContacts: List<PersonSummary> = emptyList(),
    searchActive: Boolean = false,
    onFilterClick: () -> Unit,
    onJumpClick: () -> Unit,
    onOpenDetail: (PersonSummary) -> Unit,
    onAddToSpeedDial: (PersonSummary) -> Unit,
    onPinToStart: (PersonSummary) -> Unit,
    scrollToLetter: Char?,
    onScrollConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val showLetterMarkers = !searchActive
    // Index 0 is the "showing" filter row when markers are visible.
    val headerIndices = remember(grouped, showLetterMarkers) {
        if (!showLetterMarkers) return@remember emptyMap()
        var index = 1
        buildMap {
            grouped.forEach { (letter, people) ->
                put(MetroJumpListLogic.normalize(letter), index)
                index += 1 + people.size
            }
        }
    }
    LaunchedEffect(scrollToLetter, headerIndices, showLetterMarkers) {
        if (!showLetterMarkers) {
            onScrollConsumed()
            return@LaunchedEffect
        }
        val letter = scrollToLetter ?: return@LaunchedEffect
        val index = headerIndices[MetroJumpListLogic.normalize(letter)]
        if (index != null) {
            listState.scrollToItem(index)
        }
        onScrollConsumed()
    }

    var contextMenuPerson by remember { mutableStateOf<PersonSummary?>(null) }
    var contextMenuAnchor by remember { mutableStateOf(Rect.Zero) }
    var contextMenuRoot by remember { mutableStateOf(Rect.Zero) }
    val contextMenuVisible = remember { MutableTransitionState(false) }
    val popupRootBounds = remember { RectRef() }
    val contextMenuFocusTransition =
        updateTransition(contextMenuVisible, label = "peopleContextMenuFocus")
    val contextMenuFocusFraction by contextMenuFocusTransition.animateFloat(
        transitionSpec = { tween(MetroTransitions.AppBarSlideMs) },
        label = "focusFraction",
    ) { visible -> if (visible) 1f else 0f }

    val openContextMenu: (PersonSummary, Rect) -> Unit = { person, bounds ->
        contextMenuAnchor = bounds
        contextMenuRoot = popupRootBounds.value
        contextMenuPerson = person
        contextMenuVisible.targetState = true
    }
    val dismissContextMenu: () -> Unit = {
        contextMenuVisible.targetState = false
    }
    MetroContextMenuClearOnDismiss(contextMenuVisible) {
        contextMenuPerson = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                popupRootBounds.value = coordinates.boundsInWindow()
            },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(horizontal = 12.dp),
        ) {
            if (showLetterMarkers) {
                item {
                    MetroShowingLabel(
                        label = filterLabel,
                        modifier = Modifier.padding(vertical = 12.dp),
                        onClick = onFilterClick,
                    )
                }
                grouped.forEach { (letter, people) ->
                    metroStickyLetterHeader(letter = letter) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black)
                                .padding(vertical = 8.dp)
                                .graphicsLayer {
                                    alpha = if (contextMenuPerson != null) {
                                        1f - contextMenuFocusFraction *
                                            (1f - MetroContextMenuDimmedAlpha)
                                    } else {
                                        1f
                                    }
                                },
                        ) {
                            MetroLetterTile(
                                letter = letter,
                                onClick = onJumpClick,
                            )
                        }
                    }
                    items(people, key = { it.id }) { person ->
                        ContactRow(
                            person = person,
                            contextMenuTarget = contextMenuPerson?.id == person.id,
                            contextMenuFocusFraction = contextMenuFocusFraction,
                            onOpenDetail = { onOpenDetail(person) },
                            onLongClick = { bounds -> openContextMenu(person, bounds) },
                        )
                    }
                }
            } else {
                items(flatContacts, key = { it.id }) { person ->
                    ContactRow(
                        person = person,
                        contextMenuTarget = contextMenuPerson?.id == person.id,
                        contextMenuFocusFraction = contextMenuFocusFraction,
                        onOpenDetail = { onOpenDetail(person) },
                        onLongClick = { bounds -> openContextMenu(person, bounds) },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(96.dp)) }
        }

        contextMenuPerson?.let { person ->
            val speedDialLabel = stringResource(R.string.add_to_speed_dial)
            val pinLabel = stringResource(R.string.pin_to_start)
            MetroContextMenuPopup(
                visibleState = contextMenuVisible,
                anchorBounds = contextMenuAnchor,
                rootBounds = contextMenuRoot,
                items = listOf(
                    MetroContextMenuItem(
                        label = speedDialLabel,
                        enabled = !person.defaultPhone.isNullOrBlank(),
                        onClick = {
                            onAddToSpeedDial(person)
                            dismissContextMenu()
                        },
                    ),
                    MetroContextMenuItem(
                        label = pinLabel,
                        onClick = {
                            onPinToStart(person)
                            dismissContextMenu()
                        },
                    ),
                ),
                onDismissRequest = dismissContextMenu,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactRow(
    person: PersonSummary,
    contextMenuTarget: Boolean,
    contextMenuFocusFraction: Float,
    onOpenDetail: () -> Unit,
    onLongClick: (Rect) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val avatarBounds = remember(person.id) { RectRef() }
    val density = LocalDensity.current
    val activeShiftPx = with(density) { MetroContextMenuActiveShift.toPx() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                if (contextMenuTarget) {
                    translationX = -contextMenuFocusFraction * activeShiftPx
                    alpha = 1f
                } else {
                    translationX = 0f
                    alpha = 1f - contextMenuFocusFraction * (1f - MetroContextMenuDimmedAlpha)
                }
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onOpenDetail,
                onLongClick = { onLongClick(avatarBounds.value) },
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContactAvatar(
            contactId = person.id,
            modifier = Modifier
                .size(48.dp)
                .onGloballyPositioned { coordinates ->
                    avatarBounds.value = coordinates.boundsInWindow()
                },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            MetroText(
                text = person.displayName,
                style = MetroTextStyle.ListItemTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier.size(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            MetroSystemIcon(
                type = MetroSystemIconType.Forward,
                iconSize = 40.dp,
                color = MetroTheme.colors.primaryText,
            )
        }
    }
}
