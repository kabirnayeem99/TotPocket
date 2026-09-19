package io.github.kabirnayeem99.totpocket.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import io.github.kabirnayeem99.totpocket.ui.components.NavigationButtons
import io.github.kabirnayeem99.totpocket.ui.components.StatusStrip
import io.github.kabirnayeem99.totpocket.ui.components.ToddlerButton
import io.github.kabirnayeem99.totpocket.ui.icons.TotPocketIcons
import io.github.kabirnayeem99.totpocket.ui.launcher.BrandColors
import io.github.kabirnayeem99.totpocket.ui.launcher.LauncherGlyphs
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.grid.items as gridItems

private val DarkText = Color(0xFF0F0F0F)
private val GreyText = Color(0xFF606060)

@Composable
fun YouTubeScreen(onBack: () -> Unit, onHome: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { YouTubeViewModel(container.mediaLibrary, container.soundPlayer) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    YouTubeContent(state, viewModel::onAction, onBack, onHome)
}

/**
 * YouTube's Home feed, Shorts, the watch page, full screen and Shorts reels. Only watching and
 * play / pause — no likes, comments, subscribing, sharing or downloading.
 */
@Composable
fun YouTubeContent(state: YouTubeUiState, onAction: (YouTubeAction) -> Unit, onBack: () -> Unit, onHome: () -> Unit) {
    val player = state.player
    if (player != null) PlatformBackHandler(enabled = true) { onAction(YouTubeAction.Close) }
    when (player?.mode) {
        PlayerMode.FullScreen -> FullScreenPlayer(player, onAction, onHome)
        PlayerMode.Reels -> ReelsPager(state, player, onAction, onHome)
        else -> Column(Modifier.fillMaxSize().background(Color.White)) {
            if (player == null) {
                YouTubeTopBar(onBack)
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (state.tab) {
                        YouTubeTab.Home -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 8.dp)) {
                            items(state.videos, key = { it.id }) { video ->
                                FeedCard(video, onClick = { onAction(YouTubeAction.Open(video)) })
                            }
                        }
                        YouTubeTab.Shorts -> ShortsGrid(state.videos, onOpen = { onAction(YouTubeAction.OpenReels(it)) })
                    }
                }
                BottomTabs(state.tab, onSelect = { onAction(YouTubeAction.SelectTab(it)) })
            } else {
                // Watch page: black status strip above the player, like the real app.
                StatusStrip(contentColor = Color.White, modifier = Modifier.background(Color.Black))
                WatchPage(player, state.upNext, onAction, Modifier.weight(1f))
            }
            NavigationButtons(color = AppChromeStyles.System.gestureBar, onHome = onHome)
        }
    }
}

// ---------------------------------------------------------------- feed

