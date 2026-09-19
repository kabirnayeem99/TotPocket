package io.github.kabirnayeem99.totpocket.media

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW

/**
 * A phone video, played with ExoPlayer. One player lives as long as this surface: a new [uri] or
 * [playKey] just loads into it, [paused] pauses and resumes it, and it's released when the surface
 * leaves the screen. It takes audio focus like a real video app and plays once, calling
 * [onFinished] at the end (or on an error), nothing next on its own.
 */
@OptIn(UnstableApi::class)
@Composable
actual fun VideoSurface(uri: String, playKey: Int, paused: Boolean, onFinished: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val finished by rememberUpdatedState(onFinished)
    val player = remember {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
                /* handleAudioFocus = */ true,
            )
            .build()
            .apply { repeatMode = Player.REPEAT_MODE_OFF }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) finished()
            }

            override fun onPlayerError(error: PlaybackException) = finished()
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    LaunchedEffect(uri, playKey) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = !paused
    }
    LaunchedEffect(paused) { player.playWhenReady = !paused }
    // TextureView: moves and fades smoothly inside pagers and scrolling lists.
    ContentFrame(player = player, modifier = modifier, surfaceType = SURFACE_TYPE_TEXTURE_VIEW, contentScale = ContentScale.Fit)
}
