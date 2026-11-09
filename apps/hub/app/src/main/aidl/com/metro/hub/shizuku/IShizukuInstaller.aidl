package com.metro.hub.shizuku;

interface IShizukuInstaller {
    /** Streams [apk] into a privileged PackageInstaller session and commits with [intentSender]. */
    void install(in AssetFileDescriptor apk, in IntentSender intentSender) = 1;

    /** Shizuku UserService destroy hook — must call System.exit. */
    void destroy() = 16777114;
}
