package com.metro.hub.shizuku

import android.annotation.SuppressLint
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.content.res.AssetFileDescriptor
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.UserHandle
import rikka.shizuku.SystemServiceHelper
import java.io.OutputStream
import kotlin.system.exitProcess

/**
 * Shizuku UserService — runs as shell (ADB) or root and commits PackageInstaller sessions
 * without the stock confirm UI.
 *
 * Both constructors are required: Shizuku v13 prefers the [android.content.Context] one;
 * older hosts use empty.
 */
class ShizukuInstallerService : IShizukuInstaller.Stub {
    @Suppress("unused")
    constructor()

    @Suppress("unused")
    constructor(context: android.content.Context)

    private val userId = UserHandle::class.java
        .getMethod("myUserId")
        .invoke(null) as Int

    private val packageName = "com.metro.hub"

    @SuppressLint("PrivateApi")
    override fun install(apk: AssetFileDescriptor, intentSender: IntentSender) {
        val pmInterface = Class.forName("android.content.pm.IPackageManager\$Stub")
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, SystemServiceHelper.getSystemService("package"))

        val packageInstaller = Class.forName("android.content.pm.IPackageManager")
            .getMethod("getPackageInstaller")
            .invoke(pmInterface)

        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            val installFlags = this::class.java.getField("installFlags")
            installFlags.set(
                this,
                installFlags.getInt(this) or REPLACE_EXISTING_INSTALL_FLAG,
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                setPackageSource(PackageInstaller.PACKAGE_SOURCE_STORE)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                setInstallerPackageName(packageName)
            }
        }

        val sessionId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            packageInstaller::class.java.getMethod(
                "createSession",
                PackageInstaller.SessionParams::class.java,
                String::class.java,
                String::class.java,
                Int::class.java,
            ).invoke(packageInstaller, params, packageName, packageName, userId) as Int
        } else {
            packageInstaller::class.java.getMethod(
                "createSession",
                PackageInstaller.SessionParams::class.java,
                String::class.java,
                Int::class.java,
            ).invoke(packageInstaller, params, packageName, userId) as Int
        }

        val session = packageInstaller::class.java
            .getMethod("openSession", Int::class.java)
            .invoke(packageInstaller, sessionId)

        session::class.java.getMethod(
            "openWrite",
            String::class.java,
            Long::class.java,
            Long::class.java,
        )
            .invoke(session, "base.apk", 0L, apk.length)
            .let { it as ParcelFileDescriptor }
            .let { fd ->
                val revocable = Class.forName("android.os.SystemProperties")
                    .getMethod("getBoolean", String::class.java, Boolean::class.java)
                    .invoke(null, "fw.revocable_fd", false) as Boolean

                if (revocable) {
                    ParcelFileDescriptor.AutoCloseOutputStream(fd)
                } else {
                    Class.forName("android.os.FileBridge\$FileBridgeOutputStream")
                        .getConstructor(ParcelFileDescriptor::class.java)
                        .newInstance(fd) as OutputStream
                }
            }
            .use { output ->
                apk.createInputStream().use { input -> input.copyTo(output) }
            }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            session::class.java.getMethod("commit", IntentSender::class.java, Boolean::class.java)
                .invoke(session, intentSender, false)
        } else {
            session::class.java.getMethod("commit", IntentSender::class.java)
                .invoke(session, intentSender)
        }
    }

    override fun destroy() {
        exitProcess(0)
    }
}

/** Hidden PackageManager flag — replace an existing package. */
private const val REPLACE_EXISTING_INSTALL_FLAG = 0x00000002
