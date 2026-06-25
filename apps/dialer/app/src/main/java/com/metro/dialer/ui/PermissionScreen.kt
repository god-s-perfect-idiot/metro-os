package com.metro.dialer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.dialer.R
import com.metro.ui.MetroPageHeader
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
fun PermissionScreen(
    hasCallLogPermission: Boolean,
    hasCallPhonePermission: Boolean,
    isDefaultDialer: Boolean,
    onRequestPermissions: () -> Unit,
    onRequestDefaultDialer: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val needsPermissions = !hasCallLogPermission || !hasCallPhonePermission
    val needsDefaultDialer = !isDefaultDialer

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color.Black)
            .padding(horizontal = 24.dp),
    ) {
        MetroPageHeader(title = stringResource(R.string.setup_title))

        if (needsPermissions) {
            MetroText(
                text = stringResource(R.string.permission_call_log_body),
                style = MetroTextStyle.Body,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            MetroText(
                text = stringResource(R.string.permission_call_phone_body),
                style = MetroTextStyle.Body,
                modifier = Modifier.padding(bottom = 24.dp),
            )
            MetroText(
                text = stringResource(R.string.grant_permissions),
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.accent,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .clickable(onClick = onRequestPermissions),
            )
        }

        if (!needsPermissions && needsDefaultDialer) {
            MetroText(
                text = stringResource(R.string.default_dialer_body),
                style = MetroTextStyle.Body,
                modifier = Modifier.padding(bottom = 24.dp),
            )
            MetroText(
                text = stringResource(R.string.set_default_dialer),
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.accent,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clickable(onClick = onRequestDefaultDialer),
            )
            MetroText(
                text = stringResource(R.string.continue_without_default),
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.clickable(onClick = onContinue),
            )
        }
    }
}
