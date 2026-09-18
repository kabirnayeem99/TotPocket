package io.github.kabirnayeem99.totpocket.gallery

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.kabirnayeem99.totpocket.LocalAppContainer
import io.github.kabirnayeem99.totpocket.PlatformBackHandler
import io.github.kabirnayeem99.totpocket.ui.components.AppChromeStyles
import io.github.kabirnayeem99.totpocket.ui.components.GestureBar
import io.github.kabirnayeem99.totpocket.ui.components.ToddlerButton
import io.github.kabirnayeem99.totpocket.ui.components.isLandscape
import io.github.kabirnayeem99.totpocket.ui.icons.TotPocketIcons
import io.github.kabirnayeem99.totpocket.ui.launcher.BrandColors
import io.github.kabirnayeem99.totpocket.ui.launcher.LauncherGlyphs
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketTheme

private val DarkText = Color(0xFF0F0F0F)
private val GreyText = Color(0xFF606060)

/** Eight "videos" fill the home feed in a 2×4 grid — the whole feed, no scrolling. */
private const val VideosOnHome = 8

@Composable
fun YouTubeScreen(onBack: () -> Unit, onHome: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel(key = "youtube") {
        SoundGridViewModel(GalleryCatalog.Animals, container.soundPlayer, pageSize = VideosOnHome)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    YouTubeContent(state, viewModel::onAction, onBack, onHome, modifier)
}

/**
 * A pretend YouTube: white bar with the logo, thumbnails with titles below. Tapping a video opens
 * the player. Nothing autoplays and nothing plays "next" by itself — the child picks every video.
 */
@Composable
fun YouTubeContent(
    state: SoundGridUiState,
    onAction: (SoundGridAction) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val open = state.openItem
    Column(modifier.fillMaxSize().background(Color.White)) {
        YouTubeTopBar(onBack = { if (open != null) onAction(SoundGridAction.CloseItem) else onBack() })
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (open != null) {
                PlatformBackHandler(enabled = true) { onAction(SoundGridAction.CloseItem) }
                Player(open, isPlaying = state.playingId == open.id, upNext = upNext(state.tiles, open), onAction = onAction)
            } else {
                Feed(state.tiles, onAction)
            }
        }
        GestureBar(color = AppChromeStyles.System.gestureBar, onHome = onHome)
    }
}

private fun upNext(tiles: List<GalleryItem>, current: GalleryItem): List<GalleryItem> {
    val start = tiles.indexOf(current)
    return (1..3).map { tiles[(start + it) % tiles.size] }
}

@Composable
private fun YouTubeTopBar(onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        ToddlerButton(
            onClick = onBack,
            contentDescription = "Back",
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
            color = Color.Transparent,
            outline = Color.Transparent,
            outlineWidth = 1.dp,
        ) {
            Icon(TotPocketIcons.Back, contentDescription = null, tint = DarkText, modifier = Modifier.size(28.dp))
        }
        Icon(LauncherGlyphs.YouTube, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(4.dp))
        Text("YouTube", color = DarkText, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
    }
}

@Composable
private fun Feed(videos: List<GalleryItem>, onAction: (SoundGridAction) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        val columns = if (isLandscape(maxWidth, maxHeight)) 4 else 2
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            videos.chunked(columns).forEach { row ->
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { video ->
                        VideoCard(video, onClick = { onAction(SoundGridAction.TileTapped(video.id)) }, Modifier.weight(1f).fillMaxHeight())
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun VideoCard(video: GalleryItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ToddlerButton(
        onClick = onClick,
        contentDescription = video.title(),
        modifier = modifier,
        shape = RoundedCornerShape(0.dp),
        color = Color.Transparent,
        outline = Color.Transparent,
        outlineWidth = 1.dp,
        playTapSound = false,
        pressedScale = 0.96f,
        contentAlignment = Alignment.TopStart,
    ) {
        Column {
            Thumbnail(video, Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)))
            Spacer(Modifier.height(6.dp))
            Text(
                video.title(),
                color = DarkText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("TotPocket Animals", color = GreyText, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun Thumbnail(video: GalleryItem, modifier: Modifier = Modifier, emojiSize: Int = 44) {
    Box(modifier.background(Color(video.backdrop)), contentAlignment = Alignment.Center) {
        Text(video.emoji, fontSize = emojiSize.sp)
        Text(
            "0:03",
            color = Color.White,
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}

/**
 * The watch page: a 16:9 player (tap it to watch again) with a red progress bar that runs while
 * the sound plays, the title and channel, then three "Up next" videos the child can choose.
 */
@Composable
private fun Player(video: GalleryItem, isPlaying: Boolean, upNext: List<GalleryItem>, onAction: (SoundGridAction) -> Unit) {
    val progress = remember(video.id) { Animatable(0f) }
    LaunchedEffect(video.id, isPlaying) {
        if (isPlaying) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(3_000, easing = LinearEasing))
        }
    }
    Column(Modifier.fillMaxSize()) {
        ToddlerButton(
            onClick = { onAction(SoundGridAction.TileTapped(video.id)) },
            contentDescription = "Play ${video.title()}",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .drawBehind {
                    val bar = 4.dp.toPx()
                    drawRect(Color.White.copy(alpha = 0.3f), topLeft = Offset(0f, size.height - bar), size = Size(size.width, bar))
                    drawRect(
                        BrandColors.YouTube,
                        topLeft = Offset(0f, size.height - bar),
                        size = Size(size.width * progress.value, bar),
                    )
                },
            shape = RoundedCornerShape(0.dp),
            color = Color.Black,
            outline = Color.Transparent,
            outlineWidth = 1.dp,
            playTapSound = false,
            pressedScale = 1f,
        ) {
            Thumbnail(video, Modifier.fillMaxSize().padding(bottom = 4.dp), emojiSize = 110)
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(video.title(), color = DarkText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFFFE0B2)), contentAlignment = Alignment.Center) {
                    Text("🐾", fontSize = 16.sp)
                }
                Spacer(Modifier.width(10.dp))
                Text("TotPocket Animals", color = DarkText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
        Text("Up next", color = DarkText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp))
        Spacer(Modifier.height(6.dp))
        upNext.forEach { next ->
            ToddlerButton(
                onClick = { onAction(SoundGridAction.TileTapped(next.id)) },
                contentDescription = next.title(),
                modifier = Modifier.fillMaxWidth().height(96.dp),
                shape = RoundedCornerShape(0.dp),
                color = Color.Transparent,
                outline = Color.Transparent,
                outlineWidth = 1.dp,
                playTapSound = false,
                pressedScale = 0.97f,
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Thumbnail(next, Modifier.height(80.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)), emojiSize = 36)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(next.title(), color = DarkText, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        Text("TotPocket Animals", color = GreyText, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun GalleryItem.title(): String = "$name sounds for kids"

@Preview
@Composable
private fun YouTubeFeedPreview() {
    val items = GalleryCatalog.Animals.items.take(VideosOnHome)
    TotPocketTheme {
        YouTubeContent(SoundGridUiState("Animals", 0, 2, items, openItem = null, playingId = null), onAction = {}, onBack = {}, onHome = {})
    }
}
