package com.metro.launcher.data

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.core.graphics.createBitmap

/** One App Widget provider offered by a package for the tile customize list. */
data class TileWidgetOption(
    val provider: ComponentName,
    val label: String,
    val previewImage: Int,
    val minWidth: Int,
    val minHeight: Int,
    /** Provider preview (or icon fallback) for the customize picker; null when unavailable. */
    val previewBitmap: Bitmap? = null,
)

/**
 * Discovers App Widget providers for a package and owns the Start [AppWidgetHost] lifecycle.
 */
class TileAppWidgetController(context: Context) {
    private val appContext = context.applicationContext
    private val manager = AppWidgetManager.getInstance(appContext)
    val host: AppWidgetHost = AppWidgetHost(appContext, HOST_ID)

    fun startListening() {
        host.startListening()
    }

    fun stopListening() {
        host.stopListening()
    }

    fun listProvidersForPackage(packageName: String): List<TileWidgetOption> {
        val pm = appContext.packageManager
        val density = appContext.resources.displayMetrics.densityDpi
        return manager.installedProviders
            .asSequence()
            .filter { it.provider.packageName == packageName }
            .map { info ->
                val preview = info.loadPreviewImage(appContext, density)
                    ?: info.loadIcon(appContext, density)
                TileWidgetOption(
                    provider = info.provider,
                    label = info.loadLabel(pm)?.toString()
                        ?: info.provider.shortClassName.substringAfterLast('.'),
                    previewImage = info.previewImage,
                    minWidth = info.minWidth,
                    minHeight = info.minHeight,
                    previewBitmap = preview?.let { drawableToBitmap(it) },
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun providerInfo(flattened: String?): AppWidgetProviderInfo? {
        val component = flattened?.let { ComponentName.unflattenFromString(it) } ?: return null
        return manager.installedProviders.firstOrNull { it.provider == component }
    }

    fun createHostView(appWidgetId: Int, info: AppWidgetProviderInfo): AppWidgetHostView {
        val view = host.createView(appContext, appWidgetId, info)
        view.setAppWidget(appWidgetId, info)
        // Medium / wide Start tiles: tell the widget the available cell budget.
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, info.minWidth.coerceAtLeast(110))
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, info.minHeight.coerceAtLeast(110))
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 480)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 280)
        }
        view.updateAppWidgetOptions(options)
        return view
    }

    /**
     * Allocates and binds [provider]. Returns the new id, or null when the system bind prompt
     * must run first ([bindRequestIntent] is then non-null for the caller).
     */
    fun allocateAndBind(
        provider: ComponentName,
        bindRequestIntent: ((Intent) -> Unit),
    ): Int? {
        val id = host.allocateAppWidgetId()
        val bound = manager.bindAppWidgetIdIfAllowed(id, provider)
        if (bound) return id
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
        }
        bindRequestIntent(intent)
        return null
    }

    fun deleteAppWidgetId(id: Int) {
        if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
            host.deleteAppWidgetId(id)
        }
    }

    fun configurationIntent(appWidgetId: Int, info: AppWidgetProviderInfo): Intent? {
        val configure = info.configure ?: return null
        return Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
            component = configure
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
    }

    companion object {
        /** Stable host id within com.metro.launcher — do not change once widgets are allocated. */
        const val HOST_ID = 0x4D455452 // 'METR'

        /** Cap preview decode so customize list stays light. */
        private const val MaxPreviewEdgePx = 512

        private fun drawableToBitmap(drawable: Drawable): Bitmap {
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                return drawable.bitmap
            }
            val rawW = drawable.intrinsicWidth.coerceAtLeast(1)
            val rawH = drawable.intrinsicHeight.coerceAtLeast(1)
            val scale = minOf(1f, MaxPreviewEdgePx.toFloat() / maxOf(rawW, rawH))
            val w = (rawW * scale).toInt().coerceAtLeast(1)
            val h = (rawH * scale).toInt().coerceAtLeast(1)
            val bitmap = createBitmap(w, h)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, w, h)
            drawable.draw(canvas)
            return bitmap
        }
    }
}
