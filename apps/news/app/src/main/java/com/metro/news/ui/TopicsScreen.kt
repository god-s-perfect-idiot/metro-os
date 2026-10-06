package com.metro.news.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metro.news.data.NewsCategory
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroListItem
import com.metro.ui.MetroPageHeader
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch

@Composable
fun TopicsScreen(
    state: NewsState,
    mode: TopicsMode,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = MetroAppBarDefaults.BarHeight + 12.dp),
        ) {
            MetroAppTitle(title = "news")
            MetroPageHeader(title = if (mode == TopicsMode.Topics) "topics" else "sources")

            NewsCategory.entries.forEach { category ->
                val enabled = state.enabledCategories[category] == true
                if (mode == TopicsMode.Topics) {
                    MetroToggleSwitch(
                        checked = enabled,
                        onCheckedChange = { state.setCategoryEnabled(category, it) },
                        label = category.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                } else {
                    MetroListItem(
                        title = category.sourceLabel,
                        subtitle = category.sourceSubtitle,
                        trailing = {
                            MetroToggleSwitch(
                                checked = enabled,
                                onCheckedChange = { state.setCategoryEnabled(category, it) },
                                showStatus = false,
                            )
                        },
                    )
                }
            }
        }

        MetroAppBar(
            minimized = true,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

enum class TopicsMode {
    Topics,
    Sources,
}
