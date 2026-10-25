package com.metro.people.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.people.R
import com.metro.people.data.RecentPerson
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

/** 2 columns × 5 rows for the 10-contact recent mosaic. */
private const val RecentColumns = 2
private val RecentGap = 4.dp

/**
 * Hub recent — up to 10 saved contacts from calls + messages as a 2×5 photo tile grid.
 */
@Composable
fun RecentPane(
    hasCallLogPermission: Boolean,
    people: List<RecentPerson>,
    onRequestCallLogPermission: () -> Unit,
    onOpenPerson: (RecentPerson) -> Unit,
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
        !hasCallLogPermission && people.isEmpty() -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(horizontal = 12.dp)
                    .padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MetroText(
                    text = stringResource(R.string.recent_permission_body),
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.secondaryText,
                )
                MetroBorderButton(
                    text = stringResource(R.string.grant_call_log),
                    onClick = onRequestCallLogPermission,
                )
            }
        }
        people.isEmpty() -> {
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
            LazyVerticalGrid(
                columns = GridCells.Fixed(RecentColumns),
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(RecentGap),
                verticalArrangement = Arrangement.spacedBy(RecentGap),
            ) {
                items(people, key = { it.contactId }) { person ->
                    ContactAvatar(
                        contactId = person.contactId,
                        photoUri = person.photoUri,
                        fallbackLabel = person.displayName,
                        onClick = { onOpenPerson(person) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                    )
                }
            }
        }
    }
}
