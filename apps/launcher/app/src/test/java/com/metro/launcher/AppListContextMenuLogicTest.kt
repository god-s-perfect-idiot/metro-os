package com.metro.launcher

import com.metro.launcher.ui.AppListContextMenuLogic
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppListContextMenuLogicTest {
    @Test
    fun pinToStartEnabled_whenAppIsNotOnStart() {
        assertTrue(AppListContextMenuLogic.pinToStartEnabled(isPinned = false))
    }

    @Test
    fun pinToStartEnabled_whenAppAlreadyHasLiveTile() {
        assertFalse(AppListContextMenuLogic.pinToStartEnabled(isPinned = true))
    }
}
