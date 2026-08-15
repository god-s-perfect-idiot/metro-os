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

package dev.patrickgold.florisboard.ime.keyboard

import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import com.metro.ui.MetroSystemIconType
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.DisplayLanguageNamesIn
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.editor.FlorisEditorInfo
import dev.patrickgold.florisboard.ime.editor.ImeOptions
import dev.patrickgold.florisboard.ime.input.InputShiftState
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyType
import dev.patrickgold.florisboard.ime.window.ImeWindowMode
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.lib.compose.vectorResource

interface ComputingEvaluator {
    val version: Int

    val keyboard: Keyboard

    val editorInfo: FlorisEditorInfo

    val state: KeyboardState

    val subtype: Subtype

    fun context(): Context?

    fun displayLanguageNamesIn(): DisplayLanguageNamesIn

    fun evaluateEnabled(data: KeyData): Boolean

    fun evaluateVisible(data: KeyData): Boolean

    fun isSlot(data: KeyData): Boolean

    fun slotData(data: KeyData): KeyData?
}

object DefaultComputingEvaluator : ComputingEvaluator {
    override val version = -1

    override val keyboard = PlaceholderLoadingKeyboard

    override val editorInfo = FlorisEditorInfo.Unspecified

    override val state = KeyboardState.new()

    override val subtype = Subtype.DEFAULT

    override fun context(): Context? = null

    override fun displayLanguageNamesIn() = DisplayLanguageNamesIn.NATIVE_LOCALE

    override fun evaluateEnabled(data: KeyData): Boolean = true

    override fun evaluateVisible(data: KeyData): Boolean = true

    override fun isSlot(data: KeyData): Boolean = false

    override fun slotData(data: KeyData): KeyData? = null
}

private var cachedDisplayNameState = Triple(FlorisLocale.ROOT, DisplayLanguageNamesIn.SYSTEM_LOCALE, "")

/**
 * Compute language name with a cache to prevent repetitive calling of `locale.displayName()`, which invokes the
 * underlying `LocaleNative.getLanguageName()` method and in turn uses the rather slow ICU data table to look up the
 * language name. This only caches the last display name, but that's more than enough, as a one-time re-computation when
 * the subtype changes does not hurt, the repetitive computation for the same language hurts.
 */
private fun computeLanguageDisplayName(locale: FlorisLocale, displayLanguageNamesIn: DisplayLanguageNamesIn): String {
    val (cachedLocale, cachedDisplayLanguageNamesIn, cachedDisplayName) = cachedDisplayNameState
    if (cachedLocale == locale && cachedDisplayLanguageNamesIn == displayLanguageNamesIn) {
        return cachedDisplayName
    }
    val displayName = when (displayLanguageNamesIn) {
        DisplayLanguageNamesIn.SYSTEM_LOCALE -> locale.displayName()
        DisplayLanguageNamesIn.NATIVE_LOCALE -> locale.displayName(locale)
    }
    cachedDisplayNameState = Triple(locale, displayLanguageNamesIn, displayName)
    return displayName
}

fun ComputingEvaluator.computeLabel(data: KeyData): String? {
    val evaluator = this
    return if (data.type == KeyType.CHARACTER && data.code != KeyCode.SPACE && data.code != KeyCode.CJK_SPACE
        && data.code != KeyCode.HALF_SPACE && data.code != KeyCode.KESHIDA || data.type == KeyType.NUMERIC
    ) {
        data.asString(isForDisplay = true)
    } else {
        when (data.code) {
            KeyCode.PHONE_PAUSE -> evaluator.context()?.getString(R.string.key__phone_pause)
            KeyCode.PHONE_WAIT -> evaluator.context()?.getString(R.string.key__phone_wait)
            KeyCode.SPACE, KeyCode.CJK_SPACE -> {
                when (evaluator.keyboard.mode) {
                    KeyboardMode.CHARACTERS -> evaluator.subtype.primaryLocale.let { locale ->
                        computeLanguageDisplayName(locale, evaluator.displayLanguageNamesIn())
                    }
                    else -> null
                }
            }
            KeyCode.IME_UI_MODE_TEXT,
            KeyCode.VIEW_CHARACTERS -> {
                evaluator.context()?.getString(R.string.key__view_characters)
            }
            KeyCode.VIEW_NUMERIC,
            KeyCode.VIEW_NUMERIC_ADVANCED -> {
                evaluator.context()?.getString(R.string.key__view_numeric)
            }
            KeyCode.VIEW_PHONE -> {
                evaluator.context()?.getString(R.string.key__view_phone)
            }
            KeyCode.VIEW_PHONE2 -> {
                evaluator.context()?.getString(R.string.key__view_phone2)
            }
            KeyCode.VIEW_SYMBOLS -> {
                evaluator.context()?.getString(R.string.key__view_symbols)
            }
            KeyCode.VIEW_SYMBOLS2 -> {
                evaluator.context()?.getString(R.string.key__view_symbols2)
            }
            KeyCode.HALF_SPACE -> {
                evaluator.context()?.getString(R.string.key__view_half_space)
            }
            KeyCode.KESHIDA -> {
                evaluator.context()?.getString(R.string.key__view_keshida)
            }
            else -> null
        }
    }
}