@Composable
private fun YouTubeTopBar(onBack: () -> Unit) {
    StatusStrip(contentColor = DarkText)
    Row(Modifier.fillMaxWidth().height(56.dp).padding(start = 4.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundButton(TotPocketIcons.Back, "Back", size = 48.dp, tint = DarkText, background = Color.Transparent, onClick = onBack)
        Spacer(Modifier.width(8.dp))
        Icon(LauncherGlyphs.YouTube, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(4.dp))
        Text("YouTube", color = DarkText, fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp)
    }
}

/** YouTube's bottom navigation: Home and Shorts. */
@Composable
private fun BottomTabs(selected: YouTubeTab, onSelect: (YouTubeTab) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White).padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        listOf(YouTubeTab.Home to TotPocketIcons.Home, YouTubeTab.Shorts to TotPocketIcons.Shorts).forEach { (tab, icon) ->
            ToddlerButton(
                onClick = { onSelect(tab) },
                contentDescription = tab.name,
                modifier = Modifier.width(120.dp).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                outline = Color.Transparent,
                outlineWidth = 1.dp,
                pressedScale = 0.95f,
                minSize = 44.dp,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(icon, contentDescription = null, tint = DarkText, modifier = Modifier.size(24.dp))
                    Text(tab.name, color = DarkText, fontSize = 11.sp, fontWeight = if (tab == selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

/** Full-width thumbnail with a duration badge, then the channel picture, title and channel. */
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
        Column(Modifier.padding(bottom = 12.dp)) {
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

/** Shorts: tall thumbnails, two per row, each opening the full-screen reels. */
@Composable
private fun ShortsGrid(videos: List<MediaVideo>, onOpen: (MediaVideo) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        gridItems(videos, key = { "short-${it.id}" }) { video ->
            ToddlerButton(
                onClick = { onOpen(video) },
                contentDescription = video.title,
                modifier = Modifier.aspectRatio(9f / 16f),
                shape = RoundedCornerShape(12.dp),
                color = Color.Black,
                outline = Color.Transparent,
                outlineWidth = 1.dp,
                playTapSound = false,
                pressedScale = 0.97f,
                minSize = 80.dp,
            ) {
                MediaImage(video.thumb, maxPx = 540, modifier = Modifier.fillMaxSize())
                Text(
                    video.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                        .padding(10.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- watch page

/** The player, the title and channel, then more videos to choose from. */
@Composable
private fun WatchPage(player: PlayerState, upNext: List<MediaVideo>, onAction: (YouTubeAction) -> Unit, modifier: Modifier) {
    LazyColumn(modifier.fillMaxWidth()) {
        item(key = "player") {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) {
                PlayerSurface(player, onAction, ContentScale.Crop, Modifier.fillMaxSize())
                PlayerControls(player, onAction, fullScreen = false)
            }
        }
        item(key = "about") {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                Text(player.video.title, color = DarkText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChannelAvatar(player.video.channel)
                    Spacer(Modifier.width(10.dp))
                    Text(player.video.channel, color = DarkText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        item(key = "divider") { Box(Modifier.fillMaxWidth().padding(bottom = 6.dp).height(1.dp).background(Color(0xFFE5E5E5))) }
        items(upNext, key = { "next-${it.id}" }) { next ->
            FeedCard(next, onClick = { onAction(YouTubeAction.Open(next)) })
        }
    }
}

/** The video alone, filling the screen, with the same controls. */
@Composable
private fun FullScreenPlayer(player: PlayerState, onAction: (YouTubeAction) -> Unit, onHome: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            PlayerSurface(player, onAction, ContentScale.Fit, Modifier.fillMaxSize())
            PlayerControls(player, onAction, fullScreen = true)
        }
        NavigationButtons(color = Color.White, onHome = onHome)
    }
}

/**
 * YouTube-style controls over the player: tap the video to show them — a dimmed layer with a big
 * play / pause button in the middle and the full-screen button in the corner. They hide again
 * after a few seconds while the video plays. A finished video shows "watch again" instead.
 */
@Composable
private fun BoxScope.PlayerControls(player: PlayerState, onAction: (YouTubeAction) -> Unit, fullScreen: Boolean) {
    var visible by remember(player.video.id) { mutableStateOf(true) }
    LaunchedEffect(visible, player.paused, player.playing) {
        if (visible && !player.paused && player.playing) {
            delay(3_000)
            visible = false
        }
    }
    Box(
        Modifier.matchParentSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
            visible = !visible
        },
    )
    if (!player.playing) {
        RoundButton(TotPocketIcons.Replay, "Watch again", modifier = Modifier.align(Alignment.Center), size = 72.dp) {
            onAction(YouTubeAction.Replay)
        }
        return
    }
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.matchParentSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))) {
            RoundButton(
                if (player.paused) TotPocketIcons.Play else TotPocketIcons.Pause,
                if (player.paused) "Play" else "Pause",
                modifier = Modifier.align(Alignment.Center),
                size = 72.dp,
            ) { onAction(YouTubeAction.TogglePause) }
            RoundButton(
                if (fullScreen) TotPocketIcons.ExitFullScreen else TotPocketIcons.FullScreen,
                if (fullScreen) "Exit full screen" else "Full screen",
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                size = 48.dp,
            ) { onAction(if (fullScreen) YouTubeAction.ExitFullScreen else YouTubeAction.EnterFullScreen) }
        }
    }
}

// ---------------------------------------------------------------- shorts reels

/**
 * Shorts reels: one full-screen video per page, swiped up and down. The page on screen plays;
 * tap to pause or play. When a reel ends the next one slides in and starts, like the real app.
 */
@Composable
private fun ReelsPager(state: YouTubeUiState, player: PlayerState, onAction: (YouTubeAction) -> Unit, onHome: () -> Unit) {
    val videos = state.videos
    if (videos.isEmpty()) return
    val pager = rememberPagerState(initialPage = state.reelIndex) { videos.size }
    LaunchedEffect(pager.settledPage) { onAction(YouTubeAction.ReelShown(videos[pager.settledPage])) }
    // Follow the ViewModel when a reel ends and the next one starts on its own.
    LaunchedEffect(state.reelIndex) {
        if (pager.currentPage != state.reelIndex) pager.animateScrollToPage(state.reelIndex)
    }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth(), key = { videos[it].id }) { page ->
            val video = videos[page]
            Box(Modifier.fillMaxSize()) {
                if (player.video == video) {
                    PlayerSurface(player, onAction, ContentScale.Crop, Modifier.fillMaxSize())
                    Box(
                        Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            onAction(YouTubeAction.TogglePause)
                        },
                    )
                    if (player.paused) {
                        Icon(
                            TotPocketIcons.Play,
                            contentDescription = "Paused",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.align(Alignment.Center).size(90.dp),
                        )
                    }
                } else {
                    MediaImage(video.thumb, maxPx = 720, modifier = Modifier.fillMaxSize(), placeholder = Color.Black)
                }
                ReelOverlay(video, onAction)
            }
        }
        NavigationButtons(color = Color.White, onHome = onHome)
    }
}

/** Back and "Shorts" at the top; the channel and title at the bottom. */
@Composable
private fun BoxScope.ReelOverlay(video: MediaVideo, onAction: (YouTubeAction) -> Unit) {
    Column(Modifier.align(Alignment.TopStart)) {
        StatusStrip(contentColor = Color.White)
        Row(Modifier.fillMaxWidth().padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundButton(TotPocketIcons.Back, "Back", size = 48.dp, background = Color.Transparent) { onAction(YouTubeAction.Close) }
            Spacer(Modifier.width(4.dp))
            Text("Shorts", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
    Column(
        Modifier.align(Alignment.BottomStart).fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
            .padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChannelAvatar(video.channel)
            Spacer(Modifier.width(10.dp))
            Text(video.channel, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        Text(video.title, color = Color.White, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

// ---------------------------------------------------------------- player

/** Whatever is playing — a slideshow or a phone video — sized by [contentScale]. */
@Composable
private fun PlayerSurface(player: PlayerState, onAction: (YouTubeAction) -> Unit, contentScale: ContentScale, modifier: Modifier) {
    Box(modifier) {
        when (val video = player.video) {
            is MediaVideo.Slideshow -> Slideshow(video, player, contentScale)
            is MediaVideo.DeviceVideo -> VideoSurface(
                uri = video.uri,
                playKey = player.playKey,
                paused = player.paused,
                onFinished = { onAction(YouTubeAction.VideoEnded) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Photos cross-fading with a slow zoom, and YouTube's red progress bar along the bottom. Both
 * follow the slideshow's clock, so pausing freezes them.
 */
@Composable
private fun Slideshow(video: MediaVideo.Slideshow, player: PlayerState, contentScale: ContentScale) {
    val position = player.positionMs
    Box(
        Modifier.fillMaxSize().drawBehind {
            val bar = 3.dp.toPx()
            val fraction = (position.toFloat() / video.durationMs).coerceIn(0f, 1f)
            drawRect(Color.White.copy(alpha = 0.3f), topLeft = Offset(0f, size.height - bar), size = Size(size.width, bar))
            drawRect(BrandColors.YouTube, topLeft = Offset(0f, size.height - bar), size = Size(size.width * fraction, bar))
        },
    ) {
        Crossfade(targetState = player.slide, animationSpec = tween(600), label = "slide") { slide ->
            MediaImage(
                video.photos[slide].image,
                maxPx = 1080,
                modifier = Modifier.fillMaxSize().padding(bottom = 3.dp).graphicsLayer {
                    val intoSlide = ((position - slide * MediaVideo.SLIDE_MS).toFloat() / MediaVideo.SLIDE_MS).coerceIn(0f, 1f)
                    val zoom = 1f + 0.08f * intoSlide
                    scaleX = zoom
                    scaleY = zoom
                },
                contentScale = contentScale,
                placeholder = Color.Black,
            )
        }
    }
}

@Composable
private fun RoundButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    tint: Color = Color.White,
    background: Color = Color.Black.copy(alpha = 0.45f),
    onClick: () -> Unit,
) {
    ToddlerButton(
        onClick = onClick,
        contentDescription = label,
        modifier = modifier.size(size),
        shape = CircleShape,
        color = background,
        outline = Color.Transparent,
        outlineWidth = 1.dp,
        minSize = 44.dp,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}
