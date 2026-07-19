package com.metro.dialer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.dialer.R
import com.metro.dialer.data.CallDirection
import com.metro.dialer.data.CallGroup
import com.metro.dialer.data.DialerCallLogic
import com.metro.ui.MetroColors
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
fun HistoryScreen(
    groups: List<CallGroup>,
    hasPermission: Boolean,
    onOpenDetail: (CallGroup) -> Unit,
    onCallBack: (CallGroup) -> Unit,
    onAddSpeedDial: (CallGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!hasPermission) {
        EmptyPane(
            message = stringResource(R.string.history_permission_denied),
            modifier = modifier,
        )
        return
    }
    if (groups.isEmpty()) {
        EmptyPane(
            message = stringResource(R.string.history_empty),
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 12.dp),
    ) {
        items(groups, key = { it.normalizedNumber }) { group ->
            HistoryRow(
                group = group,
                onOpenDetail = { onOpenDetail(group) },
                onCallBack = { onCallBack(group) },
                onLongPress = { onAddSpeedDial(group) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(
    group: CallGroup,
    onOpenDetail: () -> Unit,
    onCallBack: () -> Unit,
    onLongPress: () -> Unit,
) {
    val primaryColor = when (group.latestType) {
        CallDirection.Missed -> MetroColors.AccentRed
        else -> MetroTheme.colors.primaryText
    }
    val directionLabel = when (group.latestType) {
        CallDirection.Incoming -> stringResource(R.string.incoming)
        CallDirection.Outgoing -> stringResource(R.string.outgoing)
        CallDirection.Missed -> stringResource(R.string.missed)
    }
    val subtitle = "${directionLabel} · ${DialerCallLogic.relativeTime(group.latestTimestamp)}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpenDetail,
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpenDetail,
                    onLongClick = onLongPress,
                ),
        ) {
            MetroText(
                text = DialerCallLogic.primaryLabel(group),
                style = MetroTextStyle.ListItemTitle,
                color = primaryColor,
            )
            MetroText(
                text = subtitle,
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
            )
        }
        Box(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCallBack,
                )
                .padding(start = 8.dp),
        ) {
            PhoneCallIcon(color = Color.White)
        }
    }
}
