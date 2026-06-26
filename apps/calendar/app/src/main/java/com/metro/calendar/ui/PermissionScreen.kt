package com.metro.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.calendar.R
import com.metro.ui.MetroPageHeader
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
fun PermissionScreen(
    onRequestPermission: () -> Unit,
    onContinueWithDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp),
    ) {
        MetroPageHeader(title = stringResource(R.string.permission_calendar_title))
        MetroText(
            text = stringResource(R.string.permission_calendar_body),
            style = MetroTextStyle.Body,
            modifier = Modifier.padding(bottom = 24.dp),
        )
        MetroText(
            text = stringResource(R.string.grant_calendar),
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.accent,
            modifier = Modifier
                .clickable(onClick = onRequestPermission)
                .padding(bottom = 16.dp),
        )
        MetroText(
            text = stringResource(R.string.continue_with_demo),
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.clickable(onClick = onContinueWithDemo),
        )
    }
}
