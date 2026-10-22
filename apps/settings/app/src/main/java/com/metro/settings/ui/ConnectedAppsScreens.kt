package com.metro.settings.ui

import android.content.ContentUris
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.metro.settings.R
import com.metro.settings.data.GalleryAlbumCatalog
import com.metro.settings.data.GalleryAlbumEntry
import com.metro.settings.data.InstalledAppEntry
import com.metro.system.MetroAppBranding
import com.metro.system.MetroPreferences
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppGlyphs
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroCheckBox
import com.metro.ui.MetroColors
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListItem
import com.metro.ui.MetroMultiSelectItem
import com.metro.ui.MetroMultiSelectList
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

private val AlbumCoverPlaceholder = Color(0xFF333333)
private val AlbumPickerCheckboxScrim = Color(0x66000000)

private val ConnectedHubRowHeight = 90.dp
private val ConnectedAppIconSize = 40.dp
private val ConnectedAppIconInset = 4.dp
private val ConnectedHubGlyphSize = 52.dp
private val ConnectedSelectedRowHeight = 56.dp

/**
 * Settings → connected apps hub (WP8.1 email+account layout: icon + title + subtitle).
 */
@Composable
fun ConnectedAppsScreen(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        MetroSettingsHeader(pageTitle = stringResource(R.string.settings_connected_apps))
        SettingsBodyText(text = stringResource(R.string.settings_connected_apps_intro))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                ConnectedHubRow(
                    glyphRes = MetroAppGlyphs.Photos,
                    title = stringResource(R.string.settings_gallery_apps),
                    subtitle = stringResource(R.string.settings_gallery_apps_subtitle),
                    onClick = { state.open(SettingsRoute.GalleryApps) },
                )
            }
            item {
                ConnectedHubRow(
                    glyphRes = MetroAppGlyphs.Music,
                    title = stringResource(R.string.settings_music_apps),
                    subtitle = stringResource(R.string.settings_music_apps_subtitle),
                    onClick = { state.open(SettingsRoute.MusicApps) },
                )
            }
            item {
                ConnectedHubRow(
                    glyphRes = MetroAppGlyphs.Conversations,
                    title = stringResource(R.string.settings_conversation_apps),
                    subtitle = stringResource(R.string.settings_conversation_apps_subtitle),
                    onClick = { state.open(SettingsRoute.ConversationApps) },
                )
            }
        }
    }
}

