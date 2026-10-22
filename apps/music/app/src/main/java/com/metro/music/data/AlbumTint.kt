package com.metro.music.data

import android.content.Context
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Derives hub colours from album art:
 * - [tintArgb] — darkened single wash (legacy; hub backdrop now uses a clear artist photo).
 * - [brandGradientArgb] — bright left→right stops for the panoramic brand title gradient.
 */
object AlbumTintLogic {
    /** 30° buckets — wide enough that dithered gradients land together, narrow enough to keep hue. */
    private const val HueBuckets = 12
    private const val MinAlpha = 128
    private const val MinPixelWeight = 0.01f
    private const val MinSaturation = 0.35f
    private const val MaxSaturation = 0.85f
    private const val TintValue = 0.20f
    private const val NeutralValue = 0.12f
    /** Brand-title ink — vivid enough to read as a gradient over a dark hub. */
    private const val BrandMinSaturation = 0.45f
    private const val BrandMaxSaturation = 0.95f
    private const val BrandValue = 0.92f
    private const val BrandStops = 5
    private const val BrandMinDistinctStops = 2

    /** Returns the packed ARGB backdrop for [pixels] (any sample grid), or null when unusable. */
    fun tintArgb(pixels: IntArray): Int? {
        val weights = FloatArray(HueBuckets)
        val hueSin = FloatArray(HueBuckets)
        val hueCos = FloatArray(HueBuckets)
        val saturations = FloatArray(HueBuckets)
        var opaquePixels = 0

        for (pixel in pixels) {
            if ((pixel ushr 24 and 0xFF) < MinAlpha) continue
            opaquePixels++
            val red = (pixel ushr 16 and 0xFF) / 255f
            val green = (pixel ushr 8 and 0xFF) / 255f
            val blue = (pixel and 0xFF) / 255f
            val (hue, saturation, value) = rgbToHsv(red, green, blue)
            // Colourful mid-tones decide the hue; black borders and grey noise barely count.
            val weight = saturation * value
            if (weight <= MinPixelWeight) continue
            val bucket = (hue / 360f * HueBuckets).toInt().coerceIn(0, HueBuckets - 1)
            val radians = Math.toRadians(hue.toDouble())
            weights[bucket] += weight
            hueSin[bucket] += weight * sin(radians).toFloat()
            hueCos[bucket] += weight * cos(radians).toFloat()
            saturations[bucket] += weight * saturation
        }

        if (opaquePixels == 0) return null
        val bucket = weights.indices.maxByOrNull { weights[it] } ?: return null
        // Greyscale or near-black art has no hue to borrow — fall back to a neutral dark backdrop.
        if (weights[bucket] <= 0f) return hsvToArgb(0f, 0f, NeutralValue)

        val hue = (Math.toDegrees(
            atan2(hueSin[bucket].toDouble(), hueCos[bucket].toDouble()),
        ).toFloat() + 360f) % 360f
        val saturation = (saturations[bucket] / weights[bucket])
            .coerceIn(MinSaturation, MaxSaturation)
        return hsvToArgb(hue, saturation, TintValue)
    }

    /**
     * Left→right brand-title colours from a [width]×[height] pixel grid (row-major).
     * Returns null when the art is greyscale / too dull for a readable gradient (caller paints white).
     */
    fun brandGradientArgb(pixels: IntArray, width: Int, height: Int): IntArray? {
        if (width <= 0 || height <= 0 || pixels.size < width * height) return null
        val stops = IntArray(BrandStops)
        var colourful = 0
        for (stop in 0 until BrandStops) {
            val x0 = stop * width / BrandStops
            val x1 = ((stop + 1) * width / BrandStops).coerceAtLeast(x0 + 1)
            val color = columnBrandArgb(pixels, width, height, x0, x1)
            if (color == null) return null
            stops[stop] = color
            colourful++
        }
        if (colourful < BrandMinDistinctStops) return null
        // Collapse near-identical columns (solid-colour art) — not a gradient.
        val distinct = stops.toSet().size
        if (distinct < BrandMinDistinctStops) return null
        return stops
    }

