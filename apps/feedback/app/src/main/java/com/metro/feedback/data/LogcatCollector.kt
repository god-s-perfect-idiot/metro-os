package com.metro.feedback.data

import android.content.Context
import android.content.pm.PackageManager
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class LogcatCapture(
    val file: File,
    val lineCount: Int,
    val byteCount: Long,
)

object LogcatCollector {
    private const val METRO_PREFIX = "com.metro."
    private val metroPackageHint = Regex("""com\.metro\.[a-z0-9_.]+""", RegexOption.IGNORE_CASE)
    /** threadtime lines look like `… E MetroTheme: …` — keep suite tags. */
    private val metroTagHint = Regex("""\s[VDIWEF]\s+Metro[A-Za-z0-9_]*:""")

    fun collect(context: Context): LogcatCapture {
        val metroPackages = installedMetroPackages(context)
        val raw = dumpLogcat()
        val filtered = filterMetroLines(raw, metroPackages)
        val dir = File(context.cacheDir, "logcat").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val file = File(dir, "metro-logcat-$stamp.txt")
        val header = buildString {
            appendLine("# metro-os logcat (filtered to metro apps)")
            appendLine("# captured: $stamp")
            appendLine("# packages: ${metroPackages.sorted().joinToString(", ")}")
            appendLine()
        }
        val body = if (filtered.isEmpty()) {
            "# (no matching metro log lines in this dump)\n"
        } else {
            filtered.joinToString("\n", postfix = "\n")
        }
        file.writeText(header + body)
        return LogcatCapture(
            file = file,
            lineCount = filtered.size,
            byteCount = file.length(),
        )
    }

    fun installedMetroPackages(context: Context): Set<String> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .map { it.packageName }
            .filter { it.startsWith(METRO_PREFIX) }
            .toSet()
    }

    internal fun filterMetroLines(raw: String, metroPackages: Set<String>): List<String> {
        if (raw.isBlank()) return emptyList()
        val packageTokens = metroPackages.toList()
        return raw.lineSequence()
            .map { it.trimEnd() }
            .filter { line ->
                if (line.isBlank() || line.startsWith("---")) return@filter false
                if (metroPackageHint.containsMatchIn(line)) return@filter true
                if (metroTagHint.containsMatchIn(line)) return@filter true
                packageTokens.any { pkg -> line.contains(pkg) }
            }
            .toList()
    }

    private fun dumpLogcat(): String {
        val process = ProcessBuilder("logcat", "-d", "-v", "threadtime")
            .redirectErrorStream(true)
            .start()
        val output = StringBuilder()
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }
        }
        process.waitFor(15, TimeUnit.SECONDS)
        if (process.isAlive) {
            process.destroyForcibly()
        }
        return output.toString()
    }
}
