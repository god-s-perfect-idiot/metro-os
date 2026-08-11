/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.text.keyboard

import dev.patrickgold.florisboard.ime.keyboard.KeyboardMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

class TextKeyboardSpacingTest : FunSpec({
    val keyboardWidth = 1000f
    val keyboardHeight = 400f
    val keyMargin = 4f
    val gutter = keyMargin * 2f
    val tolerance = 0.05f

    fun letterKey(): TextKey = TextKey(TextKeyData.UNSPECIFIED).apply {
        flayWidthFactor = 1.0f
        flayShrink = 1.0f
        flayGrow = 0.0f
    }

    fun layout(rows: Array<Array<TextKey>>): TextKeyboard {
        val keyboard = TextKeyboard(
            arrangement = rows,
            mode = KeyboardMode.CHARACTERS,
            extendedPopupMapping = null,
            extendedPopupMappingDefault = null,
        )
        val desired = TextKey(TextKeyData.UNSPECIFIED).apply {
            touchBounds.apply {
                width = keyboardWidth / 10f
                height = keyboardHeight / rows.size
            }
            visibleBounds.applyFrom(touchBounds).deflateBy(keyMargin, keyMargin)
        }
        keyboard.layout(keyboardWidth, keyboardHeight, desired, extendTouchBoundariesDownwards = false)
        return keyboard
    }

    test("row and column gutters are equal, including outer edges") {
        val keyboard = layout(
            arrayOf(
                Array(10) { letterKey() },
                Array(10) { letterKey() },
                Array(10) { letterKey() },
                Array(10) { letterKey() },
            ),
        )
        val rows = keyboard.arrangement

        val columnGaps = rows[0].toList().zipWithNext { a, b ->
            b.visibleBounds.left - a.visibleBounds.right
        }
        columnGaps.forEach { it shouldBe (gutter plusOrMinus tolerance) }

        val rowGaps = rows.toList().zipWithNext { top, bottom ->
            bottom[0].visibleBounds.top - top[0].visibleBounds.bottom
        }
        rowGaps.forEach { it shouldBe (gutter plusOrMinus tolerance) }

        rows[0][0].visibleBounds.left shouldBe (gutter plusOrMinus tolerance)
        rows[0].last().visibleBounds.right shouldBe ((keyboardWidth - gutter) plusOrMinus tolerance)
        rows[0][0].visibleBounds.top shouldBe (gutter plusOrMinus tolerance)
        rows.last()[0].visibleBounds.bottom shouldBe ((keyboardHeight - gutter) plusOrMinus tolerance)
    }

    test("inset 9-key row keeps the same inner gutter as the 10-key row") {
        val keyboard = layout(
            arrayOf(
                Array(10) { letterKey() },
                Array(9) { letterKey() },
            ),
        )
        val row1Gaps = keyboard.arrangement[0].toList().zipWithNext { a, b ->
            b.visibleBounds.left - a.visibleBounds.right
        }
        val row2Gaps = keyboard.arrangement[1].toList().zipWithNext { a, b ->
            b.visibleBounds.left - a.visibleBounds.right
        }
        row1Gaps.forEach { it shouldBe (gutter plusOrMinus tolerance) }
        row2Gaps.forEach { it shouldBe (gutter plusOrMinus tolerance) }

        val rowGap = keyboard.arrangement[1][0].visibleBounds.top -
            keyboard.arrangement[0][0].visibleBounds.bottom
        rowGap shouldBe (gutter plusOrMinus tolerance)
    }
})
