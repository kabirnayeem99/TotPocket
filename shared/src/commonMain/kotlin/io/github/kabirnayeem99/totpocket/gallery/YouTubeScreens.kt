package io.github.kabirnayeem99.totpocket.gallery

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

/**
 * YouTube's Home feed, Shorts, the watch page, full screen and Shorts reels. Only watching — no
 * share, download or comments.
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
                        YouTubeTab.Home -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                            items(state.videos, key = { it.id }) { video ->
                                FeedCard(video, onClick = { onAction(YouTubeAction.Open(video)) })
                            }
                        }
                        YouTubeTab.Shorts -> ShortsGrid(state.videos, onOpen = { onAction(YouTubeAction.OpenReels(it)) })
                    }
                }
                BottomTabs(state.tab, onSelect = { onAction(YouTubeAction.SelectTab(it)) })
            } else {
                WatchPage(player, state.upNext, onAction, Modifier.weight(1f))
            }
            GestureBar(color = AppChromeStyles.System.gestureBar, onHome = onHome)
        }
    }
}

/** YouTube's bottom navigation: Home and Shorts. */
@Composable
private fun BottomTabs(selected: YouTubeTab, onSelect: (YouTubeTab) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White), horizontalArrangement = Arrangement.SpaceEvenly) {
        listOf(YouTubeTab.Home to TotPocketIcons.Home, YouTubeTab.Shorts to TotPocketIcons.Shorts).forEach { (tab, icon) ->
            ToddlerButton(
                onClick = { onSelect(tab) },
                contentDescription = tab.name,
                modifier = Modifier.width(120.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                outline = Color.Transparent,
                outlineWidth = 1.dp,
                pressedScale = 0.95f,
                minSize = 56.dp,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 6.dp)) {
                    Icon(icon, contentDescription = null, tint = DarkText, modifier = Modifier.size(if (tab == selected) 28.dp else 24.dp))
                    Text(
                        tab.name,
                        color = DarkText,
                        fontSize = 11.sp,
                        fontWeight = if (tab == selected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
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

/** The video alone, filling the screen. Tap it to show or hide the exit button. */
@Composable
private fun FullScreenPlayer(player: PlayerState, onAction: (YouTubeAction) -> Unit, onHome: () -> Unit) {
    var controls by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Box(
            Modifier.weight(1f).fillMaxWidth().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { controls = !controls },
        ) {
            PlayerSurface(player, onAction, ContentScale.Fit, Modifier.fillMaxSize())
            if (controls) {
                CornerButton(
                    TotPocketIcons.ExitFullScreen,
                    "Exit full screen",
                    Modifier.align(Alignment.BottomEnd).padding(16.dp),
                ) { onAction(YouTubeAction.ExitFullScreen) }
            }
        }
        GestureBar(color = Color.White, onHome = onHome)
    }
}

/**
 * Shorts reels: one full-screen video per page, swiped up and down. The page on screen plays;
 * when it ends the next one slides in and starts, like the real app.
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
                } else {
                    MediaImage(video.thumb, maxPx = 720, modifier = Modifier.fillMaxSize(), placeholder = Color.Black)
                }
                ReelOverlay(video, onAction)
            }
        }
        GestureBar(color = Color.White, onHome = onHome)
    }
}

@Composable
private fun ReelOverlay(video: MediaVideo, onAction: (YouTubeAction) -> Unit) {
    var liked by remember(video.id) { mutableStateOf(false) }
    val seed = video.id.hashCode().let { if (it < 0) -it else it }
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CornerButton(TotPocketIcons.Back, "Back", Modifier) { onAction(YouTubeAction.Close) }
            Text("Shorts", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ToddlerButton(
                onClick = { liked = !liked },
                contentDescription = "Like",
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.35f),
                outline = Color.Transparent,
                outlineWidth = 1.dp,
                minSize = 56.dp,
            ) { Text(if (liked) "❤️" else "🤍", fontSize = 28.sp) }
            Text("${seed % 90 + (if (liked) 11 else 10)}K", color = Color.White, fontSize = 13.sp)
        }
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                .padding(start = 16.dp, end = 90.dp, top = 40.dp, bottom = 20.dp),
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
}

@Composable
private fun CornerButton(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    ToddlerButton(
        onClick = onClick,
        contentDescription = label,
        modifier = modifier.size(56.dp),
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.35f),
        outline = Color.Transparent,
        outlineWidth = 1.dp,
        minSize = 48.dp,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
    }
}

/** Whatever is playing — a slideshow or a phone video — sized by [contentScale]. */
@Composable
private fun PlayerSurface(player: PlayerState, onAction: (YouTubeAction) -> Unit, contentScale: ContentScale, modifier: Modifier) {
    Box(modifier) {
        when (val video = player.video) {
            is MediaVideo.Slideshow -> Slideshow(video, player, contentScale)
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
                PlayerSurface(player, onAction, ContentScale.Crop, Modifier.fillMaxSize())
                CornerButton(TotPocketIcons.FullScreen, "Full screen", Modifier.align(Alignment.BottomEnd).padding(6.dp)) {
                    onAction(YouTubeAction.EnterFullScreen)
                }
            }
        }
        item(key = "about") { VideoInfo(player.video) }
        item(key = "divider-1") { Divider() }
        item(key = "channel") { ChannelRow(player.video.channel) }
        item(key = "divider-2") { Divider() }
        items(upNext, key = { "next-${it.id}" }) { next ->
            FeedCard(next, onClick = { onAction(YouTubeAction.Open(next)) })
        }
    }
}

/** Title, a "views · age" line and like / dislike — after the YouTube clone. No share or download. */
@Composable
private fun VideoInfo(video: MediaVideo) {
    var liked by remember(video.id) { mutableStateOf(false) }
    var disliked by remember(video.id) { mutableStateOf(false) }
    val seed = video.id.hashCode().let { if (it < 0) -it else it }
    Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp)) {
        Text(video.title, color = DarkText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("${seed % 900 + 12}K views · ${seed % 5 + 1}y ago", color = GreyText, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill(if (liked) "👍 ${seed % 90 + 11}K" else "👍 ${seed % 90 + 10}K", selected = liked) {
                liked = !liked
                if (liked) disliked = false
            }
            Pill("👎", selected = disliked) {
                disliked = !disliked
                if (disliked) liked = false
            }
        }
    }
}

@Composable
private fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    ToddlerButton(
        onClick = onClick,
        contentDescription = text,
        shape = RoundedCornerShape(50),
        color = if (selected) DarkText else Color(0xFFF2F2F2),
        outline = Color.Transparent,
        outlineWidth = 1.dp,
        pressedScale = 0.95f,
        minSize = 44.dp,
    ) {
        Text(text, color = if (selected) Color.White else DarkText, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
    }
}

/** Channel picture, name and subscribers, with YouTube's red SUBSCRIBE. */
@Composable
private fun ChannelRow(channel: String) {
    var subscribed by remember(channel) { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        ChannelAvatar(channel)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(channel, color = DarkText, fontSize = 17.sp)
            Text("${channel.length * 31}K subscribers", color = GreyText, fontSize = 13.sp)
        }
        ToddlerButton(
            onClick = { subscribed = !subscribed },
            contentDescription = "Subscribe",
            shape = RoundedCornerShape(50),
            color = Color.Transparent,
            outline = Color.Transparent,
            outlineWidth = 1.dp,
            pressedScale = 0.95f,
            minSize = 48.dp,
        ) {
            Text(
                if (subscribed) "SUBSCRIBED" else "SUBSCRIBE",
                color = if (subscribed) GreyText else BrandColors.YouTube,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp).height(1.dp).background(Color(0xFFE5E5E5)))
}

/** Photos cross-fading with a slow zoom, and YouTube's red progress bar along the bottom. */
@Composable
private fun Slideshow(video: MediaVideo.Slideshow, player: PlayerState, contentScale: ContentScale) {
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
                contentScale = contentScale,
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