fun ComputingEvaluator.computeImageVector(data: KeyData): ImageVector? {
    val evaluator = this
    // Prefer [computeMetroIcon] for suite chrome; only custom drawables remain here.
    if (evaluator.computeMetroIcon(data) != null) return null
    return when (data.code) {
        KeyCode.COMPACT_LAYOUT_TO_LEFT,
        KeyCode.COMPACT_LAYOUT_TO_RIGHT,
        KeyCode.TOGGLE_COMPACT_LAYOUT -> {
            context()?.vectorResource(id = R.drawable.ic_accessibility_one_handed)
        }
        KeyCode.TOGGLE_FLOATING_WINDOW -> {
            val enabledIcon = context()?.vectorResource(id = R.drawable.ic_floating_keyboard)
            val disabledIcon = context()?.vectorResource(id = R.drawable.ic_floating_keyboard_disable)
            val windowController = FlorisImeService.windowControllerOrNull() ?: return enabledIcon
            when (windowController.activeWindowConfig.value.mode) {
                ImeWindowMode.FIXED -> enabledIcon
                ImeWindowMode.FLOATING -> disabledIcon
            }
        }
        KeyCode.TOGGLE_INCOGNITO_MODE -> {
            if (evaluator.state.isIncognitoMode) {
                this.context()?.vectorResource(id = R.drawable.ic_incognito)
            } else {
                this.context()?.vectorResource(id = R.drawable.ic_incognito_off)
            }
        }
        KeyCode.KANA_SWITCHER -> {
            if (evaluator.state.isKanaKata) {
                this.context()?.vectorResource(R.drawable.ic_keyboard_kana_switcher_kata)
            } else {
                this.context()?.vectorResource(R.drawable.ic_keyboard_kana_switcher_hira)
            }
        }
        KeyCode.CHAR_WIDTH_SWITCHER -> {
            if (evaluator.state.isCharHalfWidth) {
                this.context()?.vectorResource(R.drawable.ic_keyboard_char_width_switcher_full)
            } else {
                this.context()?.vectorResource(R.drawable.ic_keyboard_char_width_switcher_half)
            }
        }
        KeyCode.CHAR_WIDTH_FULL -> {
            this.context()?.vectorResource(R.drawable.ic_keyboard_char_width_switcher_full)
        }
        KeyCode.CHAR_WIDTH_HALF -> {
            this.context()?.vectorResource(R.drawable.ic_keyboard_char_width_switcher_half)
        }
        else -> null
    }
}

/**
 * WP8.1 chrome icons from the shared metro-ui toolkit (SIP keys + smartbar extra actions).
 */
fun ComputingEvaluator.computeMetroIcon(data: KeyData): MetroSystemIconType? {
    val evaluator = this
    return when (data.code) {
        KeyCode.ARROW_LEFT -> MetroSystemIconType.ChevronLeft
        KeyCode.ARROW_RIGHT -> MetroSystemIconType.ChevronRight
        KeyCode.ARROW_UP -> MetroSystemIconType.ChevronUp
        KeyCode.ARROW_DOWN -> MetroSystemIconType.ChevronDown
        KeyCode.CLIPBOARD_COPY -> MetroSystemIconType.Copy
        KeyCode.CLIPBOARD_CUT -> MetroSystemIconType.Cut
        KeyCode.CLIPBOARD_PASTE -> MetroSystemIconType.Paste
        KeyCode.CLIPBOARD_SELECT_ALL -> MetroSystemIconType.SelectAll
        KeyCode.CLIPBOARD_CLEAR_PRIMARY_CLIP -> MetroSystemIconType.Delete
        KeyCode.VOICE_INPUT -> MetroSystemIconType.Microphone
        KeyCode.IME_HIDE_UI -> MetroSystemIconType.KeyboardHide
        KeyCode.DELETE,
        KeyCode.FORWARD_DELETE -> MetroSystemIconType.Backspace
        KeyCode.IME_UI_MODE_MEDIA -> MetroSystemIconType.Emoji
        KeyCode.IME_UI_MODE_CLIPBOARD -> MetroSystemIconType.Clipboard
        KeyCode.LANGUAGE_SWITCH -> MetroSystemIconType.Language
        KeyCode.SETTINGS -> MetroSystemIconType.Settings
        KeyCode.UNDO -> MetroSystemIconType.Undo
        KeyCode.REDO -> MetroSystemIconType.Redo
        KeyCode.TOGGLE_ACTIONS_OVERFLOW -> MetroSystemIconType.More
        KeyCode.TOGGLE_AUTOCORRECT -> MetroSystemIconType.Autocorrect
        KeyCode.TOGGLE_RESIZE_MODE -> MetroSystemIconType.Resize
        KeyCode.DRAG_MARKER -> {
            if (evaluator.state.debugShowDragAndDropHelpers) MetroSystemIconType.Close else null
        }
        KeyCode.NOOP -> MetroSystemIconType.Close
        KeyCode.SHIFT -> when (evaluator.state.inputShiftState) {
            InputShiftState.CAPS_LOCK -> MetroSystemIconType.ShiftLocked
            else -> MetroSystemIconType.Shift
        }
        KeyCode.ENTER -> {
            val imeOptions = evaluator.editorInfo.imeOptions
            val inputAttributes = evaluator.editorInfo.inputAttributes
            if (imeOptions.flagNoEnterAction || inputAttributes.flagTextMultiLine) {
                MetroSystemIconType.Enter
            } else {
                when (imeOptions.action) {
                    ImeOptions.Action.DONE -> MetroSystemIconType.Check
                    ImeOptions.Action.GO,
                    ImeOptions.Action.NEXT -> MetroSystemIconType.Forward
                    ImeOptions.Action.PREVIOUS -> MetroSystemIconType.Back
                    ImeOptions.Action.SEARCH -> MetroSystemIconType.Search
                    ImeOptions.Action.SEND -> MetroSystemIconType.Send
                    ImeOptions.Action.NONE,
                    ImeOptions.Action.UNSPECIFIED -> MetroSystemIconType.Enter
                }
            }
        }
        else -> null
    }
}
