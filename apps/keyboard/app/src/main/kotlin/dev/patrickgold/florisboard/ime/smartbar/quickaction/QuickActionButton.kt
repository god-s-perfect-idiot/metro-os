/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.smartbar.quickaction

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.ui.MetroSystemIcon
import com.metro.ui.MetroSystemIconDefaultSize
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTextStyle
import dev.patrickgold.compose.tooltip.PlainTooltip
import dev.patrickgold.florisboard.ime.input.LocalInputFeedbackController
import dev.patrickgold.florisboard.ime.keyboard.ComputingEvaluator
import dev.patrickgold.florisboard.ime.keyboard.computeImageVector
import dev.patrickgold.florisboard.ime.keyboard.computeLabel
import dev.patrickgold.florisboard.ime.keyboard.computeMetroIcon
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggText
import org.florisboard.lib.snygg.ui.rememberSnyggThemeQuery

enum class QuickActionBarType {
    INTERACTIVE_BUTTON,
    INTERACTIVE_TILE,
    EDITOR_TILE;
}

@Composable
fun QuickActionButton(
    action: QuickAction,
    evaluator: ComputingEvaluator,
    modifier: Modifier = Modifier,
    type: QuickActionBarType = QuickActionBarType.INTERACTIVE_BUTTON,
) {
    if (type == QuickActionBarType.INTERACTIVE_TILE) {
        MetroQuickActionTile(
            action = action,
            evaluator = evaluator,
            modifier = modifier,
        )
        return
    }

    val context = LocalContext.current
    val inputFeedbackController = LocalInputFeedbackController.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isEnabled = type == QuickActionBarType.EDITOR_TILE || evaluator.evaluateEnabled(action.keyData())
    val elementName = when (type) {
        QuickActionBarType.INTERACTIVE_BUTTON -> FlorisImeUi.SmartbarActionKey
        QuickActionBarType.INTERACTIVE_TILE -> FlorisImeUi.SmartbarActionTile
        QuickActionBarType.EDITOR_TILE -> FlorisImeUi.SmartbarActionsEditorTile
    }.elementName
    val attributes = mapOf(FlorisImeUi.Attr.Code to action.keyData().code)
    val selector = when {
        isPressed -> SnyggSelector.PRESSED
        !isEnabled -> SnyggSelector.DISABLED
        else -> null
    }

    // Need to manually cancel an action if this composable suddenly leaves the composition to prevent the key from
    // being stuck in the pressed state
    DisposableEffect(action, isEnabled) {
        onDispose {
            if (action is QuickAction.InsertKey) {
                action.onPointerCancel(context)
            }
        }
    }

    PlainTooltip(action.computeTooltip(evaluator), enabled = type == QuickActionBarType.INTERACTIVE_BUTTON) {
        SnyggBox(
            elementName = elementName,
            attributes = attributes,
            selector = selector,
            modifier = modifier,
            clickAndSemanticsModifier = Modifier
                .aspectRatio(1f)
                .indication(interactionSource, LocalIndication.current)
                .pointerInput(action, isEnabled) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        if (isEnabled && type != QuickActionBarType.EDITOR_TILE) {
                            val press = PressInteraction.Press(down.position)
                            inputFeedbackController.keyPress(TextKeyData.UNSPECIFIED)
                            interactionSource.tryEmit(press)
                            action.onPointerDown(context)
                            val up = waitForUpOrCancellation()
                            if (up != null) {
                                up.consume()
                                interactionSource.tryEmit(PressInteraction.Release(press))
                                action.onPointerUp(context)
                            } else {
                                interactionSource.tryEmit(PressInteraction.Cancel(press))
                                action.onPointerCancel(context)
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (action) {
                    is QuickAction.InsertKey -> {
                        val metroIcon = remember(action, evaluator.version, evaluator.state) {
                            evaluator.computeMetroIcon(action.data)
                        }
                        val (imageVector, label) = remember(action, evaluator.version, evaluator.state) {
                            evaluator.computeImageVector(action.data) to evaluator.computeLabel(action.data)
                        }
                        if (metroIcon != null) {
                            val iconStyle = rememberSnyggThemeQuery(
                                elementName = "$elementName-icon",
                                attributes = attributes,
                                selector = selector,
                            )
                            val styledIconSize = with(LocalDensity.current) {
                                val fontSize = iconStyle.fontSize(default = 0.sp)
                                if (fontSize.isSp && fontSize >= 1.sp) {
                                    fontSize.toDp()
                                } else {
                                    MetroSystemIconDefaultSize
                                }
                            }
                            SnyggBox(
                                elementName = "$elementName-icon",
                                attributes = attributes,
                                selector = selector,
                            ) {
                                MetroSystemIcon(
                                    type = metroIcon,
                                    modifier = Modifier.size(styledIconSize),
                                    iconSize = styledIconSize,
                                    color = iconStyle.foreground(),
                                    showCircle = false,
                                )
                            }
                        } else if (imageVector != null) {
                            SnyggBox(
                                elementName = "$elementName-icon",
                                attributes = attributes,
                                selector = selector,
                            ) {
                                SnyggIcon(imageVector = imageVector)
                            }
                        } else if (label != null) {
                            SnyggText(
                                elementName = "$elementName-text",
                                attributes = attributes,
                                selector = selector,
                                text = label,
                            )
                        }
                    }

                    is QuickAction.InsertText -> {
                        SnyggText(
                            elementName = "$elementName-text",
                            attributes = attributes,
                            selector = selector,
                            text = action.data.firstOrNull().toString().ifBlank { "?" },
                        )
                    }
                }

                if (type != QuickActionBarType.INTERACTIVE_BUTTON) {
                    SnyggText(
                        elementName = "$elementName-text",
                        attributes = attributes,
                        selector = selector,
                        text = action.computeDisplayName(evaluator = evaluator),
                    )
                }
            }
        }
    }
}

/**
 * WP8.1 Start-style square action tile: 0dp corners, accent fill, 8dp title inset.
 */
@Composable
private fun MetroQuickActionTile(
    action: QuickAction,
    evaluator: ComputingEvaluator,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val inputFeedbackController = LocalInputFeedbackController.current
    val interactionSource = remember { MutableInteractionSource() }
    val isEnabled = evaluator.evaluateEnabled(action.keyData())
    val tileBackground = remember(action.keyData().code) {
        QuickActionTileColors.backgroundFor(action.keyData().code)
    }
    val contentColor = remember(tileBackground) {
        QuickActionTileColors.contentFor(tileBackground)
    }
    val displayName = action.computeDisplayName(evaluator = evaluator)

    DisposableEffect(action, isEnabled) {
        onDispose {
            if (action is QuickAction.InsertKey) {
                action.onPointerCancel(context)
            }
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(tileBackground, RectangleShape)
            .indication(interactionSource, LocalIndication.current)
            .pointerInput(action, isEnabled) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    if (isEnabled) {
                        val press = PressInteraction.Press(down.position)
                        inputFeedbackController.keyPress(TextKeyData.UNSPECIFIED)
                        interactionSource.tryEmit(press)
                        action.onPointerDown(context)
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            up.consume()
                            interactionSource.tryEmit(PressInteraction.Release(press))
                            action.onPointerUp(context)
                        } else {
                            interactionSource.tryEmit(PressInteraction.Cancel(press))
                            action.onPointerCancel(context)
                        }
                    }
                }
            }
            .padding(8.dp),
    ) {
        val metroIcon = remember(action, evaluator.version, evaluator.state) {
            when (action) {
                is QuickAction.InsertKey -> evaluator.computeMetroIcon(action.data)
                is QuickAction.InsertText -> null
            }
        }
        val imageVector = remember(action, evaluator.version, evaluator.state) {
            when (action) {
                is QuickAction.InsertKey -> evaluator.computeImageVector(action.data)
                is QuickAction.InsertText -> null
            }
        }
        MetroQuickActionTileGlyph(
            metroIcon = metroIcon,
            imageVector = imageVector,
            fallbackLabel = when (action) {
                is QuickAction.InsertText -> action.data.firstOrNull()?.toString()?.ifBlank { "?" } ?: "?"
                is QuickAction.InsertKey -> evaluator.computeLabel(action.data)
            },
            contentColor = contentColor,
            enabled = isEnabled,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 16.dp),
        )
        BasicText(
            text = displayName,
            style = MetroTextStyle.DialogBody.toTextStyle().copy(
                color = contentColor.copy(
                    alpha = if (isEnabled) contentColor.alpha else 0.48f,
                ),
                fontSize = 15.sp,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

@Composable
private fun MetroQuickActionTileGlyph(
    metroIcon: MetroSystemIconType?,
    imageVector: ImageVector?,
    fallbackLabel: String?,
    contentColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = contentColor.copy(alpha = if (enabled) contentColor.alpha else 0.48f)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            metroIcon != null -> {
                MetroSystemIcon(
                    type = metroIcon,
                    modifier = Modifier.size(32.dp),
                    iconSize = 32.dp,
                    color = color,
                    showCircle = false,
                )
            }
            imageVector != null -> {
                Image(
                    painter = rememberVectorPainter(imageVector),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    colorFilter = ColorFilter.tint(color),
                )
            }
            !fallbackLabel.isNullOrBlank() -> {
                BasicText(
                    text = fallbackLabel,
                    style = MetroTextStyle.Body.toTextStyle().copy(color = color),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
