/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.lib.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroColors
import com.metro.ui.MetroDimens
import com.metro.ui.MetroFontFamily
import com.metro.ui.MetroTheme
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.lib.util.InputMethodUtils
import org.florisboard.lib.android.showShortToastSync
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.compose.verticalTween

private const val AnimationDuration = 200

private val PreviewEnterTransition = EnterTransition.verticalTween(AnimationDuration)
private val PreviewExitTransition = ExitTransition.verticalTween(AnimationDuration)

val LocalPreviewFieldController = staticCompositionLocalOf<PreviewFieldController?> { null }

@Composable
fun rememberPreviewFieldController(): PreviewFieldController {
    return remember { PreviewFieldController() }
}

class PreviewFieldController {
    val focusRequester = FocusRequester()
    var isVisible by mutableStateOf(false)
    var text by mutableStateOf(TextFieldValue(""))
}

/**
 * WP8.1 preview input bar — inset light text box + detached border "input" button
 * (not an edge-to-edge Material TextField with a trailing keyboard icon).
 */
@Composable
fun PreviewKeyboardField(
    controller: PreviewFieldController,
    modifier: Modifier = Modifier,
    hint: String = stringRes(R.string.settings__preview_keyboard),
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val darkChrome = MetroTheme.colors.background.luminance() < 0.5f
    val fieldBackground = if (darkChrome) MetroColors.LightSecondarySurface else MetroColors.DarkSecondarySurface
    val fieldTextColor = if (darkChrome) MetroColors.LightPrimaryText else MetroColors.DarkPrimaryText
    val placeholderColor = fieldTextColor.copy(alpha = 0.45f)

    AnimatedVisibility(
        visible = controller.isVisible,
        enter = PreviewEnterTransition,
        exit = PreviewExitTransition,
    ) {
        SelectionContainer {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MetroDimens.ScreenHorizontalMargin,
                        vertical = 8.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .background(fieldBackground, RectangleShape)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .onPreviewKeyEvent { event ->
                            if (event.key == Key.Back) {
                                focusManager.clearFocus()
                            }
                            false
                        }
                        .focusRequester(controller.focusRequester),
                    value = controller.text,
                    onValueChange = { controller.text = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = MetroFontFamily,
                        fontSize = 18.sp,
                        color = fieldTextColor,
                        textDirection = TextDirection.ContentOrLtr,
                    ),
                    cursorBrush = SolidColor(MetroTheme.colors.accent),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() },
                    ),
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = true),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (controller.text.text.isEmpty()) {
                                BasicText(
                                    text = hint,
                                    style = TextStyle(
                                        fontFamily = MetroFontFamily,
                                        fontSize = 18.sp,
                                        color = placeholderColor,
                                    ),
                                    maxLines = 1,
                                )
                            }
                            inner()
                        }
                    },
                )
                Spacer(modifier = Modifier.width(8.dp))
                MetroBorderButton(
                    text = "input",
                    onClick = {
                        if (!InputMethodUtils.showImePicker(context)) {
                            context.showShortToastSync("Error: InputMethodManager service not available!")
                        }
                    },
                )
            }
        }
    }
}
