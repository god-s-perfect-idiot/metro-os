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
 * Derives the now-playing backdrop from album art: take the art's dominant hue, then crush its
 * value so white Metro type keeps full contrast over it (WP8.1 faded the artist image the same way).
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

/**
 * Loads [artworkModel] (any Coil model, including [LocalArtwork]) at thumbnail size and reduces it
 * to a single backdrop colour.
 */
suspend fun loadAlbumTintArgb(context: Context, artworkModel: Any): Int? {
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
    return AlbumTintLogic.tintArgb(pixels)
}
