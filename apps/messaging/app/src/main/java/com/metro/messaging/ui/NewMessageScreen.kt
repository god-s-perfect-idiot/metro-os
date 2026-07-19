package com.metro.messaging.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.metro.messaging.data.ContactSuggestion
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NewMessageScreen(
    recipient: String,
    body: String,
    contactSuggestions: List<ContactSuggestion>,
    onRecipientChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onSelectContact: (ContactSuggestion) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val canSend = recipient.isNotBlank() && body.isNotBlank()
    // Shell already clears the IME; default bring-into-view pans again and shoves the header off-screen.
    val noExtraBringIntoView = remember {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float,
            ): Float = 0f
        }
    }

    CompositionLocalProvider(LocalBringIntoViewSpec provides noExtraBringIntoView) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState()),
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
                keyboardType = KeyboardType.Text,
                singleLine = true,
            )
            if (contactSuggestions.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    contactSuggestions.forEach { suggestion ->
                        ContactSuggestionRow(
                            suggestion = suggestion,
                            onClick = { onSelectContact(suggestion) },
                        )
                    }
                }
            }
        }

        MessageComposer(
            text = body,
            onTextChange = onBodyChange,
            onSend = onSend,
            sendEnabled = canSend,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
        )
    }
    }
}

@Composable
private fun ContactSuggestionRow(
    suggestion: ContactSuggestion,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 10.dp),
    ) {
        MetroText(
            text = suggestion.displayName,
            style = MetroTextStyle.ListItemTitle,
        )
        MetroText(
            text = suggestion.phoneNumber,
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun UnderlineField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
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
