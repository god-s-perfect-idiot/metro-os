package com.metro.photos.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.photos.R
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroStatusBarFullscreenEffect
import com.metro.ui.MetroSystemIcon
import com.metro.ui.MetroSystemIconType
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.launch

/** Slightly inset share/delete so they read lighter than heart in the viewer bar. */
private val ViewerActionGlyphSize = 34.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ViewerScreen(
    state: PhotosState,
    modifier: Modifier = Modifier,
) {
    val photos = state.viewerPhotos
    if (photos.isEmpty()) {
        LaunchedEffect(Unit) { state.navigateBack() }
        return
    }

    val context = LocalContext.current
    // Chrome visible → system tray + soft keys stay up. Tap photo → immersive fullscreen.
    var chromeVisible by remember { mutableStateOf(true) }
    MetroStatusBarFullscreenEffect(active = !chromeVisible)

    val pagerState = rememberPagerState(
        initialPage = state.viewerIndex.coerceIn(0, photos.lastIndex),
        pageCount = { photos.size },
    )
    val scope = rememberCoroutineScope()

    val deleteConsentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val id = state.viewerPhotos.getOrNull(state.viewerIndex)?.id ?: return@rememberLauncherForActivityResult
            state.onPhotoDeleted(id)
        }
    }

    LaunchedEffect(state.pendingDeleteConsent) {
        val sender = state.consumeDeleteConsent() ?: return@LaunchedEffect
        deleteConsentLauncher.launch(IntentSenderRequest.Builder(sender).build())
    }

    LaunchedEffect(pagerState.currentPage) {
        state.updateViewerIndex(pagerState.currentPage)
    }

    // Keep pager in range after delete.
    LaunchedEffect(photos.size, state.viewerIndex) {
        val target = state.viewerIndex.coerceIn(0, photos.lastIndex)
        if (pagerState.currentPage != target && photos.isNotEmpty()) {
            pagerState.scrollToPage(target)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(if (chromeVisible) Modifier.statusBarsPadding() else Modifier),
    ) {
        // Pager scroll is disabled — ZoomablePhoto owns pinch and page swipes so
        // HorizontalPager cannot steal two-finger gestures.
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            ZoomablePhoto(
                uri = photos[page].uri,
                isActive = page == pagerState.currentPage,
                modifier = Modifier.fillMaxSize(),
                onTap = { chromeVisible = !chromeVisible },
                onSwipeToAdjacent = { direction ->
                    val target = (pagerState.currentPage + direction)
                        .coerceIn(0, photos.lastIndex)
                    if (target != pagerState.currentPage) {
                        scope.launch { pagerState.animateScrollToPage(target) }
                    }
                },
            )
        }

        val current = photos.getOrNull(pagerState.currentPage)
        val favorite = current?.let { state.isFavorite(it.id) } == true
        val shareLabel = stringResource(R.string.share_photo)
        val deleteLabel = stringResource(R.string.delete_photo)
        val favoriteLabel = stringResource(R.string.add_favorite)
        val unfavoriteLabel = stringResource(R.string.remove_favorite)
        MetroAppBar(
            visible = chromeVisible,
            icons = listOf(
                MetroAppBarIcon(
                    label = shareLabel,
                    onClick = { state.shareCurrent(context) },
                    contentDescription = shareLabel,
                    enabled = current != null,
                    icon = { color ->
                        MetroSystemIcon(
                            type = MetroSystemIconType.Share,
                            iconSize = MetroAppBarDefaults.GlyphSize,
                            glyphSize = ViewerActionGlyphSize,
                            color = color,
                            showCircle = false,
                        )
                    },
                ),
                if (favorite) {
                    MetroAppBarIcon(
                        type = MetroSystemIconType.HeartSlash,
                        label = unfavoriteLabel,
                        onClick = { current?.let { state.removeFavorite(it.id) } },
                        contentDescription = unfavoriteLabel,
                        enabled = current != null,
                    )
                } else {
                    MetroAppBarIcon(
                        type = MetroSystemIconType.Heart,
                        label = favoriteLabel,
                        onClick = { current?.let { state.addFavorite(it.id) } },
                        contentDescription = favoriteLabel,
                        enabled = current != null,
                    )
                },
                MetroAppBarIcon(
                    label = deleteLabel,
                    onClick = { state.deleteCurrent() },
                    contentDescription = deleteLabel,
                    enabled = current != null,
                    icon = { color ->
                        MetroSystemIcon(
                            type = MetroSystemIconType.Delete,
                            iconSize = MetroAppBarDefaults.GlyphSize,
                            glyphSize = ViewerActionGlyphSize,
                            color = color,
                            showCircle = false,
                        )
                    },
                ),
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .metroNavBarPadding(),
        )
    }
}
