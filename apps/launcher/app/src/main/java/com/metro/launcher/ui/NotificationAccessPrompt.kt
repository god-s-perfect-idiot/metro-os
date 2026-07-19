package com.metro.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.launcher.R
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle

/**
 * Soft prompt for notification-listener access — does not block Start.
 */
@Composable
fun NotificationAccessPrompt(
    onGrant: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        MetroText(
            text = stringResource(R.string.notification_access_title),
            style = MetroTextStyle.SectionHeader,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(6.dp))
        MetroText(
            text = stringResource(R.string.notification_access_body),
            style = MetroTextStyle.Body,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(12.dp))
        MetroBorderButton(
            text = stringResource(R.string.notification_access_grant),
            onClick = onGrant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        MetroBorderButton(
            text = stringResource(R.string.notification_access_later),
            onClick = onDismiss,
        )
    }
}
