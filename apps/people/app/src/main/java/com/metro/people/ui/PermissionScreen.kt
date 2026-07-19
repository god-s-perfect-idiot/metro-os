package com.metro.people.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.people.R
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.metroNavBarPadding

@Composable
fun PermissionScreen(
    onRequestPermission: () -> Unit,
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 12.dp),
        )
        MetroText(
            text = stringResource(R.string.permission_contacts_body),
            style = MetroTextStyle.Body,
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp),
        )
        MetroBorderButton(
            text = stringResource(R.string.grant_contacts),
            onClick = onRequestPermission,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}