@Composable
private fun ConnectedHubRow(
    glyphRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    MetroListItem(
        title = title,
        subtitle = subtitle,
        modifier = Modifier.height(ConnectedHubRowHeight),
        leading = {
            Image(
                painter = painterResource(glyphRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(MetroTheme.colors.primaryText),
                modifier = Modifier.size(ConnectedHubGlyphSize),
            )
        },
        onClick = onClick,
    )
}

/**
 * Gallery / Music / Conversation apps page — WP8.1 apps corner: selected apps + "tap to select apps".
 * Gallery also lists selected live-tile albums.
 */
@Composable
fun ConnectedAppListScreen(
    state: SettingsState,
    kind: ConnectedAppKind,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val packages = when (kind) {
        ConnectedAppKind.Gallery -> state.galleryAppPackages
        ConnectedAppKind.Music -> state.musicAppPackages
        ConnectedAppKind.Conversation -> state.conversationAppPackages
    }
    val selected = remember(packages, state.applicationEntries) {
        packages.mapNotNull { pkg ->
            state.applicationEntries.firstOrNull { it.packageName == pkg }
                ?: state.applications.loadApp(pkg)
        }.sortedBy { it.title.lowercase() }
    }
    val pageTitle = when (kind) {
        ConnectedAppKind.Gallery -> stringResource(R.string.settings_gallery_apps_title)
        ConnectedAppKind.Music -> stringResource(R.string.settings_music_apps_title)
        ConnectedAppKind.Conversation -> stringResource(R.string.settings_conversation_apps_title)
    }
    val intro = when (kind) {
        ConnectedAppKind.Gallery -> stringResource(R.string.settings_gallery_apps_intro)
        ConnectedAppKind.Music -> stringResource(R.string.settings_music_apps_intro)
        ConnectedAppKind.Conversation -> stringResource(R.string.settings_conversation_apps_intro)
    }
    val pickerRoute = when (kind) {
        ConnectedAppKind.Gallery -> SettingsRoute.GalleryAppPicker
        ConnectedAppKind.Music -> SettingsRoute.MusicAppPicker
        ConnectedAppKind.Conversation -> SettingsRoute.ConversationAppPicker
    }
    var albums by remember { mutableStateOf<List<GalleryAlbumEntry>>(emptyList()) }
    var albumsReady by remember { mutableStateOf(false) }
    var albumsEpoch by remember { mutableStateOf(0) }
    var hasAlbumPermission by remember {
        mutableStateOf(GalleryAlbumCatalog.hasMediaPermission(context))
    }
    if (kind == ConnectedAppKind.Gallery && hasAlbumPermission) {
        LaunchedEffect(albumsEpoch) {
            // Keep prior rows visible while refreshing so titles/covers don't flash to IDs.
            val next = withContext(Dispatchers.IO) { GalleryAlbumCatalog.listAlbums(context) }
            albums = next
            albumsReady = true
        }
    }
    // Only resolve once the catalog is ready — never fall back to raw bucket IDs.
    val selectedAlbums = remember(albums, albumsReady, state.galleryAlbumBucketIds) {
        if (!albumsReady) {
            emptyList()
        } else {
            val byId = albums.associateBy { it.bucketId }
            state.galleryAlbumBucketIds.mapNotNull { id -> byId[id] }
                .sortedBy { it.name.lowercase() }
        }
    }
    val albumsSectionPending = kind == ConnectedAppKind.Gallery &&
        hasAlbumPermission &&
        !albumsReady &&
        state.galleryAlbumBucketIds.isNotEmpty()
    var openAlbumPickerAfterGrant by remember { mutableStateOf(false) }
    val requestAlbumPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasAlbumPermission = granted
        if (granted) {
            albumsReady = false
            albumsEpoch += 1
            if (openAlbumPickerAfterGrant) {
                openAlbumPickerAfterGrant = false
                state.open(SettingsRoute.GalleryAlbumPicker)
            }
        } else {
            openAlbumPickerAfterGrant = false
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        MetroSettingsHeader(pageTitle = pageTitle)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                SettingsBodyText(text = intro)
            }
            if (selected.isEmpty()) {
                item {
                    MetroText(
                        text = stringResource(R.string.settings_connected_apps_empty),
                        style = MetroTextStyle.Body,
                        color = MetroTheme.colors.secondaryText,
                        modifier = Modifier.padding(
                            horizontal = MetroDimens.ScreenHorizontalMargin,
                            vertical = 8.dp,
                        ),
                    )
                }
            } else {
                items(selected, key = { it.packageName }) { entry ->
                    ConnectedSelectedAppRow(entry = entry)
                }
            }
            item {
                MetroListItem(
                    title = stringResource(R.string.settings_connected_apps_section),
                    subtitle = stringResource(R.string.settings_connected_apps_tap_select),
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .height(ConnectedHubRowHeight),
                    onClick = { state.open(pickerRoute) },
                )
            }
            if (kind == ConnectedAppKind.Gallery) {
                if (!hasAlbumPermission) {
                    item {
                        MetroText(
                            text = stringResource(R.string.settings_gallery_albums_need_permission),
                            style = MetroTextStyle.Body,
                            color = MetroTheme.colors.secondaryText,
                            modifier = Modifier.padding(
                                horizontal = MetroDimens.ScreenHorizontalMargin,
                                vertical = 8.dp,
                            ),
                        )
                    }
                    item {
                        MetroBorderButton(
                            text = stringResource(R.string.settings_gallery_albums_allow),
                            onClick = {
                                requestAlbumPermission.launch(GalleryAlbumCatalog.requiredPermission())
                            },
                            modifier = Modifier.padding(
                                horizontal = MetroDimens.ScreenHorizontalMargin,
                                vertical = 4.dp,
                            ),
                        )
                    }
                } else if (albumsSectionPending) {
                    // Hold the section quiet until names + cover URIs are resolved.
                } else if (selectedAlbums.isEmpty()) {
                    item {
                        MetroText(
                            text = stringResource(R.string.settings_gallery_albums_empty),
                            style = MetroTextStyle.Body,
                            color = MetroTheme.colors.secondaryText,
                            modifier = Modifier.padding(
                                horizontal = MetroDimens.ScreenHorizontalMargin,
                                vertical = 8.dp,
                            ),
                        )
                    }
                } else {
                    items(selectedAlbums, key = { it.bucketId }) { album ->
                        ConnectedSelectedAlbumRow(album = album)
                    }
                }
                item {
                    MetroListItem(
                        title = stringResource(R.string.settings_gallery_albums_section),
                        subtitle = stringResource(R.string.settings_gallery_albums_tap_select),
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .height(ConnectedHubRowHeight),
                        onClick = {
                            if (hasAlbumPermission) {
                                state.open(SettingsRoute.GalleryAlbumPicker)
                            } else {
                                openAlbumPickerAfterGrant = true
                                requestAlbumPermission.launch(GalleryAlbumCatalog.requiredPermission())
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectedSelectedAppRow(entry: InstalledAppEntry) {
    MetroListItem(
        title = entry.title,
        modifier = Modifier.height(ConnectedSelectedRowHeight),
        leading = {
            ConnectedAppIcon(packageName = entry.packageName, label = entry.title)
        },
        verticalPadding = 6.dp,
        oneLineMinHeight = ConnectedSelectedRowHeight,
    )
}

@Composable
private fun ConnectedSelectedAlbumRow(album: GalleryAlbumEntry) {
    MetroListItem(
        title = album.name,
        modifier = Modifier.height(ConnectedSelectedRowHeight),
        leading = {
            ConnectedAlbumCover(album = album)
        },
        verticalPadding = 6.dp,
        oneLineMinHeight = ConnectedSelectedRowHeight,
    )
}

@Composable
private fun ConnectedAlbumCover(album: GalleryAlbumEntry) {
    Box(
        modifier = Modifier
            .size(ConnectedAppIconSize)
            .background(AlbumCoverPlaceholder)
            .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        if (album.coverUri != null) {
            AlbumCoverImage(
                uri = album.coverUri,
                contentDescription = album.name,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            MetroText(
                text = album.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MetroTextStyle.ListItemTitle,
                color = Color.White,
            )
        }
    }
}

/**
 * Album cover image grid for gallery live tiles — Photos-style 2-column tiles with
 * multi-select checkboxes (not a folder / Apps Corner list).
 */
@Composable
fun GalleryAlbumPickerScreen(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var albums by remember { mutableStateOf<List<GalleryAlbumEntry>>(emptyList()) }
    LaunchedEffect(Unit) {
        albums = withContext(Dispatchers.IO) { GalleryAlbumCatalog.listAlbums(context) }
    }
    val committed = state.galleryAlbumBucketIds
    var draft by remember { mutableStateOf(committed) }
    LaunchedEffect(committed) {
        draft = committed
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MetroAppTitle(title = stringResource(R.string.settings_gallery_albums_select_title))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 8.dp,
                    bottom = 88.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(albums, key = { it.bucketId }) { album ->
                    val selected = album.bucketId in draft
                    AlbumPickerTile(
                        album = album,
                        selected = selected,
                        onClick = {
                            draft = if (selected) {
                                draft - album.bucketId
                            } else {
                                draft + album.bucketId
                            }
                        },
                    )
                }
            }
        }
        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Check,
                    label = stringResource(R.string.settings_connected_apps_save),
                    onClick = {
                        state.applyGalleryAlbumBucketIds(draft)
                        state.goBack()
                    },
                ),
                MetroAppBarIcon(
                    type = MetroSystemIconType.Close,
                    label = stringResource(R.string.settings_connected_apps_cancel),
                    onClick = state::goBack,
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun AlbumPickerTile(
    album: GalleryAlbumEntry,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(AlbumCoverPlaceholder)
            .metroClickable(onClick = onClick),
    ) {
        if (album.coverUri != null) {
            AlbumCoverImage(
                uri = album.coverUri,
                contentDescription = album.name,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .background(AlbumPickerCheckboxScrim)
                .padding(2.dp),
        ) {
            MetroCheckBox(
                checked = selected,
                onCheckedChange = null,
                size = 22.dp,
            )
        }
        MetroText(
            text = album.name,
            style = MetroTextStyle.ListItemSubtitle,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
        )
    }
}

@Composable
private fun AlbumCoverImage(
    uri: Uri,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var measuredPx by remember { mutableIntStateOf(0) }
    var bitmap by remember(uri) {
        mutableStateOf(AlbumCoverCache.peek(uri))
    }
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }

    LaunchedEffect(uri, measuredPx) {
        if (measuredPx <= 0) return@LaunchedEffect
        val side = measuredPx.coerceIn(64, 512)
        val cached = AlbumCoverCache.peek(uri)
        if (cached != null && bitmap == null) {
            bitmap = cached
        }
        if (cached != null && cached.width >= side / 2) return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) {
            loadAlbumCoverBitmap(context, uri, side)
        } ?: return@LaunchedEffect
        AlbumCoverCache.put(uri, loaded)
        bitmap = loaded
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { size ->
                measuredPx = max(size.width, size.height)
            },
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private object AlbumCoverCache {
    private const val MAX = 48
    private val map = object : LinkedHashMap<String, Bitmap>(MAX, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean =
            size > MAX
    }

    @Synchronized
    fun peek(uri: Uri): Bitmap? = map[uri.toString()]

    @Synchronized
    fun put(uri: Uri, bitmap: Bitmap) {
        map[uri.toString()] = bitmap
    }
}

private fun loadAlbumCoverBitmap(
    context: android.content.Context,
    uri: Uri,
    sidePx: Int,
): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.contentResolver.loadThumbnail(uri, Size(sidePx, sidePx), null)
    } else {
        @Suppress("DEPRECATION")
        android.provider.MediaStore.Images.Thumbnails.getThumbnail(
            context.contentResolver,
            ContentUris.parseId(uri),
            android.provider.MediaStore.Images.Thumbnails.MINI_KIND,
            null,
        )
    }
}.getOrNull()

/**
 * Multi-select app picker — WP Apps Corner list via [MetroMultiSelectList].
 */
@Composable
fun ConnectedAppPickerScreen(
    state: SettingsState,
    kind: ConnectedAppKind,
    modifier: Modifier = Modifier,
) {
    val committed = when (kind) {
        ConnectedAppKind.Gallery -> state.galleryAppPackages
        ConnectedAppKind.Music -> state.musicAppPackages
        ConnectedAppKind.Conversation -> state.conversationAppPackages
    }
    var draft by remember { mutableStateOf(committed) }
    LaunchedEffect(committed) {
        draft = committed
    }
    val items = remember(state.applicationEntries) {
        state.applicationEntries.map { entry ->
            MetroMultiSelectItem(id = entry.packageName, title = entry.title)
        }
    }

    MetroMultiSelectList(
        title = stringResource(R.string.settings_connected_apps_section),
        items = items,
        selectedIds = draft,
        onSelectionChange = { draft = it },
        onConfirm = {
            when (kind) {
                ConnectedAppKind.Gallery -> state.applyGalleryAppPackages(draft)
                ConnectedAppKind.Music -> state.applyMusicAppPackages(draft)
                ConnectedAppKind.Conversation -> state.applyConversationAppPackages(draft)
            }
            state.goBack()
        },
        onCancel = state::goBack,
        modifier = modifier,
        itemLeading = { item ->
            ConnectedAppIcon(packageName = item.id, label = item.title)
        },
        confirmLabel = stringResource(R.string.settings_connected_apps_save),
        cancelLabel = stringResource(R.string.settings_connected_apps_cancel),
    )
}

@Composable
private fun ConnectedAppIcon(
    packageName: String,
    label: String,
) {
    val context = LocalContext.current
    val accent = MetroTheme.colors.accent
    val pixelSize = with(LocalDensity.current) { ConnectedAppIconSize.roundToPx() }.coerceAtLeast(1)
    val tileOverride = MetroAppGlyphs.tileOverrideEntry(packageName)
    val suiteGlyph = MetroAppGlyphs.forPackage(packageName)

    val background = remember(packageName, accent) {
        when {
            tileOverride?.backgroundHex != null ->
                MetroPreferences.parseAccentHex(tileOverride.backgroundHex!!)
            suiteGlyph != null || tileOverride != null ->
                MetroAppBranding.resolveTileBackgroundColor(context, packageName)
            else ->
                MetroAppBranding.loadAppIconAsset(context, packageName).backgroundColor
        }
    }
    val contentColor = MetroColors.tileContentColor(background)
    val glyphRes = tileOverride?.glyphResId ?: suiteGlyph
    val bitmap = remember(packageName, pixelSize, glyphRes) {
        if (glyphRes != null) {
            null
        } else {
            MetroAppBranding.loadAppIcon(context, packageName)
                ?.toBitmap(pixelSize, pixelSize)
                ?.asImageBitmap()
        }
    }

    Box(
        modifier = Modifier
            .size(ConnectedAppIconSize)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        when {
            glyphRes != null -> {
                Image(
                    painter = painterResource(glyphRes),
                    contentDescription = label,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(contentColor),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(ConnectedAppIconInset),
                )
            }
            bitmap != null -> {
                Image(
                    bitmap = bitmap,
                    contentDescription = label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(ConnectedAppIconInset),
                )
            }
            else -> {
                MetroText(
                    text = label.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MetroTextStyle.ListItemTitle,
                    color = contentColor,
                )
            }
        }
    }
}

enum class ConnectedAppKind {
    Gallery,
    Music,
    Conversation,
}