    private fun columnBrandArgb(
        pixels: IntArray,
        width: Int,
        height: Int,
        x0: Int,
        x1: Int,
    ): Int? {
        val weights = FloatArray(HueBuckets)
        val hueSin = FloatArray(HueBuckets)
        val hueCos = FloatArray(HueBuckets)
        val saturations = FloatArray(HueBuckets)
        for (y in 0 until height) {
            val row = y * width
            for (x in x0 until x1) {
                val pixel = pixels[row + x]
                if ((pixel ushr 24 and 0xFF) < MinAlpha) continue
                val red = (pixel ushr 16 and 0xFF) / 255f
                val green = (pixel ushr 8 and 0xFF) / 255f
                val blue = (pixel and 0xFF) / 255f
                val (hue, saturation, value) = rgbToHsv(red, green, blue)
                val weight = saturation * value
                if (weight <= MinPixelWeight) continue
                val bucket = (hue / 360f * HueBuckets).toInt().coerceIn(0, HueBuckets - 1)
                val radians = Math.toRadians(hue.toDouble())
                weights[bucket] += weight
                hueSin[bucket] += weight * sin(radians).toFloat()
                hueCos[bucket] += weight * cos(radians).toFloat()
                saturations[bucket] += weight * saturation
            }
        }
        val bucket = weights.indices.maxByOrNull { weights[it] } ?: return null
        if (weights[bucket] <= 0f) return null
        val hue = (Math.toDegrees(
            atan2(hueSin[bucket].toDouble(), hueCos[bucket].toDouble()),
        ).toFloat() + 360f) % 360f
        val saturation = (saturations[bucket] / weights[bucket])
            .coerceIn(BrandMinSaturation, BrandMaxSaturation)
        return hsvToArgb(hue, saturation, BrandValue)
    }

    private fun rgbToHsv(red: Float, green: Float, blue: Float): Triple<Float, Float, Float> {
        val max = maxOf(red, green, blue)
        val min = minOf(red, green, blue)
        val delta = max - min
        val hue = when {
            delta == 0f -> 0f
            max == red -> 60f * (((green - blue) / delta) % 6f)
            max == green -> 60f * (((blue - red) / delta) + 2f)
            else -> 60f * (((red - green) / delta) + 4f)
        }
        val saturation = if (max == 0f) 0f else delta / max
        return Triple((hue + 360f) % 360f, saturation, max)
    }

    private fun hsvToArgb(hue: Float, saturation: Float, value: Float): Int {
        val sector = (hue % 360f) / 60f
        val chroma = value * saturation
        val second = chroma * (1f - kotlin.math.abs((sector % 2f) - 1f))
        val (red, green, blue) = when (sector.toInt()) {
            0 -> Triple(chroma, second, 0f)
            1 -> Triple(second, chroma, 0f)
            2 -> Triple(0f, chroma, second)
            3 -> Triple(0f, second, chroma)
            4 -> Triple(second, 0f, chroma)
            else -> Triple(chroma, 0f, second)
        }
        val base = value - chroma
        fun channel(component: Float) = ((component + base) * 255f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or
            (channel(red) shl 16) or
            (channel(green) shl 8) or
            channel(blue)
    }
}

/** Sample grid the art is downscaled to before hue counting. */
private const val TintSampleSize = 48

private suspend fun loadAlbumSamplePixels(
    context: Context,
    artworkModel: Any,
): Triple<IntArray, Int, Int>? {
    val request = ImageRequest.Builder(context)
        .data(artworkModel)
        .size(TintSampleSize)
        // Hardware bitmaps cannot be read back pixel by pixel.
        .allowHardware(false)
        .build()
    val bitmap = (context.imageLoader.execute(request) as? SuccessResult)
        ?.drawable
        ?.toBitmap()
        ?: return null
    val sample = if (bitmap.width > TintSampleSize || bitmap.height > TintSampleSize) {
        bitmap.scale(
            width = bitmap.width.coerceAtMost(TintSampleSize),
            height = bitmap.height.coerceAtMost(TintSampleSize),
        )
    } else {
        bitmap
    }
    val pixels = IntArray(sample.width * sample.height)
    sample.getPixels(pixels, 0, sample.width, 0, 0, sample.width, sample.height)
    return Triple(pixels, sample.width, sample.height)
}

/**
 * Loads [artworkModel] (any Coil model, including [LocalArtwork]) at thumbnail size and reduces it
 * to a single backdrop colour.
 */
suspend fun loadAlbumTintArgb(context: Context, artworkModel: Any): Int? {
    val (pixels, _, _) = loadAlbumSamplePixels(context, artworkModel) ?: return null
    return AlbumTintLogic.tintArgb(pixels)
}

/**
 * Loads left→right brand-title gradient stops from [artworkModel], or null when the art is too
 * dull / monochrome (caller should keep the default white brand).
 */
suspend fun loadAlbumBrandGradientArgb(context: Context, artworkModel: Any): IntArray? {
    val (pixels, width, height) = loadAlbumSamplePixels(context, artworkModel) ?: return null
    return AlbumTintLogic.brandGradientArgb(pixels, width, height)
}
