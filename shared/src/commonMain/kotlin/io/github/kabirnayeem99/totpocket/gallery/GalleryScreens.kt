package io.github.kabirnayeem99.totpocket.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.kabirnayeem99.totpocket.LocalAppContainer
import io.github.kabirnayeem99.totpocket.PlatformBackHandler
import io.github.kabirnayeem99.totpocket.ui.components.AppChromeStyles
import io.github.kabirnayeem99.totpocket.ui.components.AppScaffold
import io.github.kabirnayeem99.totpocket.ui.components.AppTopBar
import io.github.kabirnayeem99.totpocket.ui.components.GestureBar
import io.github.kabirnayeem99.totpocket.ui.components.ToddlerButton
import io.github.kabirnayeem99.totpocket.ui.components.isLandscape
import io.github.kabirnayeem99.totpocket.ui.components.AppChromeStyle
import io.github.kabirnayeem99.totpocket.ui.icons.TotPocketIcons
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketTheme

private val DarkText = Color(0xFF1B1B1B)
private val GreyText = Color(0xFF8A8A8E)
private val ViewerChrome = AppChromeStyle(Color.Black, Color.Black, Color.White, Color.White)

/** Gallery's "Albums": a thumbnail collage per album with its name and photo count. */
@Composable
fun GalleryAlbumsScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onOpenAlbum: (categoryId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppScaffold(title = "Albums", style = AppChromeStyles.System, onBack = onBack, onHome = onHome, modifier = modifier) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            val columns = if (isLandscape(maxWidth, maxHeight)) 3 else 2
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                GalleryCatalog.categories.chunked(columns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { album ->
                            AlbumCard(album, onClick = { onOpenAlbum(album.id) }, modifier = Modifier.weight(1f))
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(album: GalleryCategory, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        ToddlerButton(
            onClick = onClick,
            contentDescription = album.name,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFF2F3F5),
            outline = Color.Transparent,
            outlineWidth = 1.dp,
            pressedScale = 0.96f,
        ) {
            // A 2×2 collage of the album's first pictures, like a real album cover.
            Column(Modifier.fillMaxSize()) {
                album.items.take(4).chunked(2).forEach { pair ->
                    Row(Modifier.weight(1f)) {
                        pair.forEach { item ->
                            Box(
                                Modifier.weight(1f).fillMaxSize().background(Color(item.backdrop)),
                                contentAlignment = Alignment.Center,
                            ) { Text(item.emoji, fontSize = 34.sp) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(album.name, color = DarkText, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        Text("${album.items.size}", color = GreyText, fontSize = 14.sp)
    }
}

@Composable
fun PhotoGridScreen(
    categoryId: String,
    onBack: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel(key = "photos-$categoryId") {
        SoundGridViewModel(GalleryCatalog.category(categoryId), container.soundPlayer)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    PhotoGridContent(state, viewModel::onAction, onBack, onHome, modifier)
}

/** A tight grid of square photos, like any phone gallery. Tapping one opens it full-screen. */
@Composable
fun PhotoGridContent(
    state: SoundGridUiState,
    onAction: (SoundGridAction) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val open = state.openItem
    if (open != null) {
        // Back closes the photo before it leaves the album — as in the real app.
        PlatformBackHandler(enabled = true) { onAction(SoundGridAction.CloseItem) }
        PhotoViewer(open, isPlaying = state.playingId == open.id, onAction = onAction, onHome = onHome, modifier = modifier)
        return
    }
    AppScaffold(title = state.categoryName, style = AppChromeStyles.System, onBack = onBack, onHome = onHome, modifier = modifier) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val columns = if (isLandscape(maxWidth, maxHeight)) 6 else 3
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                state.tiles.chunked(columns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        row.forEach { item ->
                            PhotoTile(item, onClick = { onAction(SoundGridAction.TileTapped(item.id)) }, Modifier.weight(1f))
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoTile(item: GalleryItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ToddlerButton(
        onClick = onClick,
        contentDescription = item.name,
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(2.dp),
        color = Color(item.backdrop),
        outline = Color.Transparent,
        outlineWidth = 1.dp,
        playTapSound = false,
        pressedScale = 0.95f,
    ) {
        Text(item.emoji, fontSize = 58.sp)
    }
}

/** Full-screen photo on black. Tapping the photo plays its sound again. */
@Composable
private fun PhotoViewer(
    item: GalleryItem,
    isPlaying: Boolean,
    onAction: (SoundGridAction) -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(Color.Black)) {
        AppTopBar(title = item.name, style = ViewerChrome, onBack = { onAction(SoundGridAction.CloseItem) })
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            ToddlerButton(
                onClick = { onAction(SoundGridAction.TileTapped(item.id)) },
                contentDescription = item.name,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                shape = RoundedCornerShape(0.dp),
                color = Color(item.backdrop),
                outline = Color.Transparent,
                outlineWidth = 1.dp,
                playTapSound = false,
                pressedScale = 0.98f,
            ) {
                Text(item.emoji, fontSize = 220.sp)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                TotPocketIcons.Speaker,
                contentDescription = null,
                tint = if (isPlaying) Color.White else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(width = 6.dp, height = 6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.4f)))
        }
        GestureBar(color = Color.White, onHome = onHome)
    }
}

@Preview
@Composable
private fun PhotoGridPreview() {
    val items = GalleryCatalog.Animals.items
    TotPocketTheme {
        PhotoGridContent(
            SoundGridUiState("Animals", 0, 1, items, openItem = null, playingId = null),
            onAction = {},
            onBack = {},
            onHome = {},
        )
    }
}

@Preview
@Composable
private fun AlbumsPreview() {
    TotPocketTheme { GalleryAlbumsScreen(onBack = {}, onHome = {}, onOpenAlbum = {}) }
}
