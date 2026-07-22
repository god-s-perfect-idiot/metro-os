package com.metro.dialer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.dialer.R
import com.metro.dialer.data.ActiveCall
import com.metro.dialer.data.DialerCallLogic
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

private val IncomingSectionBackground = Color(0xFF141414)
private val IncomingSecondaryTileBackground = Color(0xFF252525)
private val IncomingTileGap = 6.dp
private val IncomingTileHeight = 78.dp

@Composable
fun IncomingCallScreen(
    call: ActiveCall,
    onAnswer: () -> Unit,
    onIgnore: () -> Unit,
    onTextReply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onIgnore)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 16.dp),
        ) {
            MetroText(
                text = stringResource(R.string.carrier_unknown),
                style = MetroTextStyle.ListItemSubtitle,
                color = Color.White.copy(alpha = 0.85f),
            )

            Spacer(modifier = Modifier.height(48.dp))

            MetroText(
                text = stringResource(R.string.incoming_call_label),
                style = MetroTextStyle.SectionHeader,
                color = Color.White,
            )

            MetroText(
                text = call.displayName,
                style = MetroTextStyle.PageTitle,
                color = Color.White,
                modifier = Modifier.padding(top = 12.dp),
            )

            MetroText(
                text = stringResource(
                    R.string.mobile_label,
                    DialerCallLogic.formatDisplayNumber(call.phoneNumber),
                ),
                style = MetroTextStyle.ListItemTitle,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(IncomingSectionBackground)
                .navigationBarsPadding()
                .padding(IncomingTileGap),
            verticalArrangement = Arrangement.spacedBy(IncomingTileGap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(IncomingTileGap),
            ) {
                IncomingActionTile(
                    label = stringResource(R.string.answer),
                    background = MetroTheme.colors.accent,
                    onClick = onAnswer,
                    modifier = Modifier.weight(1f),
                )
                IncomingActionTile(
                    label = stringResource(R.string.ignore),
                    background = IncomingSecondaryTileBackground,
                    onClick = onIgnore,
                    modifier = Modifier.weight(1f),
                )
            }
            IncomingActionTile(
                label = stringResource(R.string.text_reply),
                background = IncomingSecondaryTileBackground,
                onClick = onTextReply,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun IncomingActionTile(
    label: String,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val tileBackground = if (isPressed) {
        background.copy(alpha = 0.75f)
    } else {
        background
    }
    Box(
        modifier = modifier
            .height(IncomingTileHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .background(tileBackground),
        contentAlignment = Alignment.Center,
    ) {
        MetroText(
            text = label,
            style = MetroTextStyle.ListItemSubtitle,
            color = Color.White,
        )
    }
}
