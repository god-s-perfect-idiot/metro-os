# Statusbar ProGuard rules

# Shizuku UserService — class name must stay stable for bindUserService.
-keep class com.metro.statusbar.shizuku.PrivilegedShellService { *; }
-keep class com.metro.statusbar.shizuku.IPrivilegedShell { *; }
-keep class com.metro.statusbar.shizuku.IPrivilegedShell$Stub { *; }
