package com.metro.messaging.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
fun NewMessageScreen(
    recipient: String,
    body: String,
    onRecipientChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val canSend = recipient.isNotBlank() && body.isNotBlank()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = MetroAppBarDefaults.BarHeight),
        ) {
            MetroText(
                text = "new message",
                style = MetroTextStyle.SectionHeader,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            UnderlineField(
                label = "to",
                value = recipient,
                onValueChange = onRecipientChange,
                keyboardType = KeyboardType.Phone,
            )
            UnderlineField(
                label = "type a message",
                value = body,
                onValueChange = onBodyChange,
                keyboardType = KeyboardType.Text,
                modifier = Modifier.padding(top = 24.dp),
            )
        }

        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Forward,
                    label = "send",
                    onClick = onSend,
                    contentDescription = "send message",
                    enabled = canSend,
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun UnderlineField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = keyboardType == KeyboardType.Phone,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MetroTextStyle.Body.toTextStyle().copy(color = MetroTheme.colors.primaryText),
        cursorBrush = SolidColor(MetroTheme.colors.accent),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Column {
                if (value.isEmpty()) {
                    MetroText(
                        text = label,
                        style = MetroTextStyle.Body,
                        color = MetroTheme.colors.secondaryText,
                    )
                }
                inner()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .background(
                            if (value.isNotEmpty()) {
                                MetroTheme.colors.accent
                            } else {
                                MetroTheme.colors.secondaryText
                            },
                        )
                        .padding(vertical = 1.dp),
                )
            }
        },
    )
}
