package com.metro.feedback.data

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Debug
import android.os.Process
import android.os.storage.StorageManager
import android.provider.Settings
import com.metro.system.MetroMemoryContract
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class MemorySnapshot(
    val totalBytes: Long,
    val availBytes: Long,
) {
    val usedBytes: Long get() = (totalBytes - availBytes).coerceAtLeast(0L)
    val usedFraction: Float
        get() = if (totalBytes <= 0L) 0f else usedBytes.toFloat() / totalBytes.toFloat()
}

data class AppMetric(
    val packageName: String,
    val label: String,
    val ramBytes: Long,
    val cacheBytes: Long?,
    val dataBytes: Long? = null,
    val codeBytes: Long? = null,
) {
    /** Combined footprint (RAM + on-disk). List sort uses [ramBytes] descending. */
    val usageBytes: Long
        get() = ramBytes + (cacheBytes ?: 0L) + (dataBytes ?: 0L) + (codeBytes ?: 0L)

    fun ramFractionOf(totalRam: Long): Float {
        if (totalRam <= 0L || ramBytes <= 0L) return 0f
        return ramBytes.toFloat() / totalRam.toFloat()
    }
}

data class MetricsSnapshot(
    val memory: MemorySnapshot,
    val apps: List<AppMetric>,
    val hasUsageAccess: Boolean,
) {
    val totalCacheBytes: Long
        get() = apps.mapNotNull { it.cacheBytes }.sum()
}

object MetricsRepository {
    private const val METRO_PREFIX = "com.metro."

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            context.startActivity(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun snapshot(context: Context): MetricsSnapshot {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val memory = MemorySnapshot(
            totalBytes = memInfo.totalMem,
            availBytes = memInfo.availMem,
        )

        val pm = context.packageManager
        val metroApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.packageName.startsWith(METRO_PREFIX) }
        val metroPackages = metroApps.map { it.packageName }.toSet()

        val ramByPackage = ramByPackage(context, am, metroPackages)
        val usageAccess = hasUsageAccess(context)
        val storageByPackage = if (usageAccess) {
            storageByPackage(context, metroApps)
        } else {
            emptyMap()
        }

        val apps = metroApps.map { info ->
            val pkg = info.packageName
            val storage = storageByPackage[pkg]
            AppMetric(
                packageName = pkg,
                label = labelOf(pm, info),
                ramBytes = ramByPackage[pkg] ?: 0L,
                cacheBytes = if (usageAccess) storage?.cacheBytes else null,
                dataBytes = if (usageAccess) storage?.dataBytes else null,
                codeBytes = if (usageAccess) storage?.codeBytes else null,
            )
        }.sortedWith(
            compareByDescending<AppMetric> { it.ramBytes }
                .thenByDescending { it.cacheBytes ?: 0L }
                .thenBy { it.label.lowercase() },
        )

        return MetricsSnapshot(
            memory = memory,
            apps = apps,
            hasUsageAccess = usageAccess,
        )
    }

    private fun labelOf(pm: PackageManager, info: ApplicationInfo): String {
        return try {
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            info.packageName.removePrefix(METRO_PREFIX)
        }
    }

    /**
     * Stock Android blocks third-party [dumpsys] meminfo and cross-UID /proc reads.
     * Suite apps expose PSS via [MetroMemoryProvider] (SDK manifest merge).
     */
    private fun ramByPackage(
        context: Context,
        am: ActivityManager,
        metroPackages: Set<String>,
    ): Map<String, Long> {
        val merged = HashMap<String, Long>()

        // 1) Suite ContentProvider probe (works for apps rebuilt with current SDK).
        val resolver = context.contentResolver
        for (pkg in metroPackages) {
            if (!MetroMemoryContract.hasProvider(context.packageManager, pkg)) continue
            val bytes = MetroMemoryContract.readPssBytes(resolver, pkg) ?: continue
            if (bytes > 0L) merged[pkg] = bytes
        }

        // 2) Self via Debug (always accurate for this process).
        if (context.packageName in metroPackages) {
            val info = Debug.MemoryInfo()
            Debug.getMemoryInfo(info)
            val self = info.totalPss.toLong() * 1024L
            if (self > (merged[context.packageName] ?: 0L)) {
                merged[context.packageName] = self
            }
        }

        // 3) dumpsys — only works with DUMP permission (system/debug); keep as bonus.
        if (merged.size < 2) {
            val fromDumpsys = runCatching {
                parseMeminfoPss(runDumpsys("meminfo"), metroPackages)
            }.getOrDefault(emptyMap())
            for ((pkg, bytes) in fromDumpsys) {
                if (bytes > (merged[pkg] ?: 0L)) merged[pkg] = bytes
            }
        }

        // 4) ActivityManager — usually only our UID on modern Android.
        for ((pkg, bytes) in runningRamByPackage(am, metroPackages)) {
            if (bytes > (merged[pkg] ?: 0L)) merged[pkg] = bytes
        }

        return merged
    }

