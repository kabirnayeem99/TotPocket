package io.github.kabirnayeem99.totpocket.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.kabirnayeem99.totpocket.LocalAppContainer
import io.github.kabirnayeem99.totpocket.PlatformBackHandler
import io.github.kabirnayeem99.totpocket.media.MediaAlbum
import io.github.kabirnayeem99.totpocket.media.MediaImage
import io.github.kabirnayeem99.totpocket.media.MediaPhoto
import io.github.kabirnayeem99.totpocket.ui.components.AppChromeStyle
import io.github.kabirnayeem99.totpocket.ui.components.AppChromeStyles
import io.github.kabirnayeem99.totpocket.ui.components.AppScaffold
import io.github.kabirnayeem99.totpocket.ui.components.AppTopBar
import io.github.kabirnayeem99.totpocket.ui.components.GestureBar
import io.github.kabirnayeem99.totpocket.ui.components.ToddlerButton

private val DarkText = Color(0xFF1B1B1B)
private val GreyText = Color(0xFF6F7378)
private val ViewerChrome = AppChromeStyle(Color.Black, Color.Black, Color.White, Color.White)

/** Photos' album list: the photo pack's albums, then the phone's own folders. Read-only. */
@Composable
fun PhotosAlbumsScreen(onBack: () -> Unit, onHome: () -> Unit, onOpenAlbum: (albumId: String) -> Unit) {
    val library by LocalAppContainer.current.mediaLibrary.state.collectAsStateWithLifecycle()
    AppScaffold(title = "Photos", style = AppChromeStyles.System, onBack = onBack, onHome = onHome) {
        val (bundled, device) = library.albums.filter { it.photos.isNotEmpty() }.partition { !it.onDevice }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            section("Albums", bundled, onOpenAlbum)
            if (device.isNotEmpty()) section("On this phone", device, onOpenAlbum)
        }
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.section(
    title: String,
    albums: List<MediaAlbum>,
    onOpenAlbum: (String) -> Unit,
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Text(title, color = DarkText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
    }
    items(albums, key = { it.id }) { album -> AlbumCard(album, onClick = { onOpenAlbum(album.id) }) }
}

@Composable
private fun AlbumCard(album: MediaAlbum, onClick: () -> Unit) {
    ToddlerButton(
        onClick = onClick,
        contentDescription = album.title,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        outline = Color.Transparent,
        outlineWidth = 1.dp,
        pressedScale = 0.96f,
        contentAlignment = Alignment.TopStart,
    ) {
        Column {
            val cover = album.cover
            if (cover != null) {
                MediaImage(cover.thumb, maxPx = 360, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)))
            }
            Spacer(Modifier.height(8.dp))
            Text(album.title, color = DarkText, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${album.photos.size}", color = GreyText, fontSize = 13.sp)
        }
    }
}

@Composable
fun AlbumScreen(albumId: String, onBack: () -> Unit, onHome: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel(key = "album-$albumId") { AlbumViewModel(albumId, container.mediaLibrary, container.soundPlayer) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    AlbumContent(state, viewModel::onAction, onBack, onHome)
}

/** A tight grid of square photos, like any phone gallery; tapping one opens the viewer. */
@Composable
fun AlbumContent(state: AlbumUiState, onAction: (AlbumAction) -> Unit, onBack: () -> Unit, onHome: () -> Unit) {
    val album = state.album
    val open = state.openIndex
    if (album != null && open != null) {
        PlatformBackHandler(enabled = true) { onAction(AlbumAction.CloseViewer) }
        PhotoViewer(album.photos, open, onAction, onHome)
        return
    }
    AppScaffold(title = album?.title ?: "", style = AppChromeStyles.System, onBack = onBack, onHome = onHome) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(110.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            itemsIndexed(album?.photos.orEmpty(), key = { _, photo -> photo.id }) { index, photo ->
                ToddlerButton(
                    onClick = { onAction(AlbumAction.PhotoTapped(index)) },
                    contentDescription = photo.title,
                    modifier = Modifier.aspectRatio(1f),
                    shape = RoundedCornerShape(2.dp),
                    color = Color.Transparent,
                    outline = Color.Transparent,
                    outlineWidth = 1.dp,
                    playTapSound = false,
                    pressedScale = 0.95f,
                    minSize = 80.dp,
                ) {
                    MediaImage(photo.thumb, maxPx = 360, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

/**
 * Full-screen photos on black: swipe sideways through the album, the photo's name at the
 * bottom. Only a back arrow — no share, edit or delete.
 */
@Composable
private fun PhotoViewer(photos: List<MediaPhoto>, startIndex: Int, onAction: (AlbumAction) -> Unit, onHome: () -> Unit) {
    val pager = rememberPagerState(initialPage = startIndex) { photos.size }
    LaunchedEffect(pager.settledPage) { onAction(AlbumAction.PhotoShown(pager.settledPage)) }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        AppTopBar(title = "", style = ViewerChrome, onBack = { onAction(AlbumAction.CloseViewer) })
        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth(), key = { photos[it].id }) { page ->
            Box(Modifier.fillMaxSize()) {
                MediaImage(
                    photos[page].image,
                    maxPx = 1080,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    placeholder = Color.Black,
                )
                Text(
                    photos[page].title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                )
            }
        }
        GestureBar(color = Color.White, onHome = onHome)
    }
}
