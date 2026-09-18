package io.github.kabirnayeem99.totpocket.gallery

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.kabirnayeem99.totpocket.LocalAppContainer
import io.github.kabirnayeem99.totpocket.PlatformBackHandler
import io.github.kabirnayeem99.totpocket.media.MediaImage
import io.github.kabirnayeem99.totpocket.media.MediaVideo
import io.github.kabirnayeem99.totpocket.media.VideoSurface
import io.github.kabirnayeem99.totpocket.media.formatDuration
import io.github.kabirnayeem99.totpocket.ui.components.AppChromeStyles
import io.github.kabirnayeem99.totpocket.ui.components.GestureBar
import io.github.kabirnayeem99.totpocket.ui.components.ToddlerButton
import io.github.kabirnayeem99.totpocket.ui.icons.TotPocketIcons
import io.github.kabirnayeem99.totpocket.ui.launcher.BrandColors
import io.github.kabirnayeem99.totpocket.ui.launcher.LauncherGlyphs

private val DarkText = Color(0xFF0F0F0F)
private val GreyText = Color(0xFF606060)

@Composable
fun YouTubeScreen(onBack: () -> Unit, onHome: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { YouTubeViewModel(container.mediaLibrary, container.soundPlayer) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    YouTubeContent(state, viewModel::onAction, onBack, onHome)
}

/** YouTube's home feed and watch page. Only watching — no share, download or comments. */
@Composable
fun YouTubeContent(state: YouTubeUiState, onAction: (YouTubeAction) -> Unit, onBack: () -> Unit, onHome: () -> Unit) {
    val player = state.player
    Column(Modifier.fillMaxSize().background(Color.White)) {
        if (player == null) {
            YouTubeTopBar(onBack)
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(state.videos, key = { it.id }) { video ->
                    FeedCard(video, onClick = { onAction(YouTubeAction.Open(video)) })
                }
            }
        } else {
            PlatformBackHandler(enabled = true) { onAction(YouTubeAction.Close) }
            WatchPage(player, state.upNext, onAction, Modifier.weight(1f))
        }
        GestureBar(color = AppChromeStyles.System.gestureBar, onHome = onHome)
    }
}

@Composable
private fun YouTubeTopBar(onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        ToddlerButton(
            onClick = onBack,
            contentDescription = "Back",
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = Color.Transparent,
            outline = Color.Transparent,
            outlineWidth = 1.dp,
            minSize = 72.dp,
        ) {
            Icon(TotPocketIcons.Back, contentDescription = null, tint = DarkText, modifier = Modifier.size(26.dp))
        }
        Icon(LauncherGlyphs.YouTube, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(34.dp))
        Spacer(Modifier.width(4.dp))
        Text("YouTube", color = DarkText, fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp)
    }
}

/** Full-width thumbnail with a duration badge, then the channel avatar, title and channel line. */
@Composable
private fun FeedCard(video: MediaVideo, onClick: () -> Unit) {
    ToddlerButton(
        onClick = onClick,
        contentDescription = video.title,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        color = Color.Transparent,
        outline = Color.Transparent,
        outlineWidth = 1.dp,
        playTapSound = false,
        pressedScale = 0.98f,
        contentAlignment = Alignment.TopStart,
    ) {
        Column(Modifier.padding(bottom = 14.dp)) {
            Thumbnail(video, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                ChannelAvatar(video.channel)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(video.title, color = DarkText, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(video.channel, color = GreyText, fontSize = 13.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun Thumbnail(video: MediaVideo, modifier: Modifier = Modifier) {
    Box(modifier) {
        MediaImage(video.thumb, maxPx = 720, modifier = Modifier.fillMaxSize())
        Text(
            formatDuration(video.durationMs),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun ChannelAvatar(channel: String) {
    Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFE53935)), contentAlignment = Alignment.Center) {
        Text(channel.removePrefix("TotPocket ").take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
    }
}

/** The player on top, then the title and channel, then "Up next" to choose from. */
@Composable
private fun WatchPage(player: PlayerState, upNext: List<MediaVideo>, onAction: (YouTubeAction) -> Unit, modifier: Modifier) {
    LazyColumn(modifier.fillMaxWidth()) {
        item(key = "player") {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) {
                when (val video = player.video) {
                    is MediaVideo.Slideshow -> Slideshow(video, player)
                    is MediaVideo.DeviceVideo -> VideoSurface(
                        uri = video.uri,
                        playKey = player.playKey,
                        onFinished = { onAction(YouTubeAction.VideoEnded) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (!player.playing) ReplayButton(onClick = { onAction(YouTubeAction.Replay) }, modifier = Modifier.align(Alignment.Center))
            }
        }
        item(key = "about") {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                Text(player.video.title, color = DarkText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChannelAvatar(player.video.channel)
                    Spacer(Modifier.width(10.dp))
                    Text(player.video.channel, color = DarkText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(16.dp))
                Text("Up next", color = DarkText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        items(upNext, key = { "next-${it.id}" }) { next ->
            ToddlerButton(
                onClick = { onAction(YouTubeAction.Open(next)) },
                contentDescription = next.title,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp),
                color = Color.Transparent,
                outline = Color.Transparent,
                outlineWidth = 1.dp,
                playTapSound = false,
                pressedScale = 0.98f,
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Thumbnail(next, Modifier.width(160.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(next.title, color = DarkText, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(next.channel, color = GreyText, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Photos cross-fading with a slow zoom, and YouTube's red progress bar along the bottom. */
@Composable
private fun Slideshow(video: MediaVideo.Slideshow, player: PlayerState) {
    val progress = remember(video.id, player.playKey) { Animatable(0f) }
    LaunchedEffect(video.id, player.playKey) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(video.durationMs.toInt(), easing = LinearEasing))
    }
    Box(
        Modifier.fillMaxSize().drawBehind {
            val bar = 3.dp.toPx()
            drawRect(Color.White.copy(alpha = 0.3f), topLeft = Offset(0f, size.height - bar), size = Size(size.width, bar))
            drawRect(BrandColors.YouTube, topLeft = Offset(0f, size.height - bar), size = Size(size.width * progress.value, bar))
        },
    ) {
        Crossfade(targetState = player.slide, animationSpec = tween(600), label = "slide") { slide ->
            val zoom = remember(slide, player.playKey) { Animatable(1f) }
            LaunchedEffect(slide, player.playKey) { zoom.animateTo(1.08f, tween(MediaVideo.SLIDE_MS.toInt(), easing = LinearEasing)) }
            MediaImage(
                video.photos[slide].image,
                maxPx = 1080,
                modifier = Modifier.fillMaxSize().padding(bottom = 3.dp).graphicsLayer {
                    scaleX = zoom.value
                    scaleY = zoom.value
                },
                contentScale = ContentScale.Crop,
                placeholder = Color.Black,
            )
        }
    }
}

@Composable
private fun ReplayButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    ToddlerButton(
        onClick = onClick,
        contentDescription = "Watch again",
        modifier = modifier.size(80.dp),
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.6f),
        outline = Color.Transparent,
        outlineWidth = 1.dp,
        minSize = 72.dp,
    ) {
        Text("↻", color = Color.White, fontSize = 40.sp)
    }
}
