package com.metro.metron.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions
import eu.kanade.tachiyomi.ui.library.LibraryItem
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.manga.model.asMangaCover

/** Brightness scale for the cover (1 = unchanged). Keeps art readable as a backdrop. */
private const val ArtworkDarkenScale = 0.42f

/** Extra black wash so white Metro chrome stays legible on bright covers. */
private val ArtworkScrim = Color.Black.copy(alpha = 0.40f)

private val ArtworkDarkenFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            ArtworkDarkenScale, 0f, 0f, 0f, 0f,
            0f, ArtworkDarkenScale, 0f, 0f, 0f,
            0f, 0f, ArtworkDarkenScale, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

/**
 * Picks one random library cover and keeps it stable until the library goes empty → non-empty.
 */
@Composable
fun rememberRandomLibraryCover(favorites: List<LibraryItem>): MangaCover? {
    return remember(favorites.isNotEmpty()) {
        favorites.randomOrNull()?.libraryManga?.manga?.asMangaCover()
    }
}

/**
 * Full-bleed comic cover behind hub / library chrome. Dark theme only — light pages keep
 * the solid theme background for contrast. Art is darkened (not blurred) so covers stay
 * recognizable while chrome remains readable.
 */
@Composable
fun MetronArtworkBackdrop(
    cover: MangaCover?,
    modifier: Modifier = Modifier,
) {
    val pageBackground = MetroTheme.colors.background
    val showArt = cover != null && pageBackground.luminance() < 0.5f
    val art = cover.takeIf { showArt }

    Crossfade(
        targetState = art,
        animationSpec = tween(durationMillis = MetroTransitions.PageTransitionMs),
        label = "metronArtworkBackdrop",
        modifier = modifier.fillMaxSize(),
    ) { model ->
        if (model != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = ArtworkDarkenFilter,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ArtworkScrim),
                )
            }
        }
    }
}
