package com.metro.feedback.data

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object GitHubIssueLauncher {
    const val OWNER = "god-s-perfect-idiot"
    const val REPO = "metro-os"

    /** Keep under common browser / Intent URL limits. */
    private const val MAX_URL_CHARS = 7000

    fun openIssue(
        context: Context,
        title: String,
        logcatFile: File?,
    ) {
        val body = buildBody(context, title, logcatFile)
        val trimmedTitle = title.trim()

        // Clipboard backup — WebView injection was wiping the GitHub form; URL prefill is primary.
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("metro-os feedback", body))

        val url = buildIssueUrl(trimmedTitle, body)
        val view = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(view)
        } catch (error: ActivityNotFoundException) {
            throw IllegalStateException("No browser available to open GitHub", error)
        }
    }

    internal fun buildIssueUrl(title: String, body: String): String {
        val base = "https://github.com/$OWNER/$REPO/issues/new"
        val titlePart = urlEncode(title)
        val prefix = "$base?title=$titlePart&body="
        val budget = (MAX_URL_CHARS - prefix.length).coerceAtLeast(256)
        val encodedBody = urlEncode(body)
        val bodyPart = if (encodedBody.length <= budget) {
            encodedBody
        } else {
            val truncated = body.take(budget / 3) +
                "\n\n… truncated for URL length. Full text is on the clipboard — paste into the issue body.\n"
            urlEncode(truncated).take(budget)
        }
        return prefix + bodyPart
    }

    internal fun buildBody(context: Context, title: String, logcatFile: File?): String {
        val meta = buildString {
            appendLine("## Feedback")
            appendLine()
            appendLine(title.trim())
            appendLine()
            appendLine("## Device")
            appendLine()
            appendLine("- Manufacturer: ${Build.MANUFACTURER}")
            appendLine("- Model: ${Build.MODEL}")
            appendLine("- Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("- Feedback app: ${context.packageName}")
            appendLine()
        }

        if (logcatFile == null || !logcatFile.exists()) {
            return meta + "_(No metro logcat attached.)_\n"
        }

        val raw = logcatFile.readText()
        return buildString {
            append(meta)
            appendLine("## Metro logcat")
            appendLine()
            appendLine("File: `${logcatFile.name}` (${formatBytes(logcatFile.length())})")
            appendLine()
            appendLine("```text")
            append(raw.trimEnd())
            appendLine()
            appendLine("```")
        }
    }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(java.util.Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(java.util.Locale.US, "%.1f MB", mb)
    }
}
