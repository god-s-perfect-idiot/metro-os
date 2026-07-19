package com.metro.people.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.metro.people.data.PersonSummary
import com.metro.ui.MetroJumpListLogic
import com.metro.ui.MetroLetterTile
import com.metro.ui.MetroShowingLabel
import com.metro.ui.MetroSystemIcon
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
fun AllPane(
    filterLabel: String,
    grouped: Map<Char, List<PersonSummary>>,
    onFilterClick: () -> Unit,
    onJumpClick: () -> Unit,
    onOpenDetail: (PersonSummary) -> Unit,
    scrollToLetter: Char?,
    onScrollConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(scrollToLetter, grouped) {
        val letter = scrollToLetter ?: return@LaunchedEffect
        val target = MetroJumpListLogic.normalize(letter)
        // Index 0 is the "showing" filter row; headers follow with their contact rows.
        var lazyIndex = 1
        for ((key, people) in grouped) {
            if (MetroJumpListLogic.normalize(key) == target) {
                listState.animateScrollToItem(lazyIndex)
                onScrollConsumed()
                return@LaunchedEffect
            }
            lazyIndex += 1 + people.size
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp),
    ) {
        item {
            MetroShowingLabel(
                label = filterLabel,
                modifier = Modifier.padding(vertical = 12.dp),
                onClick = onFilterClick,
            )
        }
        grouped.forEach { (letter, people) ->
            item(key = "header-$letter") {
                MetroLetterTile(
                    letter = letter,
                    onClick = onJumpClick,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            items(people, key = { it.id }) { person ->
                ContactRow(
                    person = person,
                    onOpenDetail = { onOpenDetail(person) },
                )
            }
        }
        item { Spacer(modifier = Modifier.height(96.dp)) }
    }
}

@Composable
private fun ContactRow(
    person: PersonSummary,
    onOpenDetail: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onOpenDetail,
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContactAvatar(
            contactId = person.id,
            modifier = Modifier.size(48.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            MetroText(text = person.displayName, style = MetroTextStyle.ListItemTitle)
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
