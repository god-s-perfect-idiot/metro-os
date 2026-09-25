package com.metro.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * One installed Android icon pack (ADW / GO / Nova theme intent).
 */
data class MetroIconPackInfo(
    val packageName: String,
    val label: String,
)

/**
 * Discovers third-party icon packs and resolves launch icons from their `appfilter.xml`.
 *
 * Packs advertise via `org.adw.launcher.THEMES`, `com.gau.go.launcherex.theme`, and
 * `com.novalauncher.THEME`. Active pack package is stored in
 * [MetroPreferences.iconPackPackage] (null / blank = system icons).
 *
 * Not a WP8.1 surface — Android launcher affordance exposed in Settings → start+theme.
 */
object MetroIconPacks {

    /** Common theme intent actions used by ADW, GO Launcher, Nova, etc. */
    val THEME_ACTIONS: List<String> = listOf(
        "org.adw.launcher.THEMES",
        "com.gau.go.launcherex.theme",
        "com.novalauncher.THEME",
        "com.teslacoilsw.launcher.THEME",
    )

    private data class PackCache(
        val resources: Resources,
        /** ComponentInfo{pkg/cls} → drawable resource name */
        val componentDrawables: Map<String, String>,
    )

    private val packCaches = ConcurrentHashMap<String, PackCache>()

    /** Clears parsed appfilter caches (call when the active pack changes or packs are updated). */
    fun clearCache() {
        packCaches.clear()
    }

    fun clearCache(packPackageName: String) {
        packCaches.remove(packPackageName)
    }

    /**
     * Installed icon packs, sorted by label. Dedupes by package when multiple theme intents
     * resolve to the same app.
     */
    fun listInstalled(context: Context): List<MetroIconPackInfo> {
        val pm = context.packageManager
        val byPackage = LinkedHashMap<String, MetroIconPackInfo>()
        for (action in THEME_ACTIONS) {
            val intent = Intent(action)
            val matches = runCatching {
                pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            }.getOrDefault(emptyList())
            for (info in matches) {
                val packageName = info.activityInfo?.packageName ?: continue
                if (packageName in byPackage) continue
                val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
                    .ifEmpty {
                        runCatching {
                            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
                        }.getOrDefault(packageName)
                    }
                byPackage[packageName] = MetroIconPackInfo(packageName = packageName, label = label)
            }
        }
        return byPackage.values.sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    /** Label for [packageName], or null when the pack is not installed / not a theme pack. */
    fun labelFor(context: Context, packageName: String?): String? {
        if (packageName.isNullOrBlank()) return null
        return listInstalled(context).firstOrNull { it.packageName == packageName }?.label
            ?: runCatching {
                val pm = context.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            }.getOrNull()
    }

    /**
     * Drawable from the active [MetroPreferences.iconPackPackage] for [appPackageName]'s
     * launch activity, or null when no pack is set / no mapping / load failed.
     */
    fun loadIconForPackage(context: Context, appPackageName: String): Drawable? {
        val packPackage = MetroPreferences(context).iconPackPackage ?: return null
        return loadIconForPackage(context, packPackage, appPackageName)
    }

    fun loadIconForPackage(
        context: Context,
        packPackageName: String,
        appPackageName: String,
    ): Drawable? {
        if (packPackageName.isBlank() || appPackageName.isBlank()) return null
        val pm = context.packageManager
        val launchComponent = pm.getLaunchIntentForPackage(appPackageName)?.component
            ?: return null
        val cache = packCaches[packPackageName] ?: loadPackCache(pm, packPackageName)?.also {
            packCaches[packPackageName] = it
        } ?: return null

        val componentKey = launchComponent.toString()
        val drawableName = cache.componentDrawables[componentKey]
            ?: cache.componentDrawables[componentKey.lowercase(Locale.US)]
            ?: fallbackDrawableName(launchComponent, packPackageName, cache)
            ?: return null

        return loadDrawable(cache.resources, packPackageName, drawableName)
    }

    private fun fallbackDrawableName(
        component: ComponentName,
        packPackageName: String,
        cache: PackCache,
    ): String? {
        // Some packs omit ComponentInfo{} and only ship drawable names derived from the class.
        val derived = buildString {
            append(component.packageName)
            append('_')
            append(component.className.removePrefix(component.packageName).removePrefix("."))
        }
            .lowercase(Locale.US)
            .replace('.', '_')
            .replace('/', '_')
        val id = cache.resources.getIdentifier(derived, "drawable", packPackageName)
        return derived.takeIf { id != 0 }
    }

    private fun loadDrawable(
        resources: Resources,
        packPackageName: String,
        drawableName: String,
    ): Drawable? {
        val id = resources.getIdentifier(drawableName, "drawable", packPackageName)
        if (id == 0) return null
        return runCatching {
            @Suppress("DEPRECATION")
            resources.getDrawable(id)
        }.getOrNull()?.constantState?.newDrawable()?.mutate()
            ?: runCatching {
                @Suppress("DEPRECATION")
                resources.getDrawable(id)
            }.getOrNull()
    }

    private fun loadPackCache(pm: PackageManager, packPackageName: String): PackCache? {
        val resources = runCatching {
            pm.getResourcesForApplication(packPackageName)
        }.getOrNull() ?: return null
        val parser = openAppFilter(resources, packPackageName) ?: return null
        val map = parseAppFilter(parser)
        return PackCache(resources = resources, componentDrawables = map)
    }

    private fun openAppFilter(resources: Resources, packPackageName: String): XmlPullParser? {
        val xmlId = resources.getIdentifier("appfilter", "xml", packPackageName)
        if (xmlId != 0) {
            return runCatching { resources.getXml(xmlId) }.getOrNull()
        }
        return runCatching {
            val stream = resources.assets.open("appfilter.xml")
            val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
            factory.newPullParser().also { it.setInput(stream, "utf-8") }
        }.getOrNull()
    }

    private fun parseAppFilter(parser: XmlPullParser): Map<String, String> {
        val map = HashMap<String, String>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name.equals("item", ignoreCase = true)) {
                val component = parser.getAttributeValue(null, "component")
                val drawable = parser.getAttributeValue(null, "drawable")
                if (!component.isNullOrBlank() && !drawable.isNullOrBlank()) {
                    map[component] = drawable
                    // Also index lowercase for packs that mix casing.
                    map.putIfAbsent(component.lowercase(Locale.US), drawable)
                }
            }
            event = parser.next()
        }
        return map
    }
}
