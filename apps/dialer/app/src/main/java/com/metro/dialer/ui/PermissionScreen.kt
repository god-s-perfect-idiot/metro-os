package com.metro.dialer.ui

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
import com.metro.dialer.R
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.metroNavBarPadding

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

        if (needsPermissions) {
            MetroText(
                text = stringResource(R.string.permission_call_log_body),
                style = MetroTextStyle.Body,
                modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 8.dp),
            )
            MetroText(
                text = stringResource(R.string.permission_call_phone_body),
                style = MetroTextStyle.Body,
                modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 24.dp),
            )
            MetroBorderButton(
                text = stringResource(R.string.grant_permissions),
                onClick = onRequestPermissions,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (!needsPermissions && needsDefaultDialer) {
            MetroText(
                text = stringResource(R.string.default_dialer_body),
                style = MetroTextStyle.Body,
                modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 24.dp),
            )
            MetroBorderButton(
                text = stringResource(R.string.set_default_dialer),
                onClick = onRequestDefaultDialer,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            MetroBorderButton(
                text = stringResource(R.string.continue_without_default),
                onClick = onContinue,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}
