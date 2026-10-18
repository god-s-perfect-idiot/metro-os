package com.metro.statusbar

import com.metro.statusbar.shizuku.SystemStatusBarHider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemStatusBarHiderTest {
    @Test
    fun hideCommand_disablesSystemIconsWithoutImmersivePolicy() {
        val command = SystemStatusBarHider.HIDE_COMMAND
        assertTrue(command.contains("send-disable-flag"))
        assertTrue(command.contains("system-icons"))
        assertTrue(command.contains("clock"))
        assertTrue(command.contains("notification-icons"))
        // Immersive status policy would hide WindowInsets status bars and creep the Metro tray.
        assertFalse(command.contains("policy_control"))
        assertFalse(command.contains("immersive.status"))
    }

    @Test
    fun restoreCommand_clearsDisableFlags() {
        val command = SystemStatusBarHider.RESTORE_COMMAND
        assertTrue(command.contains("send-disable-flag none"))
        assertFalse(command.contains("immersive.status"))
    }
}
