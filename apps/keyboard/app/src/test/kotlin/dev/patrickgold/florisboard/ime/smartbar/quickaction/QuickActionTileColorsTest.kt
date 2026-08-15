package dev.patrickgold.florisboard.ime.smartbar.quickaction

import com.metro.ui.MetroColors
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class QuickActionTileColorsTest : FunSpec({
    test("settings and floating use distinct WP accent fills") {
        val settings = QuickActionTileColors.backgroundFor(KeyCode.SETTINGS)
        val floating = QuickActionTileColors.backgroundFor(KeyCode.TOGGLE_FLOATING_WINDOW)
        settings shouldBe MetroColors.AccentSteel
        floating shouldBe MetroColors.AccentTeal
        settings shouldNotBe floating
    }

    test("tile content is white on steel and black on yellow") {
        QuickActionTileColors.contentFor(MetroColors.AccentSteel) shouldBe MetroColors.TileContentOnAccent
        QuickActionTileColors.contentFor(MetroColors.AccentYellow) shouldBe MetroColors.LightPrimaryText
    }

    test("unknown codes still resolve to a palette accent") {
        val color = QuickActionTileColors.backgroundFor(-4242)
        (color in MetroColors.AccentPalette) shouldBe true
    }
})
