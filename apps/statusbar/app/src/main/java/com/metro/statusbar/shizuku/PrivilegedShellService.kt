package com.metro.statusbar.shizuku

import android.content.Context
import kotlin.system.exitProcess

/**
 * Shizuku UserService implementation. Runs in a privileged process (shell UID 2000 or root).
 * Both constructors are required: Shizuku v13 prefers the [Context] one; older hosts use empty.
 */
class PrivilegedShellService : IPrivilegedShell.Stub {
    @Suppress("unused")
    constructor()

    @Suppress("unused")
    constructor(context: Context)

    override fun exec(command: String?): Int {
        if (command.isNullOrBlank()) return 1
        return try {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start()
            // Drain so a full pipe cannot deadlock waitFor.
            process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
        } catch (_: Exception) {
            1
        }
    }

    override fun destroy() {
        exitProcess(0)
    }
}
