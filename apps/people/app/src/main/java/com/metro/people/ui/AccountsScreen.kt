package com.metro.people.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.people.R
import com.metro.people.data.AccountOption
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
fun AccountsScreen(
    options: List<AccountOption>,
    onBack: () -> Unit,
    onSelect: (AccountOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
    ) {
        MetroText(
            text = "ADD AN ACCOUNT",
            style = MetroTextStyle.SectionHeader,
            modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
        )
        options.forEach { option ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(option) }
                    .padding(vertical = 12.dp),
            ) {
                MetroText(text = option.label, style = MetroTextStyle.ListItemTitle)
                option.subtitle?.let {
                    MetroText(
                        text = it,
                        style = MetroTextStyle.ListItemSubtitle,
                        color = MetroTheme.colors.secondaryText,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        MetroText(
            text = "back",
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.accent,
            modifier = Modifier.clickable(onClick = onBack),
        )
    }
}
