package com.metro.statusbar

import org.junit.Assert.assertEquals
import org.junit.Test

class TraySpecHorizontalPaddingTest {
    @Test
    fun center_usesBasePaddingsOnly() {
        val padding = TraySpec.horizontalPaddingDp(NotchPosition.Center)
        assertEquals(TraySpec.START_PADDING_DP, padding.left)
        assertEquals(TraySpec.END_PADDING_DP, padding.right)
    }

    @Test
    fun left_addsClearanceOnLeft() {
        val padding = TraySpec.horizontalPaddingDp(NotchPosition.Left)
        assertEquals(TraySpec.START_PADDING_DP + TraySpec.NOTCH_SIDE_CLEARANCE_DP, padding.left)
        assertEquals(TraySpec.END_PADDING_DP, padding.right)
    }

    @Test
    fun right_addsClearanceOnRight() {
        val padding = TraySpec.horizontalPaddingDp(NotchPosition.Right)
        assertEquals(TraySpec.START_PADDING_DP, padding.left)
        assertEquals(TraySpec.END_PADDING_DP + TraySpec.NOTCH_SIDE_CLEARANCE_DP, padding.right)
    }

    @Test
    fun systemInsets_winWhenLargerThanNotchPadding() {
        val padding = TraySpec.horizontalPaddingDp(
            notchPosition = NotchPosition.Left,
            systemLeftDp = 80,
            systemRightDp = 40,
        )
        assertEquals(80, padding.left)
        assertEquals(40, padding.right)
    }
}
