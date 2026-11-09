package com.metro.clock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metro.clock.data.WorldCity
import com.metro.clock.data.WorldClockLogic
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroListItem
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTheme

@Composable
fun WorldCityPickerScreen(
    alreadyAddedIds: Set<String>,
    onPick: (WorldCity) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            WorldClockLogic.Catalog
        } else {
            WorldClockLogic.Catalog.filter { it.name.lowercase().contains(q) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MetroSettingsHeader(
                appTitle = "world clock",
                pageTitle = "add city",
            )
            MetroTextBox(
                value = query,
                onValueChange = { query = it },
                placeholder = "search",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 4.dp,
                    bottom = MetroAppBarDefaults.BarHeight + 16.dp,
                ),
            ) {
                items(filtered, key = { it.id }) { city ->
                    val added = city.id in alreadyAddedIds
                    MetroListItem(
                        title = city.name,
                        subtitle = WorldClockLogic.formatOffsetLabel(city.zoneId),
                        enabled = !added,
                        verticalPadding = 6.dp,
                        oneLineMinHeight = 56.dp,
                        twoLineMinHeight = 64.dp,
                        onClick = if (added) null else ({ onPick(city) }),
                    )
                }
            }
        }

        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Close,
                    label = "cancel",
                    onClick = onCancel,
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
