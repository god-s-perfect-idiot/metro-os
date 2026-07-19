package com.metro.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.calendar.R
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.metroNavBarPadding

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
            .statusBarsPadding()
            .metroNavBarPadding(),
    ) {
        MetroAppTitle(title = stringResource(R.string.app_name))
        MetroText(
            text = stringResource(R.string.setup_title),
            style = MetroTextStyle.HubTitle,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        )
        MetroText(
            text = stringResource(R.string.permission_calendar_body),
            style = MetroTextStyle.Body,
            modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 24.dp),
        )
        MetroBorderButton(
            text = stringResource(R.string.grant_calendar),
            onClick = onRequestPermission,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        MetroBorderButton(
            text = stringResource(R.string.continue_with_demo),
            onClick = onContinueWithDemo,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}
