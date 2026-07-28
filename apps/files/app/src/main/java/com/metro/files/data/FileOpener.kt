package com.metro.files.data

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File

object StorageAccess {
    fun hasFullAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val permission = android.Manifest.permission.READ_EXTERNAL_STORAGE
            context.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun manageAllFilesIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    "package:${context.packageName}".toUri(),
                )
            } catch (_: Exception) {
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:${context.packageName}".toUri()
            }
        }
    }
}

object FileOpener {
    /**
     * Opens [entry] with the system-associated app via [Intent.ACTION_VIEW] + FileProvider.
     * Uses the specific MIME type first, then falls back to a wildcard MIME so generic viewers can match.
     */
    fun open(context: Context, entry: FileEntry): OpenResult {
        if (entry.kind != FileKind.FILE) {
            return OpenResult.NotAFile
        }
        val file = File(entry.absolutePath)
        if (!file.exists() || !file.isFile) {
            return OpenResult.Missing
        }
        val mime = entry.mimeType ?: FilesLogic.mimeTypeForName(entry.name)
        val uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.files",
                file,
            )
        } catch (_: IllegalArgumentException) {
            return OpenResult.Denied
        }

        val typed = viewIntent(uri, mime)
        val launch = when {
            canHandle(context, typed) -> typed
            mime != "*/*" -> {
                val any = viewIntent(uri, "*/*")
                if (canHandle(context, any)) any else null
            }
            else -> null
        } ?: return OpenResult.NoHandler

        if (context !is Activity) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            // Prefer the default / sole handler for this MIME (respective app).
            // If several apps match, the system shows its disambiguation UI.
            context.startActivity(launch)
            OpenResult.Started
        } catch (_: ActivityNotFoundException) {
            OpenResult.NoHandler
        } catch (_: SecurityException) {
            OpenResult.Denied
        }
    }

    private fun viewIntent(uri: Uri, mime: String): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            // ClipData keeps FLAG_GRANT_* alive across chooser / resolver hops (API 24+).
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun canHandle(context: Context, intent: Intent): Boolean {
        val pm = context.packageManager
        val matches = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        if (matches.isNotEmpty()) return true
        // Some handlers are non-default; still openable via the system resolver.
        val any = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return any.isNotEmpty()
    }

    enum class OpenResult {
        Started,
        NoHandler,
        Missing,
        Denied,
        NotAFile,
    }
}
