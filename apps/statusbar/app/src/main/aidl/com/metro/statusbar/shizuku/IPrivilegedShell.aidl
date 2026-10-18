package com.metro.statusbar.shizuku;

/**
 * Shizuku UserService — runs as shell (ADB) or root and executes privileged shell commands.
 * Transaction 16777114 is Shizuku's destroy hook (must call System.exit).
 */
interface IPrivilegedShell {
    /** Runs [command] via `sh -c`. Returns exit code; non-zero means failure. */
    int exec(String command) = 1;

    void destroy() = 16777114;
}
