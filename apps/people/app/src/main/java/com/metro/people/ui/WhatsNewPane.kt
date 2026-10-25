package com.metro.people.ui

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metro.people.R
import com.metro.people.data.PeopleRecentLogic
import com.metro.people.data.WhatsNewItem
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable

private val BadgeSize = 36.dp
private val BadgeFill = Color(0xFF3A3A3A)

/**
 * Hub what's new — shade conversations from connected apps (People hub feed layout).
 */
@Composable
fun WhatsNewPane(
    hasNotificationAccess: Boolean,
    items: List<WhatsNewItem>,
    onRequestNotificationAccess: () -> Unit,
    onOpenItem: (WhatsNewItem) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    when {
        isLoading -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                MetroLoadingDots()
            }
        }
        !hasNotificationAccess -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(horizontal = 12.dp)
                    .padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MetroText(
                    text = stringResource(R.string.whats_new_permission_body),
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.secondaryText,
                )
                MetroBorderButton(
                    text = stringResource(R.string.grant_notification_access),
                    onClick = onRequestNotificationAccess,
                )
            }
        }
        items.isEmpty() -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                MetroLoadingDots()
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(horizontal = 12.dp),
            ) {
                items(items, key = { it.key }) { item ->
                    WhatsNewRow(
                        item = item,
                        onClick = { onOpenItem(item) },
                    )
                }
                item { Spacer(modifier = Modifier.height(96.dp)) }
            }
        }
    }
}

@Composable
private fun WhatsNewRow(
    item: WhatsNewItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .metroClickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            MetroText(
                text = item.authorName,
                style = MetroTextStyle.ListItemTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.body.isNotBlank()) {
                MetroText(
                    text = "\"${item.body}\"",
                    style = MetroTextStyle.Body,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            MetroText(
                text = PeopleRecentLogic.whatsNewMeta(item.sourceLabel, item.postTimeMs),
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(BadgeSize)
                .background(BadgeFill),
            contentAlignment = Alignment.Center,
        ) {
            MetroText(
                text = if (item.badgeCount > 0) item.badgeCount.toString() else "+",
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.primaryText,
            )
        }
    }
}
