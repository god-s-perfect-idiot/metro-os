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

import androidx.compose.ui.graphics.Color
import com.metro.ui.MetroColors
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import kotlin.math.abs

/**
 * WP8.1 Start-style tile fills for smartbar overflow / editor action tiles.
 * Each action gets a stable accent from the official 20-color palette (never pure black/white).
 */
object QuickActionTileColors {
    private val PreferredByCode: Map<Int, Color> = mapOf(
        KeyCode.VOICE_INPUT to MetroColors.AccentCyan,
        KeyCode.UNDO to MetroColors.AccentCobalt,
        KeyCode.REDO to MetroColors.AccentIndigo,
        KeyCode.SETTINGS to MetroColors.AccentSteel,
        KeyCode.TOGGLE_FLOATING_WINDOW to MetroColors.AccentTeal,
        KeyCode.TOGGLE_RESIZE_MODE to MetroColors.AccentEmerald,
        KeyCode.IME_UI_MODE_CLIPBOARD to MetroColors.AccentOrange,
        KeyCode.IME_UI_MODE_MEDIA to MetroColors.AccentMagenta,
        KeyCode.TOGGLE_COMPACT_LAYOUT to MetroColors.AccentViolet,
        KeyCode.TOGGLE_AUTOCORRECT to MetroColors.AccentGreen,
        KeyCode.TOGGLE_INCOGNITO_MODE to MetroColors.AccentMauve,
        KeyCode.ARROW_UP to MetroColors.AccentLime,
        KeyCode.ARROW_DOWN to MetroColors.AccentBrown,
        KeyCode.ARROW_LEFT to MetroColors.AccentAmber,
        KeyCode.ARROW_RIGHT to MetroColors.AccentYellow,
        KeyCode.CLIPBOARD_CLEAR_PRIMARY_CLIP to MetroColors.AccentCrimson,
        KeyCode.CLIPBOARD_COPY to MetroColors.AccentPink,
        KeyCode.CLIPBOARD_CUT to MetroColors.AccentRed,
        KeyCode.CLIPBOARD_PASTE to MetroColors.AccentTaupe,
        KeyCode.CLIPBOARD_SELECT_ALL to MetroColors.AccentOlive,
        KeyCode.LANGUAGE_SWITCH to MetroColors.AccentCobalt,
        KeyCode.FORWARD_DELETE to MetroColors.AccentMagenta,
        KeyCode.IME_HIDE_UI to MetroColors.AccentSteel,
        KeyCode.TOGGLE_ACTIONS_OVERFLOW to MetroColors.AccentCyan,
    )

    fun backgroundFor(code: Int): Color {
        PreferredByCode[code]?.let { return it }
        val palette = MetroColors.AccentPalette
        return palette[abs(code) % palette.size]
    }

    fun contentFor(background: Color): Color = MetroColors.tileContentColor(background)
}
