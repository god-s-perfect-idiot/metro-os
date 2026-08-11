package com.metro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * WP8.1 TextBox tokens (METRO-UX-LANGUAGE §6.12).
 *
 * The control is always a light rectangle with black text — it does not invert with dark theme.
 * Focus turns the 3dp border accent and the fill solid white.
 */
object MetroTextBoxDefaults {
    val BorderWidth = 3.dp
    val MinHeight = 44.dp
    val HorizontalPadding = 10.dp
    val VerticalPadding = 8.dp
    val RestBorder = Color(0xFFBFBFBF)
    val TextColor = MetroColors.LightPrimaryText
    val PlaceholderColor = MetroColors.LightPrimaryText.copy(alpha = 0.6f)

    fun fill(focused: Boolean): Color =
        if (focused) MetroColors.LightBackground else MetroColors.LightSecondarySurface

    fun borderColor(focused: Boolean, accent: Color): Color =
        if (focused) accent else RestBorder
}

/**
 * WP8.1 TextBox — square light fill, black text, 3dp border that turns accent when focused.
 *
 * Not Material input chrome: no floating label, no rounded corners, no dark filled chip
 * on a dark page.
 */
@Composable
fun MetroTextBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    var focused by remember { mutableStateOf(false) }
    val accent = MetroTheme.colors.accent
    val fill = MetroTextBoxDefaults.fill(focused = focused && enabled)
    val border = MetroTextBoxDefaults.borderColor(focused = focused && enabled, accent = accent)
    val textStyle = MetroTextStyle.Body.toTextStyle().copy(color = MetroTextBoxDefaults.TextColor)
    val selectionColors = remember(accent) {
        TextSelectionColors(
            handleColor = accent,
            backgroundColor = accent.copy(alpha = 0.4f),
        )
    }

    CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            textStyle = textStyle,
            cursorBrush = SolidColor(accent),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            modifier = modifier
                .defaultMinSize(minHeight = MetroTextBoxDefaults.MinHeight)
                .alpha(if (enabled) 1f else 0.4f)
                .background(fill, RectangleShape)
                .border(MetroTextBoxDefaults.BorderWidth, border, RectangleShape)
                .padding(
                    horizontal = MetroTextBoxDefaults.HorizontalPadding,
                    vertical = MetroTextBoxDefaults.VerticalPadding,
                )
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        MetroText(
                            text = placeholder,
                            style = MetroTextStyle.Body,
                            color = MetroTextBoxDefaults.PlaceholderColor,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MetroTextBoxDarkRestPreview() {
    MetroTheme(darkTheme = true) {
        MetroTextBox(
            value = "",
            onValueChange = {},
            placeholder = "search",
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MetroTextBoxDarkFilledPreview() {
    MetroTheme(darkTheme = true, accent = MetroColors.AccentTeal) {
        MetroTextBox(
            value = "the beatles",
            onValueChange = {},
            placeholder = "search",
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun MetroTextBoxLightPreview() {
    MetroTheme(darkTheme = false) {
        MetroTextBox(
            value = "",
            onValueChange = {},
            placeholder = "search",
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
        )
    }
}