    private fun runningRamByPackage(
        am: ActivityManager,
        metroPackages: Set<String>,
    ): Map<String, Long> {
        val processes = am.runningAppProcesses ?: return emptyMap()
        val pidToPackage = LinkedHashMap<Int, String>()
        for (process in processes) {
            val pkg = process.pkgList?.firstOrNull { it in metroPackages }
                ?: process.processName.takeIf { it in metroPackages }
                ?: metroPackages.find { process.processName.startsWith("$it:") }
            if (pkg != null) {
                pidToPackage[process.pid] = pkg
            }
        }
        if (pidToPackage.isEmpty()) return emptyMap()

        val pids = pidToPackage.keys.toIntArray()
        val memInfos = am.getProcessMemoryInfo(pids) ?: return emptyMap()
        val totals = HashMap<String, Long>()
        pids.forEachIndexed { index, pid ->
            val pkg = pidToPackage[pid] ?: return@forEachIndexed
            val info = memInfos.getOrNull(index) ?: return@forEachIndexed
            val bytes = info.totalPss.toLong() * 1024L
            totals[pkg] = (totals[pkg] ?: 0L) + bytes
        }
        return totals
    }

    private data class PackageStorage(
        val cacheBytes: Long,
        val dataBytes: Long,
        val codeBytes: Long,
    )

    private fun storageByPackage(
        context: Context,
        apps: List<ApplicationInfo>,
    ): Map<String, PackageStorage> {
        val statsManager = context.getSystemService(StorageStatsManager::class.java) ?: return emptyMap()
        val user = Process.myUserHandle()
        val out = HashMap<String, PackageStorage>()
        for (app in apps) {
            try {
                val stats = statsManager.queryStatsForPackage(
                    StorageManager.UUID_DEFAULT,
                    app.packageName,
                    user,
                )
                out[app.packageName] = PackageStorage(
                    cacheBytes = stats.cacheBytes,
                    dataBytes = stats.dataBytes,
                    codeBytes = stats.appBytes,
                )
            } catch (_: Exception) {
                // Missing package or permission race — skip.
            }
        }
        return out
    }

    private fun runDumpsys(vararg args: String): String {
        val command = ArrayList<String>(args.size + 1).apply {
            add("dumpsys")
            addAll(args)
        }
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        val output = StringBuilder()
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }
        }
        process.waitFor(20, TimeUnit.SECONDS)
        if (process.isAlive) {
            process.destroyForcibly()
        }
        return output.toString()
    }

    /**
     * Parses summary lines like `45231 kB: com.metro.launcher` or modern `45231K: com.metro.launcher`.
     * Prefers the `Total PSS by process` block when present so RSS+PSS are not double-counted.
     */
    internal fun parseMeminfoPss(dump: String, metroPackages: Set<String>): Map<String, Long> {
        if (dump.isBlank() || metroPackages.isEmpty()) return emptyMap()
        val section = extractMeminfoProcessSection(dump)
        val lineRe = Regex(
            """^\s*([\d,]+)\s*[kK][bB]?:\s*(com\.metro\.[A-Za-z0-9_.]+)""",
            setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE),
        )
        val totals = HashMap<String, Long>()
        for (match in lineRe.findAll(section)) {
            val kb = match.groupValues[1].replace(",", "").toLongOrNull() ?: continue
            val processName = match.groupValues[2]
            val pkg = resolveMetroPackage(processName, metroPackages) ?: continue
            totals[pkg] = (totals[pkg] ?: 0L) + kb * 1024L
        }
        return totals
    }

    private fun extractMeminfoProcessSection(dump: String): String {
        val pssIdx = dump.indexOf("Total PSS by process", ignoreCase = true)
        val rssIdx = dump.indexOf("Total RSS by process", ignoreCase = true)
        val start = when {
            pssIdx >= 0 -> pssIdx
            rssIdx >= 0 -> rssIdx
            else -> return dump
        }
        // Stop before the next major section when possible.
        val nextMarkers = listOf(
            "Total PSS by OOM adjustment",
            "Total RSS by OOM adjustment",
            "Total PSS by category",
            "Total RSS by category",
            "Total PSS by process:",
            "Total RSS by process:",
        )
        var end = dump.length
        for (marker in nextMarkers) {
            val idx = dump.indexOf(marker, startIndex = start + 10, ignoreCase = true)
            if (idx > start && idx < end) end = idx
        }
        return dump.substring(start, end)
    }

    /** Parses `TOTAL PSS: 20205` from a per-package `dumpsys meminfo <pkg>` dump. */
    internal fun parsePackageTotalPss(dump: String): Long? {
        val totalPss = Regex("""TOTAL PSS:\s*([\d,]+)""", RegexOption.IGNORE_CASE)
            .find(dump)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace(",", "")
            ?.toLongOrNull()
        if (totalPss != null) return totalPss * 1024L

        val appSummary = Regex(
            """^\s*TOTAL:\s*([\d,]+)""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
        )
        return appSummary.findAll(dump).lastOrNull()?.groupValues?.getOrNull(1)
            ?.replace(",", "")
            ?.toLongOrNull()
            ?.times(1024L)
    }

    private fun resolveMetroPackage(processName: String, metroPackages: Set<String>): String? {
        if (processName in metroPackages) return processName
        return metroPackages.find { processName.startsWith("$it:") }
    }

    fun formatBytes(bytes: Long): String = GitHubIssueLauncher.formatBytes(bytes)
}
